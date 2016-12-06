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
package com.waz.zclient.pages.main.conversationlist.views.row;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.waz.api.IConversation;
import com.waz.zclient.R;
import com.waz.zclient.core.stores.connect.InboxLinkConversation;
import com.waz.zclient.core.stores.inappnotification.KnockingEvent;
import com.waz.zclient.pages.main.conversation.ConversationUtils;
import com.waz.zclient.ui.animation.interpolators.penner.Quart;
import com.waz.zclient.ui.text.GlyphTextView;
import com.waz.zclient.utils.ViewUtils;

public class LeftIndicatorView extends FrameLayout {

    private GlyphTextView callIndicator;
    private GlyphTextView pingIndicator;
    private ConversationIndicatorView conversationIndicatorView;

    private View currentVisibleIndicator = null;

    public LeftIndicatorView(Context context, int accentColor) {
        super(context);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                                                             LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.LEFT;
        setLayoutParams(layoutParams);
        LayoutInflater.from(getContext()).inflate(R.layout.conv_list_item_unread, this, true);

        conversationIndicatorView = ViewUtils.getView(this, R.id.civ__list_row);
        callIndicator = ViewUtils.getView(this, R.id.gtv__list__call_indicator);
        pingIndicator = ViewUtils.getView(this, R.id.gtv__list__ping);

        ViewUtils.setPaddingTop(this, getResources().getDimensionPixelSize(R.dimen.calling__conversation_list__margin_top));
        setAccentColor(accentColor);
    }

    /**
     * The color of the circles.
     */
    public void setAccentColor(int accentColor) {
        conversationIndicatorView.setAccentColor(accentColor);
    }

    /**
     * Binds the conversation to this view.
     */
    public void setConversation(final IConversation conversation) {
        conversationIndicatorView.setVisibility(View.GONE);
        callIndicator.setVisibility(GONE);
        pingIndicator.setVisibility(GONE);

        if (conversation.hasUnjoinedCall()) {
            callIndicator.setVisibility(VISIBLE);
            callIndicator.setText(getContext().getString(R.string.glyph__call));
            callIndicator.setTextColor(ContextCompat.getColor(getContext(), R.color.accent_green));
            return;
        }

        if (conversation.hasMissedCall()) {
            callIndicator.setVisibility(VISIBLE);
            callIndicator.setText(getContext().getString(R.string.glyph__end_call));
            callIndicator.setTextColor(ContextCompat.getColor(getContext(), R.color.accent_red));
            return;
        }

        conversationIndicatorView.setVisibility(View.VISIBLE);

        // Outlined indicator for link to connect inbox or outgoing pending connect request
        if (conversation instanceof InboxLinkConversation || conversation.getType() == IConversation.Type.WAIT_FOR_CONNECTION && !conversation.isArchived()) {
            conversationIndicatorView.setState(ConversationIndicatorView.State.PENDING);
            return;
        }

        if (conversation.getFailedCount() > 0) {
            conversationIndicatorView.setState(ConversationIndicatorView.State.UNSENT);
            return;
        }

        conversationIndicatorView.setState(ConversationIndicatorView.State.UNREAD);

        // show unread
        int radius = ConversationUtils.getListUnreadIndicatorRadiusPx(getContext(), conversation.getUnreadCount());

        conversationIndicatorView.setUnreadSize(radius);

        invalidate();
    }

    /**
     * knocking event occured. Fade out unread and pulser and start knocking animation.
     * Afterwards bring the view back into its original state.
     */
    public void knock(KnockingEvent knockingEvent) {
        if (callIndicator.getVisibility() == VISIBLE) {
            currentVisibleIndicator = callIndicator;
        } else {
            currentVisibleIndicator = conversationIndicatorView;
        }
        currentVisibleIndicator.animate()
                               .alpha(0)
                               .setInterpolator(new Quart.EaseOut())
                               .setDuration(getResources().getInteger(R.integer.list_hello_indicator_fade_in_out_animation_duration))
                               .withEndAction(new Runnable() {
                                   @Override
                                   public void run() {
                                       showPing();
                                   }
                               });
        pingIndicator.setTextColor(knockingEvent.getColor());
    }

    private void showPing() {
        pingIndicator.setAlpha(0);
        pingIndicator.setVisibility(VISIBLE);
        pingIndicator.animate().alpha(1)
                     .setInterpolator(new Quart.EaseOut())
                     .setDuration(getResources().getInteger(R.integer.list_hello_indicator_fade_in_out_animation_duration))
                     .withEndAction(
                         new Runnable() {
                             @Override
                             public void run() {
                                 hidePing();
                             }
                         });
    }

    private void hidePing() {
        pingIndicator.animate().alpha(0)
                     .setInterpolator(new Quart.EaseIn())
                     .setDuration(getResources().getInteger(R.integer.list_hello_indicator_fade_in_out_animation_duration))
                     .withEndAction(
                         new Runnable() {
                             @Override
                             public void run() {
                                 resetAfterKnocking();
                             }
                         });
    }

    /**
     * After knocking event has occured, bring the view back to its original state.
     */
    private void resetAfterKnocking() {
        currentVisibleIndicator.animate().alpha(1).setInterpolator(new Quart.EaseOut()).setDuration(getResources().getInteger(R.integer.list_hello_indicator_fade_in_out_animation_duration));
        pingIndicator.setVisibility(GONE);
    }
}
