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

import android.support.test.runner.AndroidJUnit4;
import com.waz.api.IConversation;
import com.waz.api.Message;
import com.waz.api.User;
import com.waz.zclient.MainTestActivity;
import com.waz.zclient.R;
import com.waz.zclient.core.stores.conversation.IConversationStore;
import com.waz.zclient.testutils.FragmentTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.threeten.bp.Instant;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.waz.zclient.testutils.CustomViewAssertions.isNull;
import static com.waz.zclient.testutils.CustomViewAssertions.isVisible;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class MessageActionsTest extends FragmentTest<MainTestActivity> {

    public MessageActionsTest() {
        super(MainTestActivity.class);
    }

    @Test
    public void assertTextMessageByMeIsEditableInOneToOneConversation() throws InterruptedException {
        IConversation mockConversation = mock(IConversation.class);
        when(mockConversation.getType()).thenReturn(IConversation.Type.ONE_TO_ONE);
        when(mockConversation.isMemberOfConversation()).thenReturn(true);

        IConversationStore mockConversationStore = activity.getStoreFactory().getConversationStore();
        when(mockConversationStore.getCurrentConversation()).thenReturn(mockConversation);

        User mockUser = mock(User.class);
        when(mockUser.isMe()).thenReturn(true);

        final Message mockMessage = mock(Message.class);
        when(mockMessage.getMessageType()).thenReturn(Message.Type.TEXT);
        when(mockMessage.getUser()).thenReturn(mockUser);
        when(mockMessage.getEditTime()).thenReturn(Instant.now());

        ConversationFragment fragment = ConversationFragment.newInstance();
        attachFragment(fragment, ConversationFragment.TAG);
        fragment.onItemLongClick(mockMessage);

        Thread.sleep(500);
        onView(withId(R.id.message_bottom_menu_item_edit)).check(isVisible());
        onView(withId(R.id.message_bottom_menu_item_edit)).perform(click());

        Thread.sleep(800);
        onView(withId(R.id.emct__edit_message__toolbar)).check(isVisible());
    }

    @Test
    public void assertTextMessageByMeIsEditableInGroupConversation() throws InterruptedException {
        IConversation mockConversation = mock(IConversation.class);
        when(mockConversation.getType()).thenReturn(IConversation.Type.GROUP);
        when(mockConversation.isMemberOfConversation()).thenReturn(true);

        IConversationStore mockConversationStore = activity.getStoreFactory().getConversationStore();
        when(mockConversationStore.getCurrentConversation()).thenReturn(mockConversation);

        User mockUser = mock(User.class);
        when(mockUser.isMe()).thenReturn(true);

        final Message mockMessage = mock(Message.class);
        when(mockMessage.getMessageType()).thenReturn(Message.Type.TEXT);
        when(mockMessage.getUser()).thenReturn(mockUser);

        ConversationFragment fragment = ConversationFragment.newInstance();
        attachFragment(fragment, ConversationFragment.TAG);
        fragment.onItemLongClick(mockMessage);

        Thread.sleep(500);
        onView(withId(R.id.message_bottom_menu_item_edit)).check(isVisible());
        onView(withId(R.id.message_bottom_menu_item_edit)).perform(click());

        Thread.sleep(800);
        onView(withId(R.id.emct__edit_message__toolbar)).check(isVisible());
    }

    @Test
    public void assertTextMessageBySomeoneElseIsNotEditable() throws InterruptedException {
        IConversation mockConversation = mock(IConversation.class);
        when(mockConversation.getType()).thenReturn(IConversation.Type.ONE_TO_ONE);
        when(mockConversation.isMemberOfConversation()).thenReturn(true);

        IConversationStore mockConversationStore = activity.getStoreFactory().getConversationStore();
        when(mockConversationStore.getCurrentConversation()).thenReturn(mockConversation);

        User mockUser = mock(User.class);
        when(mockUser.isMe()).thenReturn(false);

        final Message mockMessage = mock(Message.class);
        when(mockMessage.getMessageType()).thenReturn(Message.Type.TEXT);
        when(mockMessage.getUser()).thenReturn(mockUser);

        ConversationFragment fragment = ConversationFragment.newInstance();
        attachFragment(fragment, ConversationFragment.TAG);
        fragment.onItemLongClick(mockMessage);

        Thread.sleep(500);
        onView(withId(R.id.message_bottom_menu_item_edit)).check(isNull());
    }

    @Test
    public void assertImageByMeIsNotEditable() throws InterruptedException {
        IConversation mockConversation = mock(IConversation.class);
        when(mockConversation.getType()).thenReturn(IConversation.Type.ONE_TO_ONE);
        when(mockConversation.isMemberOfConversation()).thenReturn(true);

        IConversationStore mockConversationStore = activity.getStoreFactory().getConversationStore();
        when(mockConversationStore.getCurrentConversation()).thenReturn(mockConversation);

        User mockUser = mock(User.class);
        when(mockUser.isMe()).thenReturn(false);

        final Message mockMessage = mock(Message.class);
        when(mockMessage.getMessageType()).thenReturn(Message.Type.ASSET);
        when(mockMessage.getUser()).thenReturn(mockUser);

        ConversationFragment fragment = ConversationFragment.newInstance();
        attachFragment(fragment, ConversationFragment.TAG);
        fragment.onItemLongClick(mockMessage);

        Thread.sleep(500);
        onView(withId(R.id.message_bottom_menu_item_edit)).check(isNull());
    }
}
