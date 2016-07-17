package com.inipage.translatetoemoji.workingmodel;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class PhraseText extends RootText {
	private List<PhrasePiece> pieces;

	public PhraseText(int startIndex, int endIndex){
		super(startIndex, endIndex);
		this.pieces = new ArrayList<>();
	}

	public void push(PhrasePiece piece){
		pieces.add(piece);
	}

	public PhrasePiece peek(){
		return pieces.get(pieces.size() - 1);
	}

	public PhrasePiece pop(){
		return pieces.remove(pieces.size() - 1);
	}

	/**
	 * Expose the underlying list. Do not edit!
	 * @return The underlying list.
	 */
	public List<PhrasePiece> expose(){
		return pieces;
	}

	/**
	 * Remove the range of values from the underlying.
	 * @param range The range (min/max).
	 */
	public void strip(Pair<Integer, Integer> range) {
		pieces.remove(pieces.subList(range.first, range.second));
	}
}
