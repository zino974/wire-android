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
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.waz.zclient.ui.R;
import com.waz.zclient.ui.views.tab.TabIndicatorLayout;
import com.waz.zclient.utils.Emojis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmojiBottomSheetDialog extends BottomSheetDialog {

    private EmojiSize currentEmojiSize;
    private EmojiDialogListener listener;

    private List<String> emojis;
    private List<Integer> spaces;

    public EmojiBottomSheetDialog(@NonNull Context context, EmojiSize currentEmojiSize, EmojiDialogListener listener, List<String> recent) {
        super(context);
        this.currentEmojiSize = currentEmojiSize;
        this.listener = listener;
        init(recent);
    }

    private void init(List<String> recent) {
        populateEmojis(recent);
        final RecyclerView recyclerView = new RecyclerView(getContext());
        final EmojiAdapter adapter = new EmojiAdapter(getContext());
        final GridLayoutManager layoutManager = new GridLayoutManager(getContext(),
                                                                      getEmojiLayoutManagerSpanCount(),
                                                                      LinearLayoutManager.HORIZONTAL,
                                                                      false);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return spaces.contains(position) ? getEmojiLayoutManagerSpanCount() : 1;
            }
        });

        LinearLayout ll = new LinearLayout(getContext());
        ll.setBackgroundColor(getContext().getResources().getColor(R.color.background_graphite));
        ll.setOrientation(LinearLayout.VERTICAL);
        final TabIndicatorLayout til = new TabIndicatorLayout(getContext());
        int[] labels = new int[3];
        labels[0] = R.string.sketch__emoji_keyboard__size_label__small;
        labels[1] = R.string.sketch__emoji_keyboard__size_label__medium;
        labels[2] = R.string.sketch__emoji_keyboard__size_label__large;
        til.setLabels(labels);
        til.setSelected(currentEmojiSize.ordinal());
        til.setTextColor(getContext().getResources().getColorStateList(R.color.wire__text_color_dark_selector));
        til.setPrimaryColor(getContext().getResources().getColor(R.color.text__primary_dark));
        til.setLabelHeight(getContext().getResources().getDimensionPixelSize(R.dimen.sketch__emoji__keyboard__tab_label_size));
        til.setCallback(new TabIndicatorLayout.Callback() {
            @Override
            public void onItemSelected(int pos) {
                til.setSelected(pos);
                currentEmojiSize = EmojiSize.values()[pos];
                adapter.setEmojiSize(currentEmojiSize);
                setRecyclerViewPadding(recyclerView);
                layoutManager.setSpanCount(getEmojiLayoutManagerSpanCount());
            }
        });
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                   getContext().getResources().getDimensionPixelSize(R.dimen.sketch__emoji__keyboard__tab_size));
        ll.addView(til, params);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        ll.addView(recyclerView);
        setRecyclerViewPadding(recyclerView);
        adapter.setOnEmojiClickListener(new EmojiAdapter.OnEmojiClickListener() {
            @Override
            public void onEmojiClick(String emoji, EmojiSize emojiSize) {
                if (listener != null) {
                    listener.onEmojiSelected(emoji, emojiSize);
                }
                dismiss();
            }
        });
        adapter.setEmojis(emojis, currentEmojiSize);
        setContentView(ll);
    }

    private void populateEmojis(List<String> recent) {
        spaces = new ArrayList<>();
        emojis = new ArrayList<>();
        if (recent != null && recent.size() > 0) {
            emojis.addAll(recent);
            spaces.add(emojis.size());
            emojis.add(EmojiAdapter.SPACE);
        }
        emojis.addAll(Arrays.asList(Emojis.PEOPLE));
        spaces.add(emojis.size());
        emojis.add(EmojiAdapter.SPACE);
        emojis.addAll(Arrays.asList(Emojis.NATURE));
        spaces.add(emojis.size());
        emojis.add(EmojiAdapter.SPACE);
        emojis.addAll(Arrays.asList(Emojis.FOOD_AND_DRINK));
        spaces.add(emojis.size());
        emojis.add(EmojiAdapter.SPACE);
        emojis.addAll(Arrays.asList(Emojis.ACTIVITY));
        spaces.add(emojis.size());
        emojis.add(EmojiAdapter.SPACE);
        emojis.addAll(Arrays.asList(Emojis.TRAVEL_AND_PLACES));
        spaces.add(emojis.size());
        emojis.add(EmojiAdapter.SPACE);
        emojis.addAll(Arrays.asList(Emojis.OBJECTS));
        spaces.add(emojis.size());
        emojis.add(EmojiAdapter.SPACE);
        emojis.addAll(Arrays.asList(Emojis.SYMBOLS));
        spaces.add(emojis.size());
        emojis.add(EmojiAdapter.SPACE);
        emojis.addAll(Arrays.asList(Emojis.FLAGS));
    }

    private void setRecyclerViewPadding(RecyclerView recyclerView) {
        int padding;
        switch (currentEmojiSize) {
            case SMALL:
                padding = getContext().getResources().getDimensionPixelOffset(R.dimen.sketch__emoji__keyboard__item_padding__small);
                break;
            case MEDIUM:
                padding = getContext().getResources().getDimensionPixelOffset(R.dimen.sketch__emoji__keyboard__item_padding__medium);
                break;
            case LARGE:
                padding = getContext().getResources().getDimensionPixelOffset(R.dimen.sketch__emoji__keyboard__item_padding__large);
                break;
            default:
                padding = getContext().getResources().getDimensionPixelOffset(R.dimen.sketch__emoji__keyboard__item_padding__small);
                break;
        }
        int sidePadding = getContext().getResources().getDimensionPixelSize(R.dimen.sketch__emoji__keyboard__side_padding);
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(sidePadding, padding, sidePadding, padding);
    }

    private int getEmojiLayoutManagerSpanCount() {
        switch (currentEmojiSize) {
            case SMALL:
                return 4;
            case MEDIUM:
                return 3;
            case LARGE:
                return 3;
            default:
                return 4;
        }
    }

    public interface EmojiDialogListener {
        void onEmojiSelected(String emoji, EmojiSize emojiSize);
    }

}
