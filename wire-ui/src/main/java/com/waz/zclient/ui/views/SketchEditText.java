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
package com.waz.zclient.ui.views;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.EditText;

import com.waz.zclient.ui.utils.TypefaceUtils;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class SketchEditText extends EditText {

    public Set<NoResizeEditTextListener> weakListenerSet;
    private String customHint;
    private int textFontId;
    private int hintFontId;
    private float regularTextSize;
    private float hintTextSize;

    public SketchEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        weakListenerSet = Collections.newSetFromMap(
            new WeakHashMap<NoResizeEditTextListener, Boolean>());
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                updateField();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        notifyListeners();
    }

    public void addListener(NoResizeEditTextListener listener) {
        weakListenerSet.add(listener);
    }

    public void removeListener(NoResizeEditTextListener listener) {
        weakListenerSet.remove(listener);
    }

    public void notifyListeners() {
        for (NoResizeEditTextListener listener : weakListenerSet) {
            listener.editTextChanged();
        }
    }

    private void updateField() {
        int textLength = getText().length();
        if (textLength > 0) {
            setHint("");
            setTypeface(TypefaceUtils.getTypeface(getContext().getString(textFontId)));
            setTextSize(TypedValue.COMPLEX_UNIT_PX, regularTextSize);
        } else {
            setHint(customHint);
            setTypeface(TypefaceUtils.getTypeface(getContext().getString(hintFontId)));
            setTextSize(TypedValue.COMPLEX_UNIT_PX, hintTextSize);
        }
    }

    public String getCustomHint() {
        return customHint;
    }

    public void setCustomHint(String customHint) {
        this.customHint = customHint;
        if (getText().length() == 0) {
            setHint(customHint);
        }
    }

    public int getTextFontId() {
        return textFontId;
    }

    public void setTextFontId(int textFontId) {
        this.textFontId = textFontId;
        updateField();
    }

    public int getHintFontId() {
        return hintFontId;
    }

    public void setHintFontId(int hintFontId) {
        this.hintFontId = hintFontId;
        updateField();
    }

    public float getRegularTextSize() {
        return regularTextSize;
    }

    public void setRegularTextSize(float textSize) {
        this.regularTextSize = textSize;
        updateField();
    }

    public float getHintTextSize() {
        return hintTextSize;
    }

    public void setHintTextSize(float hintSize) {
        this.hintTextSize = hintSize;
        updateField();
    }

    public interface NoResizeEditTextListener {
        void editTextChanged();
    }
}

