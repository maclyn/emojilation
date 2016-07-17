package com.inipage.translatetoemoji.workingmodel;

public class PhrasePiece {
	public enum Type {
		DELIMITER, WORD
	}

	private String content;
	private Type type;

	public PhrasePiece(String content, Type type) {
		this.content = content;
		this.type = type;
	}

	public String getContent() {
		return content;
	}

	public Type getType() {
		return type;
	}

	public void append(char c){
		if(type == Type.DELIMITER) throw new RuntimeException("Cannot append a delimiter; multiple delimters signify new phrases!");
		content += c;
	}

	public int getLength(){
		return getContent().length();
	}
}
