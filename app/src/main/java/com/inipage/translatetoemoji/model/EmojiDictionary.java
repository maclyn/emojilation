package com.inipage.translatetoemoji.model;

import java.util.List;

public class EmojiDictionary {
	private String language;
	private String locale;
	private String author;
	private String delimiter;
	private List<String> connectors;
	private List<EmojiEntry> emoji;

	public EmojiDictionary(String language, String locale, String author, String delimiter, List<String> connectors, List<EmojiEntry> emoji) {
		this.language = language;
		this.locale = locale;
		this.author = author;
		this.delimiter = delimiter;
		this.connectors = connectors;
		this.emoji = emoji;
	}

	public String getLanguage() {
		return language;
	}

	public String getLocale() {
		return locale;
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
