package com.inipage.translatetoemoji.model;

import java.util.ArrayList;
import java.util.List;

public class EmojiDictionary {
	private String language;
	private String author;
	private String delimiter;
	private List<String> connectors;
	private List<EmojiEntry> emoji;

	public EmojiDictionary(String language, String author, String delimiter, List<String> connectors, List<EmojiEntry> emoji) {
		this.language = language;
		this.author = author;
		this.delimiter = delimiter;
		this.connectors = connectors;
		this.emoji = emoji;
	}

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

    /**
     * Clones the dictionary. Clone is sort of a garbage fire, so we're not touching it.
     * @return A cloned dictionary. No links.
     */
    public EmojiDictionary dup() {
        EmojiDictionary copy = new EmojiDictionary(getLanguage(), getAuthor(), getDelimiter(),
            new ArrayList<String>(getConnectors().size()),
            new ArrayList<EmojiEntry>(getEmoji().size()));

        for(String connector : getConnectors()){
            copy.getConnectors().add(connector);
        }
        for(EmojiEntry entry : getEmoji()){
            String[] phrases = entry.getPhrases();
            Codepoint[] emoji = entry.getCodepoints();
            String[] tags = entry.getTags();

            Codepoint[] emojiCopy = new Codepoint[emoji.length];
            for(int i = 0; i < emoji.length; i++){
                emojiCopy[i] = new Codepoint(emoji[i].getCode(), emoji[i].hasModifier());

            }
            copy.getEmoji().add(new EmojiEntry(phrases, emojiCopy, tags));
        }

        return copy;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
