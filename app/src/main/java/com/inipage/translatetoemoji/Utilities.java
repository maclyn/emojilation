package com.inipage.translatetoemoji;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.inipage.translatetoemoji.model.EmojiDictionary;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Utilities {
	private static final String TAG = "Utilities";

	//TODO: These are only approximate
	private static final int MIN_EMOJI_CODEPOINT = 0x1F300;
	private static final int MAX_EMOJI_CODEPOINT = 0x1F700;

	private static final int MIN_SYMBOL_CODEPOINT = 0x2500;
	private static final int MAX_SYMBOL_CODEPOINT = 0x2800;

	public static String getPreferredDict(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.DICTIONARY_CHOICE_PREF, Constants.DEFAULT_DICT);
	}

	public static void setPreferredDict(Context context, String dict) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putString(Constants.DICTIONARY_CHOICE_PREF, dict).apply();
	}

	public static boolean canReadExternalStorage(Context mContext) {
		return ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) ==
				PackageManager.PERMISSION_GRANTED;
	}

	public static EmojiDictionary loadDictionaryFromAssets(AssetManager am, String path) {
		try {
			InputStream assetDict = am.open(path.replace(Constants.ASSETS_PREFIX, ""));
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			int bytesRead = 0;
			byte[] buffer = new byte[2048];
			while ((bytesRead = assetDict.read(buffer)) != -1) {
				output.write(buffer, 0, bytesRead);
			}
			return new Gson().fromJson(new String(output.toByteArray()), EmojiDictionary.class);
		} catch (Exception ignored) {
		}
		return null;
	}

	public static boolean isDictionaryFromAssets(String dict){
		return dict.startsWith(Constants.ASSETS_PREFIX);
	}

	public static EmojiDictionary loadDictionaryFromExternalStorage(String path) {
		try {
			File location = new File(path);

			InputStream fis = new BufferedInputStream(new FileInputStream(location));
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			int bytesRead = 0;
			byte[] buffer = new byte[8192];
			while ((bytesRead = fis.read(buffer)) != -1) {
				output.write(buffer, 0, bytesRead);
			}
			return new Gson().fromJson(new String(output.toByteArray()), EmojiDictionary.class);
		} catch (Exception ignored) {
		}
		return null;
	}

	/**
	 * Get a string of emoji from our custom block format ("1F343 1F5454", etc.).
	 * @param display The emoji in display format.
	 * @return The emoji as a normal, usable String.
	 */
	public static String convertDisplayFormatEmojisToString(String display){
		String[] emojiInHexString = display.split(" ");
		String emojiChars = "";
		for(int k = 0; k < emojiInHexString.length; k++){
			int value = Integer.parseInt(emojiInHexString[k], 16);
			emojiChars += String.copyValueOf(Character.toChars(value));
		}
		return emojiChars;
	}

	/**
	 * Convert emoji in standard format (as a String) to display format (a String using ASCII; e.g. "1F65AS 1F3342").
	 * @param emoji The emoji in String format.
	 * @return The emoji in display format.
	 */
	public static String getDisplayFormatForEmoji(String emoji){
		String display = "";
		List<String> blocks = createEmojiBlockFromString(emoji);
		for(int i = 0; i < blocks.size(); i++){
			String block = blocks.get(i);
			if(block.length() == 1){ //UTF-8
				display += Integer.toHexString(block.charAt(0)).toUpperCase(Locale.US);
			} else if (block.length() == 2){ //UTF-16
				display += (Integer.toHexString(Character.toCodePoint(block.charAt(0), block.charAt(1))).toUpperCase(Locale.US));
			}
			if(i != blocks.size() - 1) display += " ";
		}
		return display;
	}

	/**
	 * Creates "blocks" of different emojis from a given string.
	 * @param block The given string.
	 * @return Ready to display string of emojis.
	 */
	public static List<String> createEmojiBlockFromString(String block){
		List<String> emoji = new ArrayList<>();
		for(int i = 0; i < block.length(); i++){
			boolean canOnlyMakeUtf8 = (i == block.length() - 1 || !Character.isSurrogatePair(block.charAt(i), block.charAt(i+1)));
			int utf8Codepoint = block.charAt(i); //Needed for some symbols, unfortunately
			if(canOnlyMakeUtf8){
				emoji.add(new String(Character.toChars(utf8Codepoint)));
			} else {
				int utf16Codepoint = Character.toCodePoint(block.charAt(i), block.charAt(i+1));
				emoji.add(new String(Character.toChars(utf16Codepoint)));
				i++; //So we'll be (2) ahead the next time
			}
		}
		return emoji;
	}

	public static void showKeyboard(Context context) {
		InputMethodManager inputMananger = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMananger.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
	}

	public static void hideKeyboard(Context context) {
		InputMethodManager inputMananger = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMananger.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);
	}

	public interface EditTextDialogInterface {
		boolean onDone(String text);
		void onCancelled();
	}

	public static AlertDialog createEditTextAlertDialog(final Context context,
														final EditTextDialogInterface dialogInterface,
														String title,
														@Nullable String defaultText,
														String hint,
														String done,
														final boolean allowEmptyFields){
		View layout = LayoutInflater.from(context).inflate(R.layout.dialog_edittext, null);
		final EditText editText = (EditText) layout.findViewById(R.id.edit_text);
		if(defaultText != null) editText.setText(defaultText);
		editText.setHint(hint);
		return new AlertDialog.Builder(context)
				.setTitle(title)
				.setView(layout)
				.setCancelable(false)
				.setPositiveButton(done, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String result = editText.getText().toString();
						if(allowEmptyFields || !result.isEmpty())
							if(dialogInterface.onDone(editText.getText().toString())) dialog.dismiss();
						else
							Toast.makeText(context, R.string.you_must_enter, Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						dialogInterface.onCancelled();
					}
				}).create();
	}

	public static void wiggle(View view){
		view.setPivotX(view.getWidth() / 2);
		view.setPivotY(view.getHeight() / 2);

		ObjectAnimator wiggle = ObjectAnimator.ofFloat(view, "rotation", -10F).setDuration(250);
		ObjectAnimator wobble = ObjectAnimator.ofFloat(view, "rotation", 10F).setDuration(250);
		ObjectAnimator lame = ObjectAnimator.ofFloat(view, "rotation", 0F).setDuration(250);

		AnimatorSet theThing = new AnimatorSet();
		theThing.setInterpolator(new BounceInterpolator());
		theThing.playSequentially(wiggle, wobble, lame);
		theThing.start();
	}
}
