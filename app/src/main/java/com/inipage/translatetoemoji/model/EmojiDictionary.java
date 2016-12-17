package com.inipage.translatetoemoji.model;

import java.util.List;

public class EmojiDictionary {
	private String language;
	private String author;
	private String delimiter;
	private List<String> connectors;
	private List<EmojiEntry> emoji;

	public String getLanguage() {
		return language;
	}

	public String getAuthor() {
		return author;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public List<EmojiEntry> getEmoji() {
		return emoji;
	}

	public List<String> getConnectors() {
		return connectors;
	}
}
