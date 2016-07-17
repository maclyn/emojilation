package com.inipage.translatetoemoji.workingmodel;

public class RawText extends RootText {
	private String text;

	public RawText(String text, int startIndex, int endIndex) {
		super(startIndex, endIndex);
		this.text = text;
	}

	public void append(Character c){
		this.text += c;
	}

	public String getText() {
		return text;
	}
}
