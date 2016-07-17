package com.inipage.translatetoemoji.model;

/**
 * Shell reference to an emoji dictionary. Doesn't actually contain the emoji map, but contains the
 * necessary metadata.
 */
public class EmojiDictionaryReference {
    private String language;
    private String author;
    private String[] locales;
    private String file;

    public EmojiDictionaryReference(String language, String author, String[] locales, String file) {
        this.language = language;
        this.author = author;
        this.locales = locales;
    }

    public String getLanguage() {
        return language;
    }

    public String getAuthor() {
        return author;
    }

    public String[] getLocales() {
        return locales;
    }

    public String getFile() {
        return file;
    }
}
