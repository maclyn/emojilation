package com.inipage.translatetoemoji.model;

import com.google.gson.annotations.SerializedName;

public class EmojiEntry {
    private String[] phrases;
	private Codepoint[] codepoints;

    public EmojiEntry(String[] phrases, Codepoint[] codepoints) {
        this.phrases = phrases;
        this.codepoints = codepoints;
    }

    public String[] getPhrases() {
        return phrases;
    }

    public Codepoint[] getCodepoints() {
        return codepoints;
    }

    public void setPhrases(String[] phrases) {
        this.phrases = phrases;
    }

    public void setCodepoints(Codepoint[] codepoints) {
        this.codepoints = codepoints;
    }
}
