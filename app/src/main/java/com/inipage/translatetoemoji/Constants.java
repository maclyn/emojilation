package com.inipage.translatetoemoji;

import com.inipage.translatetoemoji.model.EmojiDictionaryReference;

import java.util.ArrayList;
import java.util.List;

public class Constants {
	public static List<EmojiDictionaryReference> AVAILABLE_DICTS = new ArrayList<>();

	//TODO: Add a 'populate' method to this
	static {
		AVAILABLE_DICTS.add(new EmojiDictionaryReference("English", "Maclyn", new String[] {"en-US"}, "/assets/english.json"));
	}

	public static final String ASSETS_PREFIX = "/assets/";
	public static final String NO_PREFERRED_DICT = "none";
	public static final String DEFAULT_DICT = "/assets/english.json";
	public static final String DICTIONARY_CHOICE_PREF = "dictionary_pref";
	public static final String EXTERNAL_STORAGE_PATH = "EmojiTranslationDictionaries";
}
