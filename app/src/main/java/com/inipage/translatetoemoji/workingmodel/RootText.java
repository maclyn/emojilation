package com.inipage.translatetoemoji.workingmodel;

/**
 * Literally just so we can all extend from something. Could be an object; might come to share
 * something unique later on, though (for debugging?), hence the custom class.
 */
public class RootText {
	private int startIndex;
	private int endIndex;

	public RootText(int startIndex, int endIndex){
		this.startIndex = startIndex;
		this.endIndex = endIndex;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}
}
