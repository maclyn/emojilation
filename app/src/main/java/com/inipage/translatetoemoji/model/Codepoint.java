package com.inipage.translatetoemoji.model;

import com.google.gson.annotations.SerializedName;

public class Codepoint {
	private String code;
	@SerializedName("has_modifier")
	private boolean hasModifier;

	public Codepoint(String code, boolean hasModifier) {
		this.code = code;
		this.hasModifier = hasModifier;
	}

	public String getCode() {
		return code;
	}

	public boolean hasModifier() {
		return hasModifier;
	}
}
