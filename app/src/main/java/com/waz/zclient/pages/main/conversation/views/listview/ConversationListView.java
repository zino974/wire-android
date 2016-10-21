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
package com.waz.zclient.pages.main.conversation.views.listview;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class ConversationListView extends ListView {

    private ConversationScrollListener conversationScrollListener = new ConversationScrollListener();
    private boolean blockLayoutChildren;

    public ConversationListView(Context context) {
        super(context);
        setOnScrollListener(conversationScrollListener);
    }

    public ConversationListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnScrollListener(conversationScrollListener);
    }

    public void setBlockLayoutChildren(boolean block) {
        blockLayoutChildren = block;
    }

    @Override
    protected void layoutChildren() {
        if (!blockLayoutChildren) {
            super.layoutChildren();
        }
    }

    public void registerScrolledToBottomListener(ConversationScrollListener.ScrolledToBottomListener listener) {
        conversationScrollListener.registerScrolledToBottomListener(listener);
    }

    public void unregisterScrolledToBottomListener(ConversationScrollListener.ScrolledToBottomListener listener) {
        conversationScrollListener.unregisterScrolledToBottomListener(listener);
    }

    public boolean computeIsScrolledToBottom() {
        return getLastVisiblePosition() == getCount() - 1 &&
               getChildAt(getChildCount() - 1) != null &&
               getChildAt(getChildCount() - 1).getBottom() <= getHeight();
    }

    public void registerVisibleMessagesChangedListener(ConversationScrollListener.VisibleMessagesChangesListener listener) {
        conversationScrollListener.registerVisibleMessagesChangedListener(listener);
    }

    public void unregisterVisibleMessagesChangedListener(ConversationScrollListener.VisibleMessagesChangesListener listener) {
        conversationScrollListener.unregisterVisibleMessagesChangedListener(listener);
    }

    public void registerScrollStateChangeListener(ConversationScrollListener.ScrollStateChangeListener listener) {
        conversationScrollListener.registerScrollStateChangeListener(listener);
    }

    public void unregisterScrollStateChangeListener(ConversationScrollListener.ScrollStateChangeListener listener) {
        conversationScrollListener.unregisterScrollStateChangeListener(listener);
    }


    public void scrollToBottom() {
        if (getAdapter() == null) {
            return;
        }
        // We want to post that so that we can ensure that the listview is ready
        post(new Runnable() {
            @Override
            public void run() {
                int lastItemIndex = getAdapter().getCount() - 1;
                View lastItem = getChildAt(lastItemIndex);
                if (measureItem(lastItem)) {
                    setSelectionFromTop(lastItemIndex, lastItem.getMeasuredHeight());
                } else {
                    setSelection(lastItemIndex);
                }
            }
        });
    }

    private boolean measureItem(View child) {
        if (child == null) {
            return false;
        }
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int childWidthSpec = ViewGroup.getChildMeasureSpec(View.MeasureSpec.makeMeasureSpec(getMeasuredWidth(), View.MeasureSpec.AT_MOST),
                                                           getPaddingLeft() + getMeasuredHeight(), p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
        return true;
    }

}
