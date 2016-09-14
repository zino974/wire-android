/**
 * Wire
 * Copyright (C) 2016 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.ui.colorpicker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;
import com.waz.zclient.ui.R;

public class ColorPickerEmojiView extends TextView implements ColorPickerView {

    private EmojiSize emojiSize;
    private String emoji;

    public ColorPickerEmojiView(Context context) {
        this(context, null);
    }

    public ColorPickerEmojiView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPickerEmojiView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        emojiSize = EmojiSize.NONE;
        setGravity(Gravity.CENTER);
        setTextSize(emojiSize.getColorPickerIconSize(getContext()));
        setTextColor(getResources().getColor(R.color.text__primary_light));
    }

    @Override
    public void setSelected(int size) {
        emojiSize = EmojiSize.values()[size];
        setTextSize(emojiSize.getColorPickerIconSize(getContext()));
        invalidate();
    }

    @Override
    public void setUnselected() {
        emojiSize = EmojiSize.NONE;
        setTextSize(emojiSize.getColorPickerIconSize(getContext()));
        invalidate();
    }

    @Override
    public int getSize() {
        return emojiSize.getIconSize(getContext());
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
        setText(emoji);
    }

    public String getEmoji() {
        return emoji;
    }

    public int getEmojiSize() {
        return emojiSize.getEmojiSize(getContext());
    }
}
