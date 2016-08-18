package com.inipage.translatetoemoji;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
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
import com.inipage.translatetoemoji.utils.RemovableItemDialogFragment;
import com.inipage.translatetoemoji.workingmodel.PhrasePiece;
import com.inipage.translatetoemoji.workingmodel.TranslationChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class EditFragment extends Fragment implements ScrollableFragment {
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
		ArrayList<String> entries = new ArrayList<>();
		RemovableItemDialogFragment df = RemovableItemDialogFragment.getInstance(entries, getString(R.string.start_with_a_phrase), getString(R.string.next), true);
		df.setListener(new RemovableItemDialogFragment.RemovableItemDialogStateListener() {
			@Override
			public void onNext(List<String> entries) {
				if (entries.isEmpty()) {
					Toast.makeText(getContext(), R.string.you_must_have_a_phrase, Toast.LENGTH_SHORT).show();
				} else {
					addEmojiToNewEntry(entries);
				}
			}

			@Override
			public void onGone() {
			}
		});
		df.show(getChildFragmentManager(), "add_phrase");
	}


	private void addEmojiToNewEntry(final List<String> phrases) {
		ArrayList<String> entries = new ArrayList<>();
		RemovableItemDialogFragment df = RemovableItemDialogFragment.getInstance(entries, getString(R.string.now_add_some_emoji), getString(R.string.add), true);
		df.setListener(new RemovableItemDialogFragment.RemovableItemDialogStateListener() {
			@Override
			public void onNext(List<String> entries) {
				List<Codepoint> toSave = new ArrayList<>();

				for(String s : entries) {
					List<String> emojis = Utilities.createEmojiBlockFromString(s.trim());
					String saveFormat = "";
					for(int i = 0; i < emojis.size(); i++){
						saveFormat += Utilities.getDisplayFormatForEmoji(emojis.get(i));
						if(i != emojis.size() - 1) saveFormat += " ";
					}
					toSave.add(new Codepoint(saveFormat, false));
				}

				if(entries.isEmpty()) {
					Toast.makeText(getContext(), getContext().getString(R.string.must_one_emoji), Toast.LENGTH_SHORT).show();
				} else {
					//Convert lists to arrays
					String[] phrasesArray = phrases.toArray(new String[phrases.size()]);
					Codepoint[] codepointsArray = toSave.toArray(new Codepoint[toSave.size()]);
					String existingEntry = LoadedDict.getInstance().addEntry(new EmojiEntry(phrasesArray, codepointsArray));
					if(existingEntry == null) {
						Toast.makeText(getContext(), getContext().getString(R.string.entry_added), Toast.LENGTH_SHORT).show();
						recyclerView.getAdapter().notifyDataSetChanged();

						scrollTo(recyclerView.getAdapter().getItemCount() - 1);
					} else {
						Toast.makeText(getContext(), getContext().getString(R.string.you_already_have_an_entry, existingEntry), Toast.LENGTH_SHORT).show();

						int location = -1;
						for(int i = 0; i < LoadedDict.getInstance().exposeEntries().size(); i++){
							EmojiEntry entry = LoadedDict.getInstance().exposeEntries().get(i);
							phraseSearch: {
								for (String phrase : entry.getPhrases()) {
									if (phrase.equals(existingEntry)) {
										location = i;
										break phraseSearch;
									}
								}
							}
						}

						if(location != -1) scrollTo(location);
					}
				}
			}

			@Override
			public void onGone() {
			}
		});
		df.show(getChildFragmentManager(), "add_emoji");
	}

	public void setAdapter() {
		recyclerView.setAdapter(new DictionaryAdapter(this, LoadedDict.getInstance().exposeEntries()));
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

	@Override
	public void scrollTo(final int position) {
		recyclerView.scrollToPosition(position);
		RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);

		//When we scroll to a new item/something far away, there's a decent chance we won't have a ViewHolder yet,
		//so we wait before we try to run the wiggle animation on it
		if(holder != null) Utilities.wiggle(holder.itemView);
		else
			new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
				@Override
				public void run() {
					RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
					if(holder != null) Utilities.wiggle(holder.itemView);
				}
			}, 200);
	}
}
