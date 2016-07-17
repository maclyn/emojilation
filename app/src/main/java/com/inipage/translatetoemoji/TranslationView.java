package com.inipage.translatetoemoji;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.inipage.translatetoemoji.workingmodel.TranslationChunk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslationView extends View {
	public static final String TAG = "TranslationView";

	private float ROW_HEIGHT = getResources().getDimension(R.dimen.emoji_row_depth);
	private float LETTER_WIDTH = getResources().getDimension(R.dimen.monospaced_text_block);
	private float BOX_THICKNESS = getResources().getDimension(R.dimen.emoji_box_width);

	private int RECTANGLE_STROKE_COLOR = getResources().getColor(R.color.rectangle_stroke);
	private int RECTANGLE_SELECTED_FILL_COLOR = getResources().getColor(R.color.rectangle_selected_fill);

	private Paint rectanglePaint;
	private TextPaint characterPaint;
	private TextPaint emojiPaint;
	private Rect tempRect = new Rect();
	private RectF tempRectF = new RectF();

	private String mText;
	private List<List<TranslationChunk>> mTranslations;
	private boolean mInited;
	private boolean mReady;

	public TranslationView(Context context, AttributeSet attrs) {
		super(context, attrs);

		characterPaint = new TextPaint();
		characterPaint.setTextSize(LETTER_WIDTH);
		characterPaint.setColor(Color.BLACK);
		characterPaint.setTypeface(Typeface.MONOSPACE);
		characterPaint.setTextAlign(Paint.Align.LEFT);

		rectanglePaint = new Paint();
		rectanglePaint.setAntiAlias(true);
		rectanglePaint.setStrokeWidth(BOX_THICKNESS);
		rectanglePaint.setStyle(Paint.Style.STROKE);
		rectanglePaint.setColor(Color.BLUE);

		emojiPaint = new TextPaint();
		emojiPaint.setTextSize(ROW_HEIGHT / 2f);
		emojiPaint.setColor(Color.BLACK);
		characterPaint.setTypeface(Typeface.MONOSPACE);
		emojiPaint.setTextAlign(Paint.Align.LEFT);
	}

	public void setup(String backingText, List<List<TranslationChunk>> translations){
		this.mText = backingText;
		this.mTranslations = translations;
		this.mInited = true;
		this.mReady = false;

		requestLayout();
		invalidate();
	}

	/**
	 * Unselect all translation data, and then select all the most relevant translation data (i.e. the longest possible translations preferred).
	 */
	public void selectDefaults(){
		boolean[] translatedCharacters = new boolean[mText.length()];
		for(int i = 0; i < translatedCharacters.length; i++){
			translatedCharacters[i] = false;
		}

		for(int i = mTranslations.size() - 1; i >= 0; i--){
			List<TranslationChunk> chunks = mTranslations.get(i);
			for(TranslationChunk chunk : chunks){
				chunk.setSelected(false);

				boolean shouldContinue = false; //TODO: This can be avoided with labels, of course
				for(int j = chunk.getStartIndex(); j <= chunk.getEndIndex(); j++){
					if(translatedCharacters[j]) shouldContinue = true;
				}
				if(shouldContinue) continue;

				chunk.setSelected(true);
				for(int j = chunk.getStartIndex(); j <= chunk.getEndIndex(); j++){
					translatedCharacters[j] = true;
				}
			}
		}

		invalidate();
	}

	public String generateResult(){
		if(!mInited) return "";

		StringBuilder result = new StringBuilder(mText.length());

		Map<Integer, TranslationChunk> chunksToUse = new HashMap<>();
		for(List<TranslationChunk> chunkList : mTranslations){
			for(TranslationChunk chunk : chunkList){
				if(chunk.isSelected()) chunksToUse.put(chunk.getStartIndex(), chunk);
			}
		}

		for(int i = 0; i < mText.length(); i++){
			if(chunksToUse.containsKey(i)){
				TranslationChunk chunk = chunksToUse.get(i);
				result.append(Utilities.convertDisplayFormatEmojisToString(chunk.getDisplay()));
				i = chunk.getEndIndex() + 1;
			} else {
				result.append(mText.charAt(i));
			}
		}

		return result.toString();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if(!mInited) return;
		if(!mReady){
			Paint dirtyPaint = new Paint();
			dirtyPaint.setColor(Color.RED);
			canvas.drawRect(new Rect(0, 0, getWidth(), getHeight()), dirtyPaint);
			return;
		}

		//(1) Draw the text
		//TODO: Align to baseline, not centering (as is, each _character_ is being centered about itself)
		int availableHeight = getHeight();
		int availableWidth = getWidth();
		for(int i = 0; i < mText.length(); i++){
			char[] c = new char[] { mText.charAt(i) };
			characterPaint.getTextBounds(c, 0, 1, tempRect);

			int startX = (int) (i * LETTER_WIDTH);
			int startY = 0;
			canvas.drawText(c, 0, 1, startX + (LETTER_WIDTH / 2) - (tempRect.width() / 2), startY + (LETTER_WIDTH / 2) + (tempRect.height() / 2), characterPaint);
		}

		//(2) Draw each row
		for(int i = 0; i < mTranslations.size(); i++){
			List<TranslationChunk> chunks = mTranslations.get(i);
			for(int j = 0; j < chunks.size(); j++){
				TranslationChunk chunk = chunks.get(j);

				//(2.1) Draw the rectangle for the chunk
				//Bounds are from [startIndex * LETTER_WIDTH] to [endIndex * LETTER_WIDTH]
				tempRectF.set(chunk.getStartIndex() * LETTER_WIDTH, LETTER_WIDTH + (i * ROW_HEIGHT), (chunk.getEndIndex() + 1) * LETTER_WIDTH, LETTER_WIDTH + ((i + 1) * ROW_HEIGHT));

				if(chunk.isSelected()) {
					rectanglePaint.setStyle(Paint.Style.FILL);
					rectanglePaint.setColor(RECTANGLE_SELECTED_FILL_COLOR);
					canvas.drawRect(tempRectF, rectanglePaint);
				}

				rectanglePaint.setColor(RECTANGLE_STROKE_COLOR);
				rectanglePaint.setStyle(Paint.Style.STROKE);
				canvas.drawRect(tempRectF, rectanglePaint);

				//(2.2) Draw the emoji for the chunk (this CAN be totally centered)
				String emojiChars = Utilities.convertDisplayFormatEmojisToString(chunk.getDisplay());
				emojiPaint.getTextBounds(emojiChars, 0, emojiChars.length(), tempRect);

				//Center vertically and horizontally, relative to tempRectF (the bounds of the box)
				int startX = (int) (tempRectF.right - (tempRectF.width() / 2) - (tempRect.width() / 2));
				int startY = (int) ((tempRectF.bottom - (tempRectF.height() / 2)) + (tempRect.height() / 2));
				canvas.drawText(
						emojiChars,
						0,
						emojiChars.length(),
						startX,
						startY,
						emojiPaint);
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);

		if(widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED){
			if(mInited){
				//Calculate needed height; it is ROW_HEIGHT * number of rows + 1
				int neededHeight = (int) ((ROW_HEIGHT * mTranslations.size()) + LETTER_WIDTH);
				int neededWidth = (int) (LETTER_WIDTH * mText.length());
				setMeasuredDimension(neededWidth, neededHeight);
				mReady = true;
			} else {
				mReady = false;
				setMeasuredDimension(0, 0); //Hidden
			}
		} else {
			mReady = false;
			setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
		}
	}
}
