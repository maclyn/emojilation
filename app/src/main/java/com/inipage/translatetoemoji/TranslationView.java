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
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.inipage.translatetoemoji.model.Codepoint;
import com.inipage.translatetoemoji.workingmodel.TranslationChunk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslationView extends View {
	public static final String TAG = "TranslationView";

	private float ROW_HEIGHT = getResources().getDimension(R.dimen.emoji_row_depth);
	private float FONT_SIZE = getResources().getDimension(R.dimen.monospaced_text_block);
	private float BOX_THICKNESS = getResources().getDimension(R.dimen.emoji_box_width);
	private float HORIZONTAL_PADDING = getResources().getDimension(R.dimen.emoji_view_horizontal_padding);

	//Fixed dimensions
	private int RECTANGLE_STROKE_COLOR = getResources().getColor(R.color.rectangle_stroke);
	private int RECTANGLE_OPTIONS_STROKE_COLOR = getResources().getColor(R.color.rectangle_stroke_options);
	private int RECTANGLE_SELECTED_FILL_COLOR = getResources().getColor(R.color.rectangle_selected_fill);

	//Dimensions calculated at runtime
	private float perCharacterWidth;
	private float expectedCharacterBaseline;
	private float expectedCharacterRowHeight;
	private float expectedWidth;
	private float expectedHeight;
	private float leftBoxPadding;
	private float topBoxPadding;
	private float bottomBoxPadding;
	private float rightBoxPadding;

	private Paint rectanglePaint;
	private TextPaint characterPaint;
	private TextPaint emojiPaint;
	private TextPaint scrapPaint;

	private Rect tempRect = new Rect();
	private RectF tempRectF = new RectF();
	private Rect scrapRect = new Rect();

	private String mText;
	private List<List<TranslationChunk>> mTranslations;
	private boolean mInited;
	private boolean mReady;

	public TranslationView(Context context, AttributeSet attrs) {
		super(context, attrs);

		characterPaint = new TextPaint();
		characterPaint.setTextSize(FONT_SIZE);
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
		emojiPaint.setTypeface(Typeface.MONOSPACE);
		emojiPaint.setTextAlign(Paint.Align.LEFT);

		scrapPaint = new TextPaint();
	}

	public void setup(String backingText, List<List<TranslationChunk>> translations){
		this.mText = backingText;
		this.mTranslations = translations;
		this.mInited = true;
		this.mReady = false;

		characterPaint.getTextBounds(mText, 0, mText.length(), tempRect);
		expectedCharacterRowHeight = tempRect.height();
		expectedCharacterBaseline = -tempRect.top;

		//Note: Simplifying by making tempRect.width() / mText.length will not yield the right value! We must experimentally measure here
		float[] out = new float[1];
		characterPaint.getTextWidths("a", out);
		perCharacterWidth = out[0];
		
		leftBoxPadding = BOX_THICKNESS + HORIZONTAL_PADDING;
		rightBoxPadding = BOX_THICKNESS + HORIZONTAL_PADDING;
		topBoxPadding = BOX_THICKNESS + (mTranslations.isEmpty() ? 0 : expectedCharacterRowHeight);
		bottomBoxPadding = BOX_THICKNESS;
		expectedWidth = leftBoxPadding + tempRect.width() + rightBoxPadding;
		expectedHeight = topBoxPadding + ((ROW_HEIGHT + BOX_THICKNESS) * mTranslations.size()) + bottomBoxPadding;

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
		canvas.drawText(mText, 0, mText.length(), leftBoxPadding, expectedCharacterBaseline, characterPaint);

		//(2) Draw each row
		float yOffset = topBoxPadding;
		for(int i = 0; i < mTranslations.size(); i++){
			List<TranslationChunk> chunks = mTranslations.get(i);
			for(int j = 0; j < chunks.size(); j++){
				TranslationChunk chunk = chunks.get(j);

				//So... the awkward fact is that, while it might be nice to
				//(2.1) Draw the rectangle for the chunk
				//Bounds are from [startIndex * FONT_SIZE] to [endIndex * FONT_SIZE]
				tempRectF.set(
						leftBoxPadding + (chunk.getStartIndex() * perCharacterWidth),
						yOffset,
						leftBoxPadding + (chunk.getEndIndex() + 1) * perCharacterWidth,
						yOffset + (ROW_HEIGHT + BOX_THICKNESS)
				);

				if(chunk.isSelected()) {
					rectanglePaint.setStyle(Paint.Style.FILL);
					rectanglePaint.setColor(RECTANGLE_SELECTED_FILL_COLOR);
					canvas.drawRect(tempRectF, rectanglePaint);
				}

				if(chunk.getOptions().length == 1) {
					rectanglePaint.setColor(RECTANGLE_STROKE_COLOR);
				} else {
					rectanglePaint.setColor(RECTANGLE_OPTIONS_STROKE_COLOR);
				}
				rectanglePaint.setStyle(Paint.Style.STROKE);
				canvas.drawRect(tempRectF, rectanglePaint);

				//(2.2) Draw the emoji for the chunk (this CAN be totally centered)
				String emojiChars = Utilities.convertDisplayFormatEmojisToString(chunk.getDisplay());
				emojiPaint.setTextSize(calculateTextSizeForWidth(emojiChars, tempRectF.width(), ROW_HEIGHT / 2F));
				emojiPaint.getTextBounds(emojiChars, 0, emojiChars.length(), tempRect);

				//Center vertically and horizontally, relative to tempRectF (the bounds of the box)
				int startX = (int) (tempRectF.right - (tempRectF.width() / 2) - (tempRect.width() / 2));

				//top of text region
				int startY = (int) tempRectF.top;
				startY += (tempRectF.height() - tempRect.height()) / 2;
				startY += (-tempRect.top); //Maybe no plus?
				canvas.drawText(
						emojiChars,
						0,
						emojiChars.length(),
						startX,
						startY,
						emojiPaint);
			}

			yOffset += (ROW_HEIGHT + BOX_THICKNESS);
		}
	}

	//TODO: Credit the SO answer where I found the general algorithm
	private float calculateTextSizeForWidth(String text, float width, float maximumSize){
		final float testSize = 36f;
		scrapPaint.setTextSize(testSize);
		scrapPaint.getTextBounds(text, 0, text.length(), scrapRect);
		float desiredTextSize = testSize * width / scrapRect.width();
		return desiredTextSize > maximumSize ? maximumSize : desiredTextSize;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch(event.getAction()){
			case MotionEvent.ACTION_DOWN:
				return true; //We want to eat this
			case MotionEvent.ACTION_UP:
				//Calculate where we are
				Log.d(TAG, "X/Y: " + event.getX() + ", " + event.getY());

				float x = event.getX();
				float y = event.getY();

				//(1) First, we narrow down by Y coordinates
				if(y < topBoxPadding){
					Log.d(TAG, "Tapped in text");
				} else {
					//We're in a row somewhere; figure out which row
					int row = 0;
					float zoneStart = topBoxPadding;
					float zoneEnd = topBoxPadding + ROW_HEIGHT + BOX_THICKNESS;
					while(zoneStart < getHeight()){
						if(y >= zoneStart && y <= zoneEnd){
							break;
						}

						zoneStart += (ROW_HEIGHT + BOX_THICKNESS);
						zoneEnd += (ROW_HEIGHT + BOX_THICKNESS);
						row++;
					}

					if(row < mTranslations.size()){
						Log.d(TAG, "In row " + row);

						//Calculate which box we're over
						int characterTapped = (int) Math.floor((x - leftBoxPadding) / perCharacterWidth);

						Log.d(TAG, "Tapped character: " + characterTapped);

						TranslationChunk toSelect = null;
						for(TranslationChunk chunk : mTranslations.get(row)){
							if(characterTapped >= chunk.getStartIndex()  && characterTapped <= chunk.getEndIndex()){
								toSelect = chunk;
								break;
							}
						}

						if(toSelect != null){
							Log.d(TAG, "Tapped translation with display: " + toSelect.getDisplay());

							Codepoint[] opts = toSelect.getOptions();
							int index = 0;
							for(int i = 0; i < opts.length; i++){
								if(opts[i].getCode().equals(toSelect.getDisplay())){
									index = i;
									break;
								}
							}

							boolean isSelected = toSelect.isSelected();

							if(isSelected){
								if(index < opts.length - 1){ //Go to next option
									toSelect.setDisplay(opts[index + 1].getCode());
								} else { //Just move to first deselect
									toSelect.setDisplay(opts[0].getCode());
									toSelect.setSelected(false);
								}
							} else { //Just select at first
								toSelect.setDisplay(opts[0].getCode());

								int targetStartIndex = toSelect.getStartIndex();
								int targetEndIndex = toSelect.getEndIndex();

								//Deselect anything below and above
								for (int i = 0; i < mTranslations.size(); i++) {
									if (i == row) continue;

									List<TranslationChunk> chunkRow = mTranslations.get(i);
									for (TranslationChunk chunk : chunkRow) {
										if(!chunk.isSelected()) continue;

										int currentStartIndex = chunk.getStartIndex();
										int currentEndIndex = chunk.getEndIndex();

										if(targetStartIndex >= currentStartIndex && targetStartIndex <= currentEndIndex)
											chunk.setSelected(false);
										else if (targetEndIndex >= currentStartIndex && targetEndIndex <= currentEndIndex)
											chunk.setSelected(false);
									}
								}

								toSelect.setSelected(true);
							}

							invalidate();
						} else {
							Log.d(TAG, "Nothing was selected");
						}
					} else {
						Log.d(TAG, "Out of any rows!");
					}
				}
				return true; //Also doesn't really matter
			case MotionEvent.ACTION_CANCEL:
				return false; //Doesn't matter in any case
		}
		return super.onTouchEvent(event);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);

		if(widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED){
			if(mInited){
				setMeasuredDimension((int) expectedWidth, (int) expectedHeight);
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
