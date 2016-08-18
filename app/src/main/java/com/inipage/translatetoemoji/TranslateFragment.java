package com.inipage.translatetoemoji;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inipage.translatetoemoji.workingmodel.PhrasePiece;
import com.inipage.translatetoemoji.workingmodel.TranslationChunk;

import java.util.List;

public class TranslateFragment extends Fragment {
	private static final String INITIAL_TEXT_STRING_EXTRA = "initial_text";
	private static final String INITIAL_TEXT_READ_ONLY_BOOLEAN_EXTRA = "is_read_only";

	boolean hasInitialText = false;
	String initialText = null;
	boolean isReadyOnly = false;

	EditText input;
	TranslationView output;
	Button translate;
	Button copyMessage;
	Button replaceMessage;

	public static TranslateFragment getInstance(String text, boolean only) {
		TranslateFragment fragment = new TranslateFragment();

		Bundle args = new Bundle();
		args.putString(INITIAL_TEXT_STRING_EXTRA, text);
		args.putBoolean(INITIAL_TEXT_READ_ONLY_BOOLEAN_EXTRA, only);
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public void setArguments(Bundle args) {
		super.setArguments(args);

		String initialTextExtra = args.getString(INITIAL_TEXT_STRING_EXTRA);
		if(initialTextExtra != null){
			initialText = initialTextExtra;
			hasInitialText = true;
			isReadyOnly = args.getBoolean(INITIAL_TEXT_READ_ONLY_BOOLEAN_EXTRA);
		} else {
			hasInitialText = false;
		}
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View layout = inflater.inflate(R.layout.fragment_translate, container, false);

		input = (EditText) layout.findViewById(R.id.og_message);
		output = (TranslationView) layout.findViewById(R.id.translation_view);
		translate = (Button) layout.findViewById(R.id.translate_button);
		copyMessage = (Button) layout.findViewById(R.id.copy_message);
		replaceMessage = (Button) layout.findViewById(R.id.replace_message);

		translate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				translate();
			}
		});
		input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == R.id.translate_entry || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
					translate();
					return true;
				}
				return false;
			}
		});
		copyMessage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String result = output.generateResult();

				ClipboardManager cm = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData clipData = ClipData.newPlainText(getString(R.string.clipboard_text, input.getText().toString()), result);
				cm.setPrimaryClip(clipData);

				Toast.makeText(v.getContext(), getString(R.string.copied_to_clipboard, output.generateResult()), Toast.LENGTH_SHORT).show();

				getActivity().finish();
			}
		});
		replaceMessage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent resultData = new Intent();
				CharSequence sequence = output.generateResult();
				resultData.putExtra(Intent.EXTRA_PROCESS_TEXT, sequence);
				TranslateFragment.this.getActivity().setResult(Activity.RESULT_OK, resultData);

				getActivity().finish();
			}
		});

		return layout;
	}

	@Override
	public void onResume() {
		super.onResume();

		if(hasInitialText){
			input.setText(initialText);
			replaceMessage.setVisibility(isReadyOnly ? View.GONE : View.VISIBLE);
		} else {
			replaceMessage.setVisibility(View.GONE);
		}
	}

	private void translate() {
		String originalMessage = input.getText().toString();
		if(originalMessage.isEmpty()){
			Toast.makeText(getContext(), R.string.no_message_to_translate, Toast.LENGTH_SHORT).show();
			return;
		}

		final ProgressDialog dialog = ProgressDialog.show(getContext(), getString(R.string.processing_text), getString(R.string.please_wait), true, false);
		new AsyncTask<String, Void, List<List<TranslationChunk>>>(){
			String message;

			@Override
			protected List<List<TranslationChunk>> doInBackground(String... params) {
				message = params[0];
				return Translator.translate(message);
			}

			@Override
			protected void onPostExecute(List<List<TranslationChunk>> lists) {
				output.setup(message, lists);
				output.selectDefaults();

				dialog.dismiss();
			}
		}.execute(originalMessage);
	}
}
