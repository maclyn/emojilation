package com.inipage.translatetoemoji;

import android.util.Log;
import android.util.Pair;

import com.inipage.translatetoemoji.model.Codepoint;
import com.inipage.translatetoemoji.workingmodel.PhrasePiece;
import com.inipage.translatetoemoji.workingmodel.PhraseText;
import com.inipage.translatetoemoji.workingmodel.RawText;
import com.inipage.translatetoemoji.workingmodel.RootText;
import com.inipage.translatetoemoji.workingmodel.TranslationChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class Translator {
	private static final String TAG = "Translator";

	/**
	 * Preprocess a given string for phrases and untranslatable (i.e. "raw") pieces of text.
	 * @param str The input string.
	 * @return A sorted list of {@linkplain PhraseText}s and {@linkplain RawText}s.
	 */
	private static List<RootText> preprocess(String str){
		if(LoadedDict.getInstance() == null) throw new RuntimeException("Can't preprocess without a loaded dictionary!");

		LoadedDict dict = LoadedDict.getInstance();

		//TODO: Test delimiter handling with 0-length delimiters (i.e. "", for Chinese/Japanese etc.)
		if(dict.getDictionary().getDelimiter() == null || dict.getDictionary().getDelimiter().isEmpty()) throw new RuntimeException("TODO: Support languages without delimiters!");
		char delimiter = dict.getDictionary().getDelimiter().charAt(0);

		List<RootText> result = new ArrayList<>();

		PhraseText currentPhrase = null;
		RawText currentRaw = null;

		//NOTE(s):
		//- Properties of this algorithm: currentPhrase and currentRaw may either BOTH be null,
		//	or one may be null, but both cannot be set
		//- There is a decent amount of redundancy in some of these conditional branches; this is preserved
		//	to ensure clarity
		for(int i = 0; i < str.length(); i++){
			char workingChar = str.charAt(i);
			if(Character.isLetter(workingChar) || dict.isConnector(workingChar) || Character.isDigit(workingChar)){ //Alphabetic/numeric type
				if(currentRaw != null){ //Was working on raw; start new phrase + add
					result.add(currentRaw);
					currentRaw = null;

					currentPhrase = new PhraseText(i, i);
					currentPhrase.push(new PhrasePiece(String.valueOf(workingChar), PhrasePiece.Type.WORD));
				} else if (currentPhrase != null) { //Was working on phrase; handle properly
					PhrasePiece piece = currentPhrase.peek();
					if(piece.getType() == PhrasePiece.Type.DELIMITER){ //Add new word
						currentPhrase.push(new PhrasePiece(String.valueOf(workingChar), PhrasePiece.Type.WORD));
						currentPhrase.setEndIndex(i);
					} else if (piece.getType() == PhrasePiece.Type.WORD) {
						piece.append(workingChar);
						currentPhrase.setEndIndex(i);
					}
				} else { //Wasn't working on either; just started processing
					currentPhrase = new PhraseText(i, i);
					currentPhrase.push(new PhrasePiece(String.valueOf(workingChar), PhrasePiece.Type.WORD));
				}
			} else if (workingChar == delimiter){ //Delimiter type
				if(currentRaw != null){ //Was working on raw; can happily append
					currentRaw.append(workingChar);
					currentRaw.setEndIndex(i);
				} else if (currentPhrase != null){
					PhrasePiece piece = currentPhrase.peek();
					if(piece.getType() == PhrasePiece.Type.DELIMITER){
						//Burn it with fire! We have a new word in town; we must pop the delimiter from the
						//last phrase and add it plus the new one to a new RAW chunk
						char orphan = currentPhrase.pop().getContent().charAt(0);

						result.add(currentPhrase);
						currentPhrase = null;

						currentRaw = new RawText(String.valueOf(orphan) + String.valueOf(delimiter), i - 1, i);
					} else if (piece.getType() == PhrasePiece.Type.WORD) {
						currentPhrase.push(new PhrasePiece(String.valueOf(workingChar), PhrasePiece.Type.DELIMITER));
					}
				} else { //Wasn't working on either; just started
					currentRaw = new RawText(String.valueOf(workingChar), i, i);
				}
			} else { //Raw type
				if(currentRaw != null){ //Was working on raw; just continue like normal
					currentRaw.append(workingChar);
					currentRaw.setEndIndex(i);
				} else if (currentPhrase != null){ //End current phrase; add new raw
					result.add(currentPhrase);
					currentPhrase = null;

					currentRaw = new RawText(String.valueOf(workingChar), i, i);
				} else { //Wasn't working on either; just started
					currentRaw = new RawText(String.valueOf(workingChar), i, i);
				}
			}
		}

		if(currentPhrase != null)
			result.add(currentPhrase);
		if(currentRaw != null)
			result.add(currentRaw);

		return result;
	}

	private static List<PhraseText> removeRawFromPreprocess(List<RootText> preprocessed){
		List<PhraseText> result = new ArrayList<>(preprocessed.size());
		for(RootText rt : preprocessed){
			if(rt instanceof PhraseText) result.add( (PhraseText) rt);
		}
		return result;
	}

	/**
	 * Translate a given piece of text.
	 * @param str The string to translate.
	 * @return The translated text in a list. A depth of 0s
	 */
	public static List<List<TranslationChunk>> translate(String str){
		long startTime = System.nanoTime();
		LoadedDict dict = LoadedDict.getInstance();
		List<PhraseText> chunks = removeRawFromPreprocess(preprocess(str));
		List<TranslationChunk> result = new ArrayList<>();

		for(PhraseText pt : chunks){
			int wordCount = 0;
			for(PhrasePiece p : pt.expose()){
				if(p.getType() == PhrasePiece.Type.WORD){
					wordCount++;
				}
			}
			int maxPhraseLength = Math.min(wordCount, dict.getLongestPhrase());

			for(int currentPhraseLength = 0; currentPhraseLength <= maxPhraseLength; currentPhraseLength++){
				List<PhrasePiece> workingElements = new ArrayList<>();
				int workingWordCount = 0; //Number of words we currently have in workingElements
				int startIndex = pt.getStartIndex();

				for(int phraseElement = 0; phraseElement < pt.expose().size(); phraseElement++){
					PhrasePiece p = pt.expose().get(phraseElement);

					if(p.getType() == PhrasePiece.Type.WORD){
						workingElements.add(p);
						workingWordCount++;
					} else {
						if(workingElements.isEmpty()){
							startIndex += p.getLength();
							continue;
						} else { //We have a word -- add it to end
							workingElements.add(p);
						}
					}

					if(workingWordCount == currentPhraseLength){ //The critical point: where we actually check the dictionary
						String construction = "";
						for(int k = 0; k < workingElements.size(); k++){
							construction += workingElements.get(k).getContent();
						}

						Codepoint[] points = dict.getMatch(construction);
						if(points != null && points.length  > 0 && !construction.isEmpty()){
							result.add(new TranslationChunk(points[0].getCode(), points, startIndex, startIndex + construction.length() - 1));
						}

						workingWordCount--;

						PhrasePiece test = workingElements.get(0);
						boolean strippedWord = false;
						while(true){
							if(test.getType() == PhrasePiece.Type.WORD){
								if(strippedWord){
									break;
								} else {
									startIndex += workingElements.remove(0).getLength();
									strippedWord = true;
								}
							} else if (test.getType() == PhrasePiece.Type.DELIMITER){
								startIndex += workingElements.remove(0).getLength();
							}

							if(!workingElements.isEmpty()){
								test = workingElements.get(0);
							} else {
								break;
							}
						}
					}
				}
			}
		}

		//Temp result now has translations only, from short -> long; start marking
		//Generate depth data for translation chunks
		int depths[] = new int[str.length()];
		for(int i = 0; i < str.length(); i++){ depths[i] = 0; }
		for(TranslationChunk chunk : result){
			int highestCoveredPhrase = 0;
			for(int i = chunk.getStartIndex(); i <= chunk.getEndIndex(); i++){
				if(depths[i] > highestCoveredPhrase) highestCoveredPhrase = depths[i];
			}
			int newDepth = highestCoveredPhrase + 1;
			chunk.setDepth(newDepth);
			for(int i = chunk.getStartIndex(); i <= chunk.getEndIndex(); i++){
				if(newDepth > depths[i]) depths[i] = newDepth;
			}
		}

		//Sort into depths
		List<List<TranslationChunk>> resultWithDepths = new ArrayList<>();
		for(TranslationChunk chunk : result){
			while(resultWithDepths.size() - chunk.getDepth() < 0){
				resultWithDepths.add(new ArrayList<TranslationChunk>());
			}
			resultWithDepths.get(chunk.getDepth() - 1).add(chunk);
		}

		//Sort each row based on start elements
		//Sort based on start element
		for(List<TranslationChunk> row : resultWithDepths) {
			Collections.sort(row, new Comparator<TranslationChunk>() {
				@Override
				public int compare(TranslationChunk lhs, TranslationChunk rhs) {
					return lhs.getStartIndex() - rhs.getStartIndex();
				}
			});
		}

		Log.d(TAG, "Took " + (System.nanoTime() - startTime) + " nanoseconds to find translations!");
		return resultWithDepths;
	}
}
