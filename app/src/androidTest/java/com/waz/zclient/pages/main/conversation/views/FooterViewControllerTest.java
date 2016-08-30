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
package com.waz.zclient.pages.main.conversation.views;

import android.support.test.runner.AndroidJUnit4;
import com.waz.api.Message;
import com.waz.api.User;
import com.waz.zclient.MainTestActivity;
import com.waz.zclient.R;
import com.waz.zclient.pages.main.conversation.views.row.footer.FooterViewController;
import com.waz.zclient.pages.main.conversation.views.row.footer.FooterViewControllerFactory;
import com.waz.zclient.pages.main.conversation.views.row.message.MessageAndSeparatorViewController;
import com.waz.zclient.pages.main.conversation.views.row.message.MessageViewController;
import com.waz.zclient.pages.main.conversation.views.row.message.MessageViewControllerFactory;
import com.waz.zclient.pages.main.conversation.views.row.separator.Separator;
import com.waz.zclient.testutils.ViewTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.threeten.bp.Instant;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.waz.zclient.testutils.CustomViewAssertions.isGone;
import static com.waz.zclient.testutils.CustomViewAssertions.isVisible;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class FooterViewControllerTest extends ViewTest<MainTestActivity> {

    public FooterViewControllerTest() {
        super(MainTestActivity.class);
    }

    @Test
    public void verifyICanLikeMessage() {
        Message message = createMockMessage(Message.Type.TEXT, Message.Status.SENT, false);
        when(message.isLikedByThisUser()).thenReturn(false);
        when(message.isLiked()).thenReturn(false);

        MessageViewsContainer messageViewsContainer = ViewControllerMockHelper.getMockMessageViewsContainer(activity);
        FooterViewController footerViewController = FooterViewControllerFactory.create(activity, message, messageViewsContainer);
        setView(footerViewController.getView());

        footerViewController.setMessage(message);
        footerViewController.toggleVisibility(false);

        onView(withId(R.id.gtv__footer__like__button)).check(isVisible());
        onView(withId(R.id.gtv__footer__like__button)).perform(click());

        verify(message).like();
    }

    @Test
    public void verifyICanLikeMessageThatIsLikedByOthers() {
        Message message = createMockMessage(Message.Type.TEXT, Message.Status.SENT, false);
        when(message.isLikedByThisUser()).thenReturn(false);
        when(message.isLiked()).thenReturn(true);

        MessageViewsContainer messageViewsContainer = ViewControllerMockHelper.getMockMessageViewsContainer(activity);
        FooterViewController footerViewController = FooterViewControllerFactory.create(activity, message, messageViewsContainer);
        setView(footerViewController.getView());

        footerViewController.setMessage(message);
        footerViewController.toggleVisibility(false);

        onView(withId(R.id.gtv__footer__like__button)).check(isVisible());
        onView(withId(R.id.gtv__footer__like__button)).perform(click());

        verify(message).like();
    }

    @Test
    public void verifyICanUnlikeMessage() {
        Message message = createMockMessage(Message.Type.TEXT, Message.Status.SENT, false);
        when(message.isLikedByThisUser()).thenReturn(true);

        MessageViewsContainer messageViewsContainer = ViewControllerMockHelper.getMockMessageViewsContainer(activity);
        FooterViewController footerViewController = FooterViewControllerFactory.create(activity, message, messageViewsContainer);
        setView(footerViewController.getView());

        footerViewController.setMessage(message);
        footerViewController.toggleVisibility(false);

        onView(withId(R.id.gtv__footer__like__button)).check(isVisible());
        onView(withId(R.id.gtv__footer__like__button)).perform(click());

        verify(message).unlike();
    }

    @Test
    public void verifyICanTapMessageToRevealLikeButtonIfMessageHasNoLikes() {
        Message message = createMockMessage(Message.Type.TEXT, Message.Status.SENT, false);
        when(message.isLiked()).thenReturn(false);

        MessageAndSeparatorViewController messageAndSeparatorViewController = createMessageAndSeparatorViewController(message);
        messageAndSeparatorViewController.setModel(message, createMockSeparator());

        setView(messageAndSeparatorViewController.getView());

        onView(withId(R.id.gtv__footer__like__button)).check(isGone());

        onView(withId(R.id.ltv__row_conversation__message)).check(isVisible());
        onView(withId(R.id.ltv__row_conversation__message)).perform(click());

        onView(withId(R.id.gtv__footer__like__button)).check(isVisible());
    }


    private MessageAndSeparatorViewController createMessageAndSeparatorViewController(Message message) {
        MessageViewsContainer messageViewsContainer = ViewControllerMockHelper.getMockMessageViewsContainer(activity);
        MessageViewController viewController = MessageViewControllerFactory.create(activity,
                                                                                   message,
                                                                                   messageViewsContainer);

        FooterViewController footerViewController = FooterViewControllerFactory.create(activity, message, messageViewsContainer);

        return new MessageAndSeparatorViewController(viewController,
                                                     footerViewController,
                                                     messageViewsContainer,
                                                     activity);
    }


    private Separator createMockSeparator() {
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn("123");

        Message message = createMockMessage(Message.Type.TEXT, Message.Status.SENT, false);
        when(message.getUser()).thenReturn(mockUser);

        Separator separator = mock(Separator.class);
        when(separator.getNextMessage()).thenReturn(message);
        when(separator.getPreviousMessage()).thenReturn(message);
        return separator;
    }

    private Message createMockMessage(Message.Type type, Message.Status status, boolean sentByMe) {
        Message message = mock(Message.class);
        when(message.getId()).thenReturn("1234");
        when(message.getMessageType()).thenReturn(type);
        when(message.getMessageStatus()).thenReturn(status);
        when(message.getBody()).thenReturn("Some message");
        when(message.isEdited()).thenReturn(false);
        when(message.getTime()).thenReturn(Instant.now());

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn("123");
        when(mockUser.isMe()).thenReturn(sentByMe);
        when(message.getUser()).thenReturn(mockUser);

        return message;
    }
}
