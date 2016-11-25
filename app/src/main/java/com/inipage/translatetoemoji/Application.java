package com.inipage.translatetoemoji;

import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import com.inipage.translatetoemoji.model.EmojiDictionary;

import java.util.Locale;

public class Application extends android.app.Application {
    private static final String TAG = "Application";

    @Override
    public void onCreate() {
        super.onCreate();

		//Because I'm not going to write a file provider simply because I share a file the user *explicitly* grants access to;
		//for goodness sakes...
		StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
		StrictMode.setVmPolicy(builder.build());

		for(Locale l : Locale.getAvailableLocales()){
			Log.d(TAG, "Locale: " + l.getCountry() + " " + l.getLanguage());
		}

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
