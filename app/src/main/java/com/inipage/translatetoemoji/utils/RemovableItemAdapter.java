package com.inipage.translatetoemoji.utils;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.inipage.translatetoemoji.R;
import com.inipage.translatetoemoji.Utilities;

import java.util.List;
import java.util.Locale;

public class RemovableItemAdapter extends RecyclerView.Adapter<RemovableItemAdapter.RemovableItemHolder> {
	public class RemovableItemHolder extends RecyclerView.ViewHolder {
		View itemView;

		//Add entries
		View addView;
		EditText addText;
		Button addButton;

		//Standard entries
		TextView contentView;
		View deleteContentView;
		View cloneContentView;

		public RemovableItemHolder(View itemView, int viewType) {
			super(itemView);
			this.itemView = itemView;

			if(viewType == VIEW_TYPE_ADD){
				this.addView = itemView.findViewById(R.id.add_view);
				this.addText = (EditText) itemView.findViewById(R.id.content_entry);
				this.addButton = (Button) itemView.findViewById(R.id.add_content_entry);
			} else {
				this.contentView = (TextView) itemView.findViewById(R.id.content_view);
				this.deleteContentView = itemView.findViewById(R.id.remove_content);
				this.cloneContentView = itemView.findViewById(R.id.clone_content);
			}
		}
	}

	private int VIEW_TYPE_ENTRY = 1;
	private int VIEW_TYPE_ADD = 2;

	private Context mContext;
	private List<String> mEntries;
	private boolean mAllowEmpty;
	private String cachedClone;
	private ItemValidatorInterface mValidator;

	public RemovableItemAdapter(Context context, List<String> entries, boolean allowCompletelyEmpty){
		this.mContext = context;
		this.mEntries = entries;
		this.mAllowEmpty = allowCompletelyEmpty;
		cachedClone = null;
	}

	@Override
	public RemovableItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		int layout = viewType == VIEW_TYPE_ENTRY ? R.layout.view_removable_item : R.layout.view_add_item;
		return new RemovableItemHolder(LayoutInflater.from(mContext).inflate(layout, parent, false), viewType);
	}

	@Override
	public void onBindViewHolder(final RemovableItemHolder holder, int position) {
		int viewType = getItemViewType(position);

		if(viewType == VIEW_TYPE_ADD) {
			if(cachedClone == null) {
				holder.addText.setText("");
			} else {
				holder.addText.setText(cachedClone);
				holder.addText.setSelection(cachedClone.length());
				cachedClone = null;
			}
			holder.addText.requestFocus();
			holder.addText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if(actionId == R.id.add_content_entry || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
						holder.addButton.performClick();
						return true;
					}
					return false;
				}
			});
			holder.addButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(!holder.addText.getText().toString().isEmpty()){
						String toAdd = holder.addText.getText().toString();
						for(String s : mEntries) {
							if (s.toLowerCase(Locale.getDefault()).equals(toAdd.toLowerCase(Locale.getDefault()))) {
								Utilities.wiggle(holder.addText);
								Toast.makeText(mContext, R.string.already_have_one, Toast.LENGTH_LONG).show();
								return;
							}
						}
						String response;
						if(mValidator != null && (response = mValidator.validate(toAdd)) != null){
							Utilities.wiggle(holder.addText);
							Toast.makeText(mContext, response, Toast.LENGTH_LONG).show();
							return;
						}

						mEntries.add(toAdd);

						notifyItemInserted(mEntries.size() - 1);
						notifyItemChanged(mEntries.size());
					}
				}
			});
		} else if (viewType == VIEW_TYPE_ENTRY) {
			final String entry = mEntries.get(position);
			holder.contentView.setText(entry);
			holder.deleteContentView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(mEntries.size() == 1 && !mAllowEmpty)
						return;

					int position = holder.getAdapterPosition();
					mEntries.remove(position); //Okay thanks to forced rebinding (still slow though)
					notifyItemRemoved(position);
				}
			});
			holder.cloneContentView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					cachedClone = holder.contentView.getText().toString();
					notifyItemChanged(mEntries.size());
				}
			});
		}
	}

	@Override
	public int getItemCount() {
		return mEntries.size() + 1;
	}

	@Override
	public int getItemViewType(int position) {
		return position >= mEntries.size() ? VIEW_TYPE_ADD : VIEW_TYPE_ENTRY;
	}

	public void setValidator(ItemValidatorInterface mValidator) {
		this.mValidator = mValidator;
	}
}
