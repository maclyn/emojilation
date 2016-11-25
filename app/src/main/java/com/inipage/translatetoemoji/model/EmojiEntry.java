package com.inipage.translatetoemoji.model;

import com.google.gson.annotations.SerializedName;

public class EmojiEntry {
    private String[] phrases;
    private String[] tags;
	private Codepoint[] codepoints;

    public EmojiEntry(String[] phrases, Codepoint[] codepoints, String[] tags) {
        this.phrases = phrases;
        this.codepoints = codepoints;
        this.tags = tags;
    }

    public String[] getPhrases() {
        return phrases;
    }

    public Codepoint[] getCodepoints() {
        return codepoints;
    }

    public String[] getTags() {
        return tags;
    }

    public void setPhrases(String[] phrases) {
        this.phrases = phrases;
    }

    public void setCodepoints(Codepoint[] codepoints) {
        this.codepoints = codepoints;
    }

    public void setTags(String[] tags){
        this.tags = tags;
    }
}
