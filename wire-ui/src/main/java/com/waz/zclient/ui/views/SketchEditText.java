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
import android.widget.EditText;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class SketchEditText extends EditText {

    public Set<NoResizeEditTextListener> weakListenerSet;
    private String customHint;

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
                if (charSequence.length() == 0) {
                    setHint(customHint);
                } else {
                    setHint("");
                }
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

    public String getCustomHint() {
        return customHint;
    }

    public void setCustomHint(String customHint) {
        this.customHint = customHint;
        if (getText().length() == 0) {
            setHint(customHint);
        }
    }

    public interface NoResizeEditTextListener {
        void editTextChanged();
    }
}

