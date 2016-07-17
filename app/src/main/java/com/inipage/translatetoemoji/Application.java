package com.inipage.translatetoemoji;

import android.widget.Toast;

import com.inipage.translatetoemoji.model.EmojiDictionary;
import com.inipage.translatetoemoji.model.EmojiDictionaryReference;

import java.util.Locale;

public class Application extends android.app.Application {
    private static final String TAG = "Application";

    @Override
    public void onCreate() {
        super.onCreate();

        //Load the appropriate locale for the user's country, or their preference
        String preferredDict = Utilities.getPreferredDict(this);
		EmojiDictionary dict = null;
		if(Utilities.isDictionaryFromAssets(preferredDict)){
			dict = Utilities.loadDictionaryFromAssets(getAssets(), preferredDict);
		} else {
			if(Utilities.canReadExternalStorage(this)){
				dict = Utilities.loadDictionaryFromExternalStorage(preferredDict);
				if(dict == null){
					Toast.makeText(this, R.string.your_default_dictionary_invalid, Toast.LENGTH_SHORT).show();
					preferredDict = Constants.DEFAULT_DICT;
					dict = Utilities.loadDictionaryFromAssets(getAssets(), Constants.DEFAULT_DICT);
				}
			} else {
				Toast.makeText(this, R.string.cant_read_external_storage, Toast.LENGTH_SHORT).show();
				preferredDict = Constants.DEFAULT_DICT;
				dict = Utilities.loadDictionaryFromAssets(getAssets(), Constants.DEFAULT_DICT);
			}
		}

		if(dict != null){
			LoadedDict.getInstance().setDictionary(preferredDict, dict);
		} else {
			Toast.makeText(this, R.string.invalid_package, Toast.LENGTH_SHORT).show();
		}
    }
}
