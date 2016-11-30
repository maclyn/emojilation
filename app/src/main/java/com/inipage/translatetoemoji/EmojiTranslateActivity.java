package com.inipage.translatetoemoji;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.ActionMenuItem;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.inipage.translatetoemoji.model.Codepoint;
import com.inipage.translatetoemoji.model.EmojiDictionary;
import com.inipage.translatetoemoji.model.EmojiEntry;
import com.inipage.translatetoemoji.workingmodel.RootText;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EmojiTranslateActivity extends AppCompatActivity implements FragmentHostInterface {
	private static final String TAG = "EmojiTranslateActivity";

	public class MainFragmentAdapter extends FragmentPagerAdapter {
		Fragment translateFragment;
		Fragment editFragment;

		public MainFragmentAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
				case 0:
					if (translateFragment == null)
						translateFragment = TranslateFragment.getInstance(initialText, isReadOnly);
					return translateFragment;
				case 1:
					if (editFragment == null) editFragment = EditFragment.getInstance();
					return editFragment;
				default:
					throw new RuntimeException("Invalid page!");
			}
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case 0:
					return getString(R.string.translate);
				case 1:
					return getString(R.string.edit_dictionary);
				default:
					return "";
			}
		}

		@Override
		public int getCount() {
			return 2;
		}
	}

	public static final int READ_DICTIONARY_REQUEST_CODE = 101;
	public static final int WRITE_DICTIONARY_REQUEST_CODE = 102;

	String initialText;
	boolean isReadOnly;

	Toolbar toolbar;
	TabLayout tabs;
	ViewPager pager;
	Menu menu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_emoji_translate);

		if (!LoadedDict.getInstance().isDictionaryLoaded()) {
			finish();
			return;
		}

		toolbar = (Toolbar) findViewById(R.id.toolbar);
		pager = (ViewPager) findViewById(R.id.pager);
		tabs = (TabLayout) findViewById(R.id.tabs);

		initialText = null;
		isReadOnly = false;
		if (getIntent() != null && getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_PROCESS_TEXT)) {
			CharSequence text = getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
			CharSequence textReadOnly = getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT_READONLY);

			if (text != null) {
				initialText = text.toString();
				isReadOnly = false;
			} else if (textReadOnly != null) {
				initialText = textReadOnly.toString();
				isReadOnly = true;
			}

			if(initialText != null){
				initialText = initialText.replace("\r\n", " ");
				initialText = initialText.replace("\n", " ");
				initialText = initialText.replace("\r", " ");
			}
		}

		setSupportActionBar(toolbar);
		pager.setAdapter(new MainFragmentAdapter(getSupportFragmentManager()));
		pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageSelected(int position) {
				invalidateOptionsMenu();
			}

			@Override
			public void onPageScrollStateChanged(int state) {
			}
		});
		tabs.setupWithViewPager(pager);
	}

	@Override
	protected void onResume() {
		super.onResume();
        
        //Remove any pending DialogFragments (ugh, I know)
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment taggedFragment = getSupportFragmentManager().findFragmentByTag("edit_tags");
        if(taggedFragment != null){
            ft.remove(taggedFragment);
            Log.d(TAG, "Tagged found!");
        }
        ft.commit();
	}

	MenuItem actionMenuItem;
	SearchView searchView;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.menu_translate_activity, menu);
		this.menu = menu;

		actionMenuItem = menu.findItem(R.id.search_emoji);
		searchView = (SearchView) actionMenuItem.getActionView();

		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				collapseSearchView();
				EditFragment edit = (EditFragment) ((FragmentPagerAdapter) pager.getAdapter()).getItem(1);
				edit.onQueryTextSubmit(query);
				return true;
			}

			@Override
			public boolean onQueryTextChange(String s) {
				if(s.isEmpty()){ //Initial state!
					SearchView.SearchAutoComplete mSearchSrcTextView = (SearchView.SearchAutoComplete) findViewById(android.support.v7.appcompat.R.id.search_src_text);
					if(mSearchSrcTextView != null){
						mSearchSrcTextView.setTextColor(getResources().getColor(R.color.white));
						mSearchSrcTextView.setHintTextColor(getResources().getColor(R.color.ligher_white));
						mSearchSrcTextView.setHint(R.string.search_for_phrases);
					}
				}
				EditFragment edit = (EditFragment) ((FragmentPagerAdapter) pager.getAdapter()).getItem(1);
				edit.onQueryTextChange(s);
				return false;
			}
		});

		return true;
	}

	private View exposeView(ViewGroup view, Class<?> aClass) {
		for(int i = 0; i < view.getChildCount(); i++){
			View v = view.getChildAt(i);
			Log.d(TAG, "Type: " + v.getClass().getName());
			if(v.getClass().getName().equals(aClass.getName())){
				return v;
			} else if (v instanceof ViewGroup){
				View v2 = exposeView((ViewGroup) v, aClass);
				if(v2 != null) return v2;
			}
		}
		return null;
	}

	public void collapseSearchView(){
		searchView.setIconified(true);
		actionMenuItem.collapseActionView();
	}

	@TargetApi(Build.VERSION_CODES.M)
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.save_emoji:
				if (!Utilities.canReadExternalStorage(this)) {
					requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_DICTIONARY_REQUEST_CODE);
				} else {
					saveDictionary();
				}
				break;
			case R.id.dict_info:
				showDictionaryInfo();
				break;
			case R.id.new_dictionary:
				showNewMenu();
				break;
			case R.id.load_emoji:
				if (!Utilities.canReadExternalStorage(this)) {
					requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, READ_DICTIONARY_REQUEST_CODE);
				} else {
					loadDictionary();
				}

				/*
				//Dumps emoji
				String dump = "";
				//Set emoji
				for(EmojiEntry entry : LoadedDict.getInstance().exposeEntries()) {
					String emojiText = entry.getPhrases()[0] + ": ";
					Codepoint[] codepoints = entry.getCodepoints();
					for (int i = 0; i < codepoints.length; i++) {
						emojiText += Utilities.convertDisplayFormatEmojisToString(codepoints[i].getCode());
						if (i != codepoints.length - 1) emojiText += ", ";
					}
					emojiText += "\n";
					dump += emojiText;
				}

				ScrollView scrollView = new ScrollView(this);
				TextView tv = new TextView(this);
				tv.setText(dump);
				tv.setPadding(50, 50, 50, 50);
				tv.setTextSize(22);
				scrollView.addView(tv);
				new AlertDialog.Builder(this)
						.setTitle("Emoji Dump")
						.setView(scrollView)
						.show();
				break;
				*/
			case R.id.share_dict:
				showShare();
				break;
			case R.id.search_emoji:
				break;
		}
		return true;
	}

	private void showShare() {
		if(Utilities.isDictionaryFromAssets(LoadedDict.getInstance().getFilename()) || LoadedDict.getInstance().isDictDirty()){
			Toast.makeText(this, R.string.save_before_share_message, Toast.LENGTH_SHORT).show();
			saveDictionary();
			return;
		}

		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("application/json");
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, "shared_dict.json");
		shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.download_to_view, Constants.EXTERNAL_STORAGE_PATH));
		shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(LoadedDict.getInstance().getFilename())));
		try {
			startActivity(shareIntent);
		} catch (ActivityNotFoundException anfe) {
			Toast.makeText(this, R.string.no_sharing_activities, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		for (int i = 0; i < menu.size(); i++) {
			menu.getItem(i).setVisible(pager.getCurrentItem() == 1);
		}

		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch(requestCode){
			case READ_DICTIONARY_REQUEST_CODE:
				if(grantResults[0] == PackageManager.PERMISSION_GRANTED) loadDictionary();
				break;
			case WRITE_DICTIONARY_REQUEST_CODE:
				if(grantResults[0] == PackageManager.PERMISSION_GRANTED) saveDictionary();
				break;
		}
	}

	private void showDictionaryInfo(){
		new AlertDialog.Builder(this)
				.setTitle(R.string.dictionary_info)
				.setMessage(getString(R.string.dict_info_block,
						LoadedDict.getInstance().getFilename(),
						LoadedDict.getInstance().getAuthor(),
						LoadedDict.getInstance().getLanguage(),
						LoadedDict.getInstance().getLocale()))
				.setPositiveButton(R.string.done, null)
				.show();
	}

	private void loadDictionary() {
		File dictionaryPath = new File(Environment.getExternalStorageDirectory() + "/" + Constants.EXTERNAL_STORAGE_PATH);
		dictionaryPath.mkdirs();
		final List<String> files = new ArrayList<>();
		if(dictionaryPath.isDirectory()){
			for(File f : dictionaryPath.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					return filename.endsWith(".json");
				}
			})){
				files.add(f.getName());
			}
		}
		files.add("Default Dictionary (English)");
		ArrayAdapter<String> fileAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, files);

		new AlertDialog.Builder(this)
				.setTitle(R.string.load_dictionary)
				.setNegativeButton(R.string.cancel, null)
				.setAdapter(fileAdapter, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(which != files.size() - 1) {
							String filename = Environment.getExternalStorageDirectory() + "/" + Constants.EXTERNAL_STORAGE_PATH + "/" + files.get(which);
							EmojiDictionary dict = Utilities.loadDictionaryFromExternalStorage(filename);
							if (dict != null) {
								LoadedDict.getInstance().setDictionary(filename, dict);
								EditFragment edit = (EditFragment) ((FragmentPagerAdapter) pager.getAdapter()).getItem(1);
								edit.setAdapter();
								Toast.makeText(EmojiTranslateActivity.this, R.string.dictionary_loaded, Toast.LENGTH_SHORT).show();
							} else {
								Toast.makeText(EmojiTranslateActivity.this, R.string.unable_to_load, Toast.LENGTH_SHORT).show();
							}
						} else {
							EmojiDictionary dict = Utilities.loadDictionaryFromAssets(getAssets(), Constants.DEFAULT_DICT);
							if (dict != null) {
								LoadedDict.getInstance().setDictionary(Constants.DEFAULT_DICT, dict);
								EditFragment edit = (EditFragment) ((FragmentPagerAdapter) pager.getAdapter()).getItem(1);
								edit.setAdapter();
								Toast.makeText(EmojiTranslateActivity.this, R.string.dictionary_loaded, Toast.LENGTH_SHORT).show();
							} else {
								Toast.makeText(EmojiTranslateActivity.this, R.string.unable_to_load, Toast.LENGTH_SHORT).show();
							}
						}
					}
				})
				.setNeutralButton(R.string.help, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Toast.makeText(EmojiTranslateActivity.this, R.string.dictionary_load_expl, Toast.LENGTH_SHORT).show();
					}
				})
				.show();
	}


	private void showNewMenu() {
		//Creates a new dictionary; this is rather tough...
	}

	private void saveDictionary() {
		if (Utilities.isDictionaryFromAssets(LoadedDict.getInstance().getFilename())) {
			AlertDialog editTextDialog = Utilities.createEditTextAlertDialog(this, new Utilities.EditTextDialogInterface() {
				@Override
				public boolean onDone(String text) {
					String path = Environment.getExternalStorageDirectory().getPath() + "/" + Constants.EXTERNAL_STORAGE_PATH + "/" + text + ".json";
					if (LoadedDict.getInstance().saveToDisk(path)) {
						Toast.makeText(EmojiTranslateActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
						Utilities.setPreferredDict(EmojiTranslateActivity.this, path);
					} else {
						Toast.makeText(EmojiTranslateActivity.this, R.string.failed_to_save, Toast.LENGTH_SHORT).show();
					}
					return true;
				}

				@Override
				public void onCancelled() {
				}
			}, getString(R.string.save_dictionary), null, getString(R.string.enter_a_filename_without_ext), getString(R.string.save), false);
			editTextDialog.show();
		} else {
			String path = LoadedDict.getInstance().getFilename();
			if (LoadedDict.getInstance().saveToDisk(path)) {
				Toast.makeText(EmojiTranslateActivity.this, R.string.updated_dictionary, Toast.LENGTH_SHORT).show();
				Utilities.setPreferredDict(EmojiTranslateActivity.this, path);
			} else {
				Toast.makeText(EmojiTranslateActivity.this, R.string.failed_to_save, Toast.LENGTH_SHORT).show();
			}
		}
	}
}
