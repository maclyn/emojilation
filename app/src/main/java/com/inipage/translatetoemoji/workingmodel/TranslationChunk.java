package com.inipage.translatetoemoji.workingmodel;

import com.inipage.translatetoemoji.model.Codepoint;

/**
 * A processed chunk of text.
 */
public class TranslationChunk {
	private int startIndex;
	private int endIndex;
	private int depth;
	private String display;
	private Codepoint[] options;
	private boolean isSelected;

	public TranslationChunk(String display, Codepoint[] options, int startIndex, int endIndex) {
		this.display = display;
		this.options = options;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.depth = 0;
		this.isSelected = false;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public String getDisplay() {
		return display;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public void setDisplay(String str){
		this.display = str;
	}

	public int getDepth() {
		return depth;
	}

	public void setSelected(boolean selected){
		this.isSelected = selected;
	}

	public boolean isSelected(){
		return isSelected;
	}

	/**
	 * Get the length of original data represented by this chunk (different that the actual length in any resultant output!).
	 * @return The length of the original data.
	 */
	public int getRepresentedLength(){
		return endIndex - startIndex + 1;
	}
}
