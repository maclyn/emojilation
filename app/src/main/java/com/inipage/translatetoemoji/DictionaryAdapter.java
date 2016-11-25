package com.inipage.translatetoemoji;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.inipage.translatetoemoji.model.Codepoint;
import com.inipage.translatetoemoji.model.EmojiEntry;
import com.inipage.translatetoemoji.utils.ItemValidatorInterface;
import com.inipage.translatetoemoji.utils.RemovableItemDialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DictionaryAdapter extends RecyclerView.Adapter<DictionaryAdapterViewHolder> {
	public static final int VIEW_TYPE_ENTRY = 1;

	List<EmojiEntry> mData;
	Context mContext;
	Fragment mFragment;

	public DictionaryAdapter(Fragment mFragment, List<EmojiEntry> data) {
		super();
		this.mData = data;
		this.mContext = mFragment.getContext();
		this.mFragment = mFragment;
	}

	@Override
	public DictionaryAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		switch (viewType) {
			case VIEW_TYPE_ENTRY:
				return new DictionaryAdapterViewHolder(inflater.inflate(R.layout.view_dictionary_card, parent, false));
			default:
				return null;
		}
	}

	@Override
	public void onBindViewHolder(final DictionaryAdapterViewHolder holder, final int position) {
		final EmojiEntry entry = mData.get(position);
		Context context = holder.entryEmojiTv.getContext();

		//Set metadata
		holder.entryMetadataTv.setText(context.getString(R.string.metadata_content, position + 1, getItemCount()));

		//Set emoji
		String emojiText = "";
		Codepoint[] codepoints = entry.getCodepoints();
		for (int i = 0; i < codepoints.length; i++) {
			emojiText += Utilities.convertDisplayFormatEmojisToString(codepoints[i].getCode());
			if (i != codepoints.length - 1) emojiText += ", ";
		}
		holder.entryEmojiTv.setText(emojiText);

		//Set phrases
		String phraseText = "";
		for (int i = 0; i < entry.getPhrases().length; i++) {
			phraseText += entry.getPhrases()[i];
			if (i != entry.getPhrases().length - 1) phraseText += ", ";
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
						switch (item.getItemId()) {
							case R.id.edit_phrases:
								displayEditPhrasesDialog(index);
								return true;
							case R.id.edit_emoji:
								displayEditEmojiDialog(index);
								return true;
							case R.id.edit_tags:
								displayEditTagsDialog(index);
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
		holder.itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				holder.menuIv.performClick();
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

	private void displayEditPhrasesDialog(final int index) {
		ArrayList<String> phrases = new ArrayList<>();
		for (String s : mData.get(index).getPhrases()) {
			phrases.add(s);
		}
		RemovableItemDialogFragment df = RemovableItemDialogFragment.getInstance(
				phrases,
				mContext.getString(R.string.edit_phrases),
				mContext.getString(R.string.save),
				true);
		df.setListener(new RemovableItemDialogFragment.RemovableItemDialogStateListener() {
			@Override
			public void onNext(List<String> toSave) {
				if (toSave.isEmpty()) {
					Toast.makeText(mContext, mContext.getString(R.string.must_enter_one_phrase), Toast.LENGTH_SHORT).show();
				} else {
					Pair<String, Integer> res = LoadedDict.getInstance().modifyEntryPhrases(mData.get(index), toSave);
					if (res == null) {
						notifyItemChanged(index);
					} else {
						Toast.makeText(mContext, mContext.getString(R.string.phrase_used_here, res.first), Toast.LENGTH_SHORT).show();
						if (mFragment instanceof ScrollableFragment) {
							((ScrollableFragment) mFragment).scrollTo(res.second);
						}
					}
				}
			}

			@Override
			public void onGone() {
			}
		});
		df.setValidator(new ItemValidatorInterface(){
			@Override
			public String validate(String item) {
				Codepoint[] existingEntry = LoadedDict.getInstance().getMatch(item);
				return existingEntry == null ? null : mContext.getString(R.string.this_phrase_already_associated_with,
						Utilities.convertDisplayFormatEmojisToString(existingEntry[0].getCode()));
			}
		});
		df.show(mFragment.getChildFragmentManager(), "edit_phrases");
	}

	private void displayEditEmojiDialog(final int index) {
		ArrayList<String> emoji = new ArrayList<>();
		for (Codepoint cp : mData.get(index).getCodepoints()) {
			emoji.add(Utilities.convertDisplayFormatEmojisToString(cp.getCode()));
		}
		RemovableItemDialogFragment df = RemovableItemDialogFragment.getInstance(
				emoji,
				mContext.getString(R.string.edit_emoji),
				mContext.getString(R.string.save),
				true);
		df.setListener(new RemovableItemDialogFragment.RemovableItemDialogStateListener() {
			@Override
			public void onNext(List<String> entries) {
				List<Codepoint> toSave = new ArrayList<>();
				for (String s : entries) {
					List<String> emojis = Utilities.createEmojiBlockFromString(s);
					String saveFormat = "";
					for (int i = 0; i < emojis.size(); i++) {
						saveFormat += Utilities.getDisplayFormatForEmoji(emojis.get(i));
						if (i != emojis.size() - 1) saveFormat += " ";
					}
					toSave.add(new Codepoint(saveFormat, false));
				}
				if (toSave.isEmpty()) {
					Toast.makeText(mContext, mContext.getString(R.string.must_one_emoji), Toast.LENGTH_SHORT).show();
				} else {
					LoadedDict.getInstance().modifyEntryEmojis(mData.get(index), toSave);
					notifyItemChanged(index);
				}
			}

			@Override
			public void onGone() {
			}
		});
		df.show(mFragment.getChildFragmentManager(), "edit_emoji");
	}

	private void displayEditTagsDialog(final int index) {
		ArrayList<String> tags = new ArrayList<>();
		if(mData.get(index).getTags() != null) {
			for (String s : mData.get(index).getTags()) {
				tags.add(s);
			}
		}
		RemovableItemDialogFragment df = RemovableItemDialogFragment.getInstance(
				tags,
				mContext.getString(R.string.edit_tags),
				mContext.getString(R.string.save),
				true);
		df.setListener(new RemovableItemDialogFragment.RemovableItemDialogStateListener() {
			@Override
			public void onNext(List<String> toSave) {
				LoadedDict.getInstance().modifyEntryTags(mData.get(index), toSave);
                notifyItemChanged(index);
			}

			@Override
			public void onGone() {
			}
		});

		df.setValidator(new ItemValidatorInterface(){
			@Override
			public String validate(String item) {
				return null; //Always validates
			}
		});
		df.show(mFragment.getChildFragmentManager(), "edit_tags");
	}

	private void deleteEntry(final int index) {
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
