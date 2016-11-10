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
package com.waz.zclient.pages.main.conversation;

import android.content.Context;
import android.os.Parcelable;
import android.view.View;
import com.waz.api.IConversation;
import com.waz.api.Message;
import com.waz.api.MessagesList;
import com.waz.zclient.core.api.scala.ModelObserver;
import com.waz.zclient.pages.main.conversation.views.listview.ConversationListView;
import com.waz.zclient.pages.main.conversation.views.row.message.MessageAndSeparatorViewController;
import com.waz.zclient.utils.LayoutSpec;
import timber.log.Timber;

public class MessageStreamManager {

    private final MessageAdapter adapter;
    private final ConversationListView listView;
    private boolean scrollToLastRead;

    private final ModelObserver<IConversation> conversationObserver = new ModelObserver<IConversation>() {
        @Override
        public void updated(IConversation model, Reason reason) {
            Timber.i("Conversation updated: %s, update reason: %s", model, reason);
            switch (reason) {
                case NEW_MODEL:
                    adapter.resetState();
                    scrollToLastRead = true;
                    break;
                default:
            }
            messagesObserver.setAndUpdate(model.getMessages());
            Timber.i("MessagesObserver listening to: %s", messagesObserver.debugCurentModels());
        }
    };

    private final ModelObserver<MessagesList> messagesObserver = new ModelObserver<MessagesList>() {
        @Override
        public void updated(MessagesList messages) {
            Timber.i("MessagesList with size: %d updated: %s", messages.size(), messages);
            if (messages.size() < 1) {
                return;
            }
            if (scrollToLastRead) {
                scrollToLastRead(messages);
            } else {
                maintainScrollPosition(messages);
            }
        }
    };

    public MessageStreamManager(ConversationListView listView, MessageAdapter adapter) {
        this.listView = listView;
        this.adapter = adapter;
    }

    public void setConversation(IConversation conversation, boolean paused) {
        if (paused) {
            conversationObserver.setAndPause(conversation);
        } else {
            conversationObserver.setAndUpdate(conversation);
        }
    }

    public void resetMessages() {
        adapter.resetState();
    }

    public void onConfigurationChanged(Context context, MessageAdapter messageAdapter) {
        if (LayoutSpec.isTablet(context) && listView != null && messageAdapter != null) {
            final Parcelable onSaveInstanceState = listView.onSaveInstanceState();
            // To clear ListView cache as we use different views for portrait and landscape
            listView.setAdapter(messageAdapter);
            listView.onRestoreInstanceState(onSaveInstanceState);
        }
        adapter.notifyDataSetChanged();
    }

    public int getCount() {
        return adapter.getCount();
    }

    public int getIndexOfMessage(Message message) {
        return adapter.getIndexOfMessage(message);
    }

    public int getUnreadMessageCount() {
        return adapter.getUnreadMessageCount();
    }

    public void onCursorStateEdit() {
        listView.setSelectionFromTop(getCount(), listView.getHeight());
        onScrolledToBottom(true);
    }

    private void scrollToLastRead(MessagesList messages) {
        adapter.setMessages(messages);
        // Need to post to set the message position selection: https://code.google.com/p/android/issues/detail?id=6741
        listView.post(new Runnable() {
            @Override
            public void run() {
                if (adapter.getUnreadMessageCount() == 0) {
                    onCursorStateEdit();
                } else {
                    listView.setSelection(adapter.getLastReadIndex());
                }
            }
        });
    }

    private void maintainScrollPosition(MessagesList messages) {
        final int newScrollPosition = adapter.getIndexOfMessage(getTopVisibleMessage());
        final int topItemOffset = getTopItemOffset();

        Timber.i("Scrolling to last visible - setting scroll position from %d to %d", listView.getFirstVisiblePosition(), newScrollPosition);
        listView.setBlockLayoutChildren(true);
        adapter.setMessages(messages);
        listView.setAdapter(adapter);
        // Need to post to set the message position selection: https://code.google.com/p/android/issues/detail?id=6741
        listView.post(new Runnable() {
            @Override
            public void run() {
                listView.setSelectionFromTop(newScrollPosition, topItemOffset);
                listView.setBlockLayoutChildren(false);
            }
        });
    }

    public void onScrolledToBottom(boolean isScrolledToBottom) {
        this.scrollToLastRead = isScrolledToBottom;
    }

    public void pause() {
        conversationObserver.pauseListening();
        messagesObserver.pauseListening();
    }

    public void resume() {
        messagesObserver.resumeListening();
        conversationObserver.resumeListening();
        conversationObserver.forceUpdate();
        messagesObserver.forceUpdate();
    }

    private Message getTopVisibleMessage() {
        View first = listView.getChildAt(0);
        if (first == null) {
            return null;
        }

        return first.getTag() == null ? null : ((MessageAndSeparatorViewController) first.getTag()).getMessage();
    }

    private int getTopItemOffset() {
        View first = listView.getChildAt(0);
        int topOffset = 0;
        if (first != null) {
            topOffset = first.getTop();
        }
        return topOffset;
    }
}
