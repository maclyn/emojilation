package com.inipage.translatetoemoji;

import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.inipage.translatetoemoji.model.EmojiEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.SuggestionsViewHolder> {
	public class SuggestionsViewHolder extends RecyclerView.ViewHolder {
		View itemView;
		TextView emojiTv;
		TextView phraseTv;

		public SuggestionsViewHolder(View itemView) {
			super(itemView);
			this.itemView = itemView;
			emojiTv = (TextView) itemView.findViewById(R.id.search_emoji);
			phraseTv = (TextView) itemView.findViewById(R.id.search_phrase);
		}
	}

	public interface OnSuggestionChosenListener {
		void onSuggestionChosen(int entryIndex);
	}

	private List<Pair<Integer, EmojiEntry>> entries = new ArrayList<>();
	private OnSuggestionChosenListener mListener;

	public SuggestionsAdapter(String query, OnSuggestionChosenListener listener){
		//TODO: Use a tree for these lookups
		this.mListener = listener;
		for(int i = 0; i < LoadedDict.getInstance().exposeEntries().size(); i++){
			EmojiEntry entry = LoadedDict.getInstance().exposeEntries().get(i);
			phraseSearch: {
				for (String phrase : entry.getPhrases()) {
					if (phrase.toLowerCase(Locale.getDefault()).contains(query)) {
						entries.add(new Pair<>(i, entry));
						break phraseSearch;
					}
				}
			}
		}
	}

	@Override
	public SuggestionsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_dictionary_search, parent, false);
		return new SuggestionsViewHolder(v);
	}

	@Override
	public void onBindViewHolder(SuggestionsViewHolder holder, int position) {
		final Pair<Integer, EmojiEntry> entryPair = entries.get(position);
		if(entryPair.second.getPhrases().length > 0){
			holder.phraseTv.setText(entryPair.second.getPhrases()[0]);
		} else {
			holder.phraseTv.setText(R.string.no_phrase);
		}

		if(entryPair.second.getCodepoints().length > 0){
			holder.emojiTv.setText(Utilities.convertDisplayFormatEmojisToString(entryPair.second.getCodepoints()[0].getCode()));
		} else {
			holder.emojiTv.setText(R.string.no_emojis);
		}

		holder.itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mListener.onSuggestionChosen(entryPair.first);
			}
		});
	}

	@Override
	public int getItemCount() {
		return entries.size();
	}

	public int getCardPosition(int searchEntryPosition){
		return (searchEntryPosition >= entries.size() ? -1 : entries.get(searchEntryPosition).first);
	}
}
