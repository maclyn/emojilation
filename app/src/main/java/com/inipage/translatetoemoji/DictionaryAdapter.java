package com.inipage.translatetoemoji;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.inipage.translatetoemoji.model.Codepoint;
import com.inipage.translatetoemoji.model.EmojiEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DictionaryAdapter extends RecyclerView.Adapter<DictionaryAdapterViewHolder> {
	public static final int VIEW_TYPE_ENTRY = 1;

	List<EmojiEntry> mData;
	Context mContext;

	public DictionaryAdapter(Context context, List<EmojiEntry> data) {
		super();
		this.mData = data;
		this.mContext = context;
	}

	@Override
	public DictionaryAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		switch(viewType){
			case VIEW_TYPE_ENTRY:
				return new DictionaryAdapterViewHolder(inflater.inflate(R.layout.view_dictionary_card, parent, false));
			default:
				return null;
		}
	}

	@Override
	public void onBindViewHolder(DictionaryAdapterViewHolder holder, final int position) {
		final EmojiEntry entry = mData.get(position);
		Context context = holder.entryEmojiTv.getContext();

		//Set metadata
		holder.entryMetadataTv.setText(context.getString(R.string.metadata_content, position + 1, getItemCount()));

		//Set emoji
		String emojiText = "";
		for(Codepoint point : entry.getCodepoints()) {
			emojiText += Utilities.convertDisplayFormatEmojisToString(point.getCode());
		}
		holder.entryEmojiTv.setText(emojiText);

		//Set phrases
		String phraseText = "";
		for(int i = 0; i < entry.getPhrases().length; i++){
			phraseText += entry.getPhrases()[i];
			if(i != entry.getPhrases().length - 1) phraseText += ", ";
		}
		holder.entryPhrasesTv.setText(phraseText);

		//Setup menu
		holder.menuIv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PopupMenu popupMenu = new PopupMenu(v.getContext(), v, Gravity.BOTTOM);
				popupMenu.inflate(R.menu.menu_edit_entry);
				popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						int index = LoadedDict.getInstance().exposeEntries().indexOf(entry);
						switch(item.getItemId()){
							case R.id.edit_phrases:
								displayEditPhrasesDialog(index);
								return true;
							case R.id.edit_emoji:
								displayEditEmojiDialog(index);
								return true;
							case R.id.delete_entry:
								deleteEntry(index);
								return true;
						}
						return false;
					}
				});
				popupMenu.show();
			}
		});
	}

	@Override
	public int getItemCount() {
		return mData.size();
	}

	@Override
	public int getItemViewType(int position) {
		return VIEW_TYPE_ENTRY;
	}

	private void displayEditPhrasesDialog(final int index){
		String prefilled = "";
		for(int i = 0; i < mData.get(index).getPhrases().length; i++){
			String phrase = mData.get(index).getPhrases()[i];
			prefilled += phrase;
			if(i != mData.get(index).getPhrases().length - 1) prefilled += ", ";
		}

		Utilities.createEditTextAlertDialog(
				mContext,
				new Utilities.EditTextDialogInterface() {
					@Override
					public boolean onDone(String text) {
						List<String> toSave = new ArrayList<>();
						for(String s : text.split(",")){
							toSave.add(s.trim());
						}
						if(toSave.isEmpty()) {
							Toast.makeText(mContext, mContext.getString(R.string.must_enter_one_phrase), Toast.LENGTH_SHORT).show();
							return false;
						} else {
							LoadedDict.getInstance().modifyEntryPhrases(mData.get(index), toSave);
							notifyItemChanged(index);
							return true;
						}
					}

					@Override
					public void onCancelled() {
					}
				},
				mContext.getString(R.string.edit_phrases),
				prefilled,
				mContext.getString(R.string.enter_phrases_separated_by),
				mContext.getString(R.string.save),
				false).show();
	}

	private void displayEditEmojiDialog(final int index){
		String prefilled = "";
		for(int i = 0; i < mData.get(index).getCodepoints().length; i++){
			Codepoint point = mData.get(index).getCodepoints()[i];
			prefilled += Utilities.convertDisplayFormatEmojisToString(point.getCode());
			if(i != mData.get(index).getCodepoints().length - 1) prefilled += ", ";
		}

		Utilities.createEditTextAlertDialog(
			mContext,
			new Utilities.EditTextDialogInterface() {
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
						Toast.makeText(mContext, mContext.getString(R.string.must_one_emoji), Toast.LENGTH_SHORT).show();
						return false;
					} else {
						LoadedDict.getInstance().modifyEntryEmojis(mData.get(index), toSave);
						notifyItemChanged(index);
						return true;
					}
				}

				@Override
				public void onCancelled() {
				}
			},
			mContext.getString(R.string.edit_emoji),
			prefilled,
			mContext.getString(R.string.enter_enter_separated_by),
			mContext.getString(R.string.save),
			false).show();
	}

	private void deleteEntry(final int index){
		new AlertDialog.Builder(mContext)
				.setTitle(R.string.delete_entry)
				.setMessage(R.string.are_you_sure_you_wish_to_delete)
				.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						LoadedDict.getInstance().deleteEntry(mData.get(index));
						notifyDataSetChanged();
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).show();
	}
}
