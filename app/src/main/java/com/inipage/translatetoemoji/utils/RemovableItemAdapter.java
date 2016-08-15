package com.inipage.translatetoemoji.utils;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.inipage.translatetoemoji.R;
import com.inipage.translatetoemoji.Utilities;

import java.util.List;

public class RemovableItemAdapter extends RecyclerView.Adapter<RemovableItemAdapter.RemovableItemHolder> {
	public class RemovableItemHolder extends RecyclerView.ViewHolder {
		View itemView;

		//Add entries
		View addGuard;
		View addView;
		EditText addText;
		Button addButton;

		//Standard entries
		TextView contentView;
		View deleteContentView;

		public RemovableItemHolder(View itemView, int viewType) {
			super(itemView);
			this.itemView = itemView;

			if(viewType == VIEW_TYPE_ADD){
				this.addGuard = itemView.findViewById(R.id.add_guard);
				this.addView = itemView.findViewById(R.id.add_view);
				this.addText = (EditText) itemView.findViewById(R.id.content_entry);
				this.addButton = (Button) itemView.findViewById(R.id.add_content_entry);
			} else {
				this.contentView = (TextView) itemView.findViewById(R.id.content_view);
				this.deleteContentView = itemView.findViewById(R.id.remove_content);
			}
		}
	}

	private int VIEW_TYPE_ENTRY = 1;
	private int VIEW_TYPE_ADD = 2;

	private Context mContext;
	private List<String> mEntries;
	private boolean mAllowEmpty;

	public RemovableItemAdapter(Context context, List<String> entries, boolean allowCompletelyEmpty){
		this.mContext = context;
		this.mEntries = entries;
		this.mAllowEmpty = allowCompletelyEmpty;
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
			holder.addGuard.setVisibility(View.VISIBLE);
			holder.addText.setText("");
			holder.addView.setVisibility(View.INVISIBLE);
			holder.addGuard.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ObjectAnimator guardAway = ObjectAnimator.ofFloat(holder.addGuard, "translationY", 0, (float) holder.itemView.getHeight());
					ObjectAnimator editTowards = ObjectAnimator.ofFloat(holder.addView, "translationY", (float) -holder.itemView.getHeight(), 0);
					AnimatorSet set = new AnimatorSet();
					set.playTogether(guardAway, editTowards);
					set.addListener(new Animator.AnimatorListener() {
						@Override
						public void onAnimationStart(Animator animation) {
							holder.addGuard.setVisibility(View.VISIBLE);
							holder.addView.setVisibility(View.VISIBLE);
						}

						@Override
						public void onAnimationEnd(Animator animation) {
							holder.addGuard.setVisibility(View.INVISIBLE);
							holder.addView.setVisibility(View.VISIBLE);
							holder.addGuard.setTranslationY(0F);
							holder.addView.setTranslationY(0F);
						}

						@Override
						public void onAnimationCancel(Animator animation) {
							holder.addGuard.setVisibility(View.INVISIBLE);
							holder.addView.setVisibility(View.VISIBLE);
							holder.addGuard.setTranslationY(0F);
							holder.addView.setTranslationY(0F);
						}

						@Override
						public void onAnimationRepeat(Animator animation) {
						}
					});
					set.setDuration(500L);
					set.start();
				}
			});
			holder.addText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if(actionId == R.id.add_content_entry || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
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
						String entry = holder.addText.getText().toString();
						mEntries.add(entry);

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
}
