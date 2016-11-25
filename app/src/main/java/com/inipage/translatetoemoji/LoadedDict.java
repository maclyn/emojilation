package com.inipage.translatetoemoji;

import android.os.Environment;
import android.util.Log;
import android.util.Pair;

import com.google.gson.Gson;
import com.inipage.translatetoemoji.model.Codepoint;
import com.inipage.translatetoemoji.model.EmojiDictionary;
import com.inipage.translatetoemoji.model.EmojiEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Wraps an EmojiDictionary to allow fast retrieval through use of a tree structure that maps
 * [word] -> codepoint(s) and fetches a sorted list (lazily!).
 */
public class LoadedDict {
    private static final String TAG = "LoadedDict";

    private static LoadedDict instance;

    private EmojiDictionary mDict = null;
	private boolean mIsDictDirty = false;
	private String mFilename;
    private Map<Integer, TreeMap<String, Codepoint[]>> mMaps = null; //List of phrases
    private List<Character> connectors = null;
    private int longestPhrase = 1;

    public static LoadedDict getInstance(){
        return instance == null ? (instance = new LoadedDict()) : instance;
    }

    public void setDictionary(String filename, EmojiDictionary dict){
		mFilename = filename;
        mDict = dict;
		mIsDictDirty = false;
		connectors = new ArrayList<>();

		long startTime = System.nanoTime();

		sortDictionary();
		for(String connector : dict.getConnectors()){
			if(connector != null && !connector.isEmpty()) connectors.add(connector.charAt(0));
		}
		mMaps = new HashMap<>();
		for(EmojiEntry entry : dict.getEmoji()){
			if(entry == null) continue; //We allow this!

			for(String phrase : entry.getPhrases()){
				int length = getLengthOfPhrase(phrase);

				TreeMap<String, Codepoint[]> mapForLength = mMaps.get(length);
				if(mapForLength == null){
					mapForLength = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
					mMaps.put(length, mapForLength);
					if(length > longestPhrase) longestPhrase = length;
				}

				if(mapForLength.containsKey(phrase)){
					Log.w(TAG, "WARNING: Duplicate phrase in EmojiDict!");
					Log.d(TAG, "An existing mapping for " + phrase + " will be replaced!");
				}
				mapForLength.put(phrase, entry.getCodepoints());
			}

		}

		Log.d(TAG, "EmojiDict loaded in " + ((System.nanoTime() - startTime) / 1000) + "ms");
    }

    private int getLengthOfPhrase(String phrase){
        int length = 1;
        if(mDict.getDelimiter() != null && !mDict.getDelimiter().isEmpty()){ //e.g. " " for English
            length = phrase.split(mDict.getDelimiter()).length;
        } else { //e.g. Chinese
            length = phrase.length();
        }
        return length;
    }

	public void deleteEntry(EmojiEntry entry){
		mDict.getEmoji().remove(entry);
		for(String s : entry.getPhrases()){
			int length = getLengthOfPhrase(s);
			TreeMap<String, Codepoint[]> mapForLength = mMaps.get(length);
			if(mapForLength.containsKey(s)) mapForLength.remove(s);
		}
		mIsDictDirty = true;
	}

	public void modifyEntryEmojis(EmojiEntry entry, List<Codepoint> newCodepoints){
		Codepoint[] result = newCodepoints.toArray(new Codepoint[newCodepoints.size()]);
		for(String s : entry.getPhrases()){
			int length = getLengthOfPhrase(s);
			TreeMap<String, Codepoint[]> mapForLength = mMaps.get(length);
			mapForLength.put(s, result);
		}
		entry.setCodepoints(result);
		mIsDictDirty = true;
	}

	public void modifyEntryTags(EmojiEntry entry, List<String> newTags){
		String[] result = newTags.toArray(new String[newTags.size()]);
		entry.setTags(result);
		mIsDictDirty = true;
	}

	public Pair<String, Integer> modifyEntryPhrases(EmojiEntry entry, List<String> newPhrases){
		String[] result = newPhrases.toArray(new String[newPhrases.size()]);

		//Perform a preliminary search to confirm that we aren't creating duplicates
		for(int i = 0; i < LoadedDict.getInstance().exposeEntries().size(); i++){
			EmojiEntry existingEntry = LoadedDict.getInstance().exposeEntries().get(i);
			if(existingEntry.equals(entry)) continue;

			for (String newPhrase : newPhrases) {
				for(String oldPhrase : existingEntry.getPhrases()){
					if(newPhrase.toLowerCase(Locale.getDefault()).equals(oldPhrase.toLowerCase(Locale.getDefault()))){
						return new Pair<>(oldPhrase, LoadedDict.getInstance().exposeEntries().indexOf(existingEntry));
					}
				}
			}
		}

		for(String s : entry.getPhrases()){
			int length = getLengthOfPhrase(s);
			TreeMap<String, Codepoint[]> mapForLength = mMaps.get(length);
			if(mapForLength.containsKey(s)) mapForLength.remove(s);
		}
		for(String s : result){
			int length = getLengthOfPhrase(s);
			TreeMap<String, Codepoint[]> mapForLength = mMaps.get(length);
			if(mapForLength == null){
				mapForLength = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
				mMaps.put(length, mapForLength);
				if(length > longestPhrase) longestPhrase = length;
			}
			mapForLength.put(s, entry.getCodepoints());
		}
		entry.setPhrases(result);
		mIsDictDirty = true;

		return null;
	}

	public String addEntry(EmojiEntry entry){
		for(String s : entry.getPhrases()){
			int length = getLengthOfPhrase(s);
			TreeMap<String, Codepoint[]> mapForLength = mMaps.get(length);
			if(mapForLength == null){
				mapForLength = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
				mMaps.put(length, mapForLength);
				if(length > longestPhrase) longestPhrase = length; //TODO: Since we could theoretically bail later, maybe this is a bad idea?
			}

			if (mapForLength.containsKey(s)){ //We'd be overwriting something -- this isn't new! Ahhh!
				return s;
			}
		}

		//Safe to commit; do so
		for(String s : entry.getPhrases()){
			int length = getLengthOfPhrase(s);
			TreeMap<String, Codepoint[]> mapForLength = mMaps.get(length);
			mapForLength.put(s, entry.getCodepoints());
		}
		mDict.getEmoji().add(entry);
		mIsDictDirty = true;

		return null;
	}

	private void sortDictionary() {
		Collections.sort(mDict.getEmoji(), new Comparator<EmojiEntry>() {
			@Override
			public int compare(EmojiEntry lhs, EmojiEntry rhs) {
				if(lhs.getPhrases().length == 0 || rhs.getPhrases().length == 0) return 0;
				return lhs.getPhrases()[0].compareToIgnoreCase(rhs.getPhrases()[0]);
			}
		});
	}

	public boolean isDictDirty(){
		return mIsDictDirty;
	}

    public EmojiDictionary getDictionary() {
        return mDict;
    }

    public boolean isDictionaryLoaded(){
        return mDict != null;
    }

    public Codepoint[] getMatch(String phrase){
        int length = getLengthOfPhrase(phrase);
        if(mMaps.containsKey(length)){
            if(mMaps.get(length).containsKey(phrase)){
                return mMaps.get(length).get(phrase);
            }
        }

        return null;
    }

	public List<EmojiEntry> exposeEntries(){
		return mDict.getEmoji();
	}

    public boolean isConnector(char character){
        return connectors.contains(character);
    }

    public int getLongestPhrase() {
        return longestPhrase;
    }

	public String reserialize(){
		return new Gson().toJson(mDict, EmojiDictionary.class);
	}

	public String getFilename() {
		return mFilename;
	}

	public boolean saveToDisk(String filename) {
		File saveFile = new File(filename);
		new File(saveFile.getPath().substring(0, saveFile.getPath().lastIndexOf("/"))).mkdirs();

		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(saveFile);
			outputStream.write(reserialize().getBytes());
			outputStream.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public String getAuthor() {
		return mDict.getAuthor();
	}

	public String getLanguage(){
		return mDict.getLanguage();
	}

	public String getLocale() {
		return mDict.getLocale();
	}
}
