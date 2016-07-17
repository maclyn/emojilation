package com.inipage.translatetoemoji;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class DictionaryAdapterViewHolder extends RecyclerView.ViewHolder {
	public TextView entryMetadataTv;
	public TextView entryEmojiTv;
	public TextView entryPhrasesTv;
	public ImageView menuIv;

	public DictionaryAdapterViewHolder(View itemView) {
		super(itemView);

		entryMetadataTv = (TextView) itemView.findViewById(R.id.entry_metadata);
		entryEmojiTv = (TextView) itemView.findViewById(R.id.entry_emoji);
		entryPhrasesTv = (TextView) itemView.findViewById(R.id.entry_phrases);
		menuIv = (ImageView) itemView.findViewById(R.id.entry_edit);
	}
}
