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
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inipage.translatetoemoji.model.Codepoint;
import com.inipage.translatetoemoji.model.EmojiEntry;
import com.inipage.translatetoemoji.utils.DividerItemViewDecoration;
import com.inipage.translatetoemoji.workingmodel.PhrasePiece;
import com.inipage.translatetoemoji.workingmodel.TranslationChunk;

import java.util.ArrayList;
import java.util.List;

public class EditFragment extends Fragment {
	RecyclerView recyclerView;
	RecyclerView suggestionsView;
	FloatingActionButton addPhrase;
	FragmentHostInterface host;

	public static EditFragment getInstance() {
		EditFragment fragment = new EditFragment();

		Bundle args = new Bundle();
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		host = (FragmentHostInterface) activity;
	}

	@Override
	public void setArguments(Bundle args) {
		super.setArguments(args);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View layout = inflater.inflate(R.layout.fragment_edit, container, false);

		recyclerView = (RecyclerView) layout.findViewById(R.id.recycler_view);
		addPhrase = (FloatingActionButton) layout.findViewById(R.id.add_entry);
		suggestionsView = (RecyclerView) layout.findViewById(R.id.suggestions_view);
		suggestionsView.setLayoutManager(new LinearLayoutManager(getContext()));
		suggestionsView.addItemDecoration(new DividerItemViewDecoration(getContext(), LinearLayoutManager.VERTICAL));

		return layout;
	}

	@Override
	public void onResume() {
		super.onResume();

		recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
		setAdapter();
		addPhrase.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startAddEntry();
			}
		});
	}

	private void startAddEntry() {
		Utilities.createEditTextAlertDialog(getContext(), new Utilities.EditTextDialogInterface() {
			@Override
			public boolean onDone(String text) {
				List<String> toSave = new ArrayList<>();
				for(String s : text.split(",")){
					toSave.add(s.trim());
				}
				if(toSave.isEmpty()) {
					Toast.makeText(getContext(), getContext().getString(R.string.must_enter_one_phrase), Toast.LENGTH_SHORT).show();
					return false;
				} else {
					addEmojiToNewEntry(toSave);
					return true;
				}
			}

			@Override
			public void onCancelled() {
			}
		}, getString(R.string.add_phrases), null, getString(R.string.separate_with_commas), getString(R.string.next), false).show();
	}

	private void addEmojiToNewEntry(final List<String> phrases) {
		Utilities.createEditTextAlertDialog(getContext(), new Utilities.EditTextDialogInterface() {
			@Override
			public boolean onDone(String text) {
				List<Codepoint> toSave = new ArrayList<>();
				for(String s : text.split(",")) {
					List<String> emojis = Utilities.createEmojiBlockFromString(s.trim());
					for(String emoji : emojis){
						toSave.add(new Codepoint(Utilities.getDisplayFormatForEmoji(emoji), false));
					}
				}

				if(toSave.isEmpty()) {
					Toast.makeText(getContext(), getContext().getString(R.string.must_one_emoji), Toast.LENGTH_SHORT).show();
					return false;
				} else {
					//Convert lists to arrays
					String[] phrasesArray = phrases.toArray(new String[phrases.size()]);
					Codepoint[] codepointsArray = toSave.toArray(new Codepoint[toSave.size()]);
					LoadedDict.getInstance().addEntry(new EmojiEntry(phrasesArray, codepointsArray));
					recyclerView.getAdapter().notifyDataSetChanged();
					recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount() - 1);
					return true;
				}
			}

			@Override
			public void onCancelled() {
			}
		}, getString(R.string.add_emoji), null, getString(R.string.separate_with_commas), getString(R.string.next), false).show();
	}

	public void setAdapter() {
		recyclerView.setAdapter(new DictionaryAdapter(getContext(), LoadedDict.getInstance().exposeEntries()));
	}

	public void onQueryTextSubmit(String query) { //Pick the top element of the adapter
		suggestionsView.setVisibility(View.GONE);
		addPhrase.show();

		SuggestionsAdapter adapter = (SuggestionsAdapter) suggestionsView.getAdapter();
		int choice = adapter.getCardPosition(0);
		if(choice != -1) recyclerView.scrollToPosition(choice);
	}

	public void onQueryTextChange(String query) {
		if(query.isEmpty()){
			suggestionsView.setVisibility(View.GONE);
			addPhrase.show();
		} else {
			suggestionsView.setVisibility(View.VISIBLE);
			addPhrase.hide();
			suggestionsView.setAdapter(new SuggestionsAdapter(query, new SuggestionsAdapter.OnSuggestionChosenListener() {
				@Override
				public void onSuggestionChosen(int entryIndex) {
					host.collapseSearchView();
					suggestionsView.setVisibility(View.GONE);
					addPhrase.show();
					recyclerView.scrollToPosition(entryIndex);
				}
			}));
		}
	}
}
