package com.inipage.translatetoemoji.utils;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.inipage.translatetoemoji.R;

import java.util.ArrayList;
import java.util.List;

public class RemovableItemDialogFragment extends DialogFragment {
	private static final String TAG = "RemovableItemDialogFrag";

	public static final String ENTRIES_KEY = "entries";
	public static final String ALLOW_EMPTY_KEY = "allow_empty";
	public static final String TITLE_KEY = "title";
	public static final String DONE_BUTTON_TEXT_KEY = "done_button_text";

	RemovableItemAdapter mAdapter;
	RecyclerView mRecyclerView;
	String mTitle;
	String mDoneButtonTitle;
	boolean mAllowEmpty;
	List<String> mEntries;
	RemovableItemDialogStateListener mListener;
	private ItemValidatorInterface mValidator;

	public interface RemovableItemDialogStateListener {
		void onNext(List<String> entries);
		void onGone();
	}

	public static RemovableItemDialogFragment getInstance(ArrayList<String> entries, String title, String doneButtonTitle, boolean allowEmpty){
		RemovableItemDialogFragment fragment = new RemovableItemDialogFragment();
		Bundle data = new Bundle();
		data.putStringArrayList(ENTRIES_KEY, entries);
		data.putString(TITLE_KEY, title);
		data.putString(DONE_BUTTON_TEXT_KEY, doneButtonTitle);
		data.putBoolean(ALLOW_EMPTY_KEY, allowEmpty);
		fragment.setArguments(data);
		return fragment;
	}

	//TODO: Canonical? How should we actually do this
	public void setListener(RemovableItemDialogStateListener listener){
		mListener = listener;
	}

	public void setValidator(ItemValidatorInterface validator) {
		this.mValidator = validator;
		if(mAdapter != null) mAdapter.setValidator(mValidator);
	}

	@Override
	public void setArguments(Bundle args) {
		super.setArguments(args);
		mEntries = args.getStringArrayList(ENTRIES_KEY);
		mAllowEmpty = args.getBoolean(ALLOW_EMPTY_KEY);
		mTitle = args.getString(TITLE_KEY);
		mDoneButtonTitle = args.getString(DONE_BUTTON_TEXT_KEY);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
        mAdapter = new RemovableItemAdapter(getContext(), mEntries, mAllowEmpty);
		if(mValidator != null) mAdapter.setValidator(mValidator);

		View layout = LayoutInflater.from(getContext()).inflate(R.layout.fragment_removable_item_dialog, null, false);
		mRecyclerView = (RecyclerView) layout.findViewById(R.id.dialog_rv);
		mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		mRecyclerView.setAdapter(mAdapter);

		final AlertDialog ad = new AlertDialog.Builder(getContext())
				.setTitle(mTitle)
				.setView(layout)
				.setPositiveButton(mDoneButtonTitle, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(mListener != null){
							mListener.onNext(mEntries);
						}
					}
				})
				.create();
		ad.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				ad.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
			}
		});
		return ad;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		onFinished();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		onFinished();
	}

	private void onFinished() {
		if(mListener != null)
			mListener.onGone();
	}
}
