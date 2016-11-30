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
package com.waz.zclient.pages.main.conversation.views.row.message.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import com.waz.api.UpdateListener;
import com.waz.api.User;
import com.waz.zclient.R;
import com.waz.zclient.pages.main.conversation.views.MessageViewsContainer;
import com.waz.zclient.pages.main.conversation.views.row.message.MessageViewController;
import com.waz.zclient.pages.main.conversation.views.row.separator.Separator;
import com.waz.zclient.ui.text.GlyphTextView;
import com.waz.zclient.ui.utils.TextViewUtils;
import com.waz.zclient.utils.ViewUtils;

import java.util.Locale;

public class MissedCallViewController extends MessageViewController implements UpdateListener {

    private View view;
    private TextView missedCallByUserTextView;
    private GlyphTextView missedCallGlyphTextView;
    private User user;
    private Locale locale;

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (footerActionCallback != null) {
                footerActionCallback.toggleVisibility();
            }
        }
    };

    @SuppressLint("InflateParams")
    public MissedCallViewController(Context context, MessageViewsContainer messageViewContainer) {
        super(context, messageViewContainer);
        view = View.inflate(context, R.layout.row_conversation_missed_call, null);
        missedCallByUserTextView = ViewUtils.getView(view, R.id.ttv__row_conversation__missed_call);
        missedCallGlyphTextView = ViewUtils.getView(view, R.id.gtv__row_conversation__missed_call__icon);
        locale = context.getResources().getConfiguration().locale;

        view.setOnClickListener(onClickListener);
    }

    @Override
    protected void onSetMessage(Separator separator) {
        user = message.getUser();
        user.addUpdateListener(this);
        message.addUpdateListener(this);
        updated();
    }

    @Override
    public void recycle() {
        if (user != null) {
            user.removeUpdateListener(this);
            user = null;
        }
        if (message != null) {
            message.removeUpdateListener(this);
        }
        super.recycle();
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void updated() {
        if (message == null ||
            missedCallByUserTextView == null ||
            messageViewsContainer == null ||
            messageViewsContainer.isTornDown()) {
            return;
        }

        if (user == null) {
            missedCallByUserTextView.setText("");
            return;
        }
        if (user.isMe()) {
            missedCallByUserTextView.setText(R.string.content__missed_call__you_called);
            missedCallGlyphTextView.setText(R.string.glyph__call);
            missedCallGlyphTextView.setTextColor(ContextCompat.getColor(context, R.color.accent_green));
        } else {
            String username = message.getUser().getDisplayName();
            if (TextUtils.isEmpty(username)) {
                missedCallByUserTextView.setText("");
            } else {
                missedCallByUserTextView.setText(context.getString(R.string.content__missed_call__xxx_called,
                                                                   username.toUpperCase(locale)));
            }
            missedCallGlyphTextView.setText(R.string.glyph__end_call);
            missedCallGlyphTextView.setTextColor(ContextCompat.getColor(context, R.color.accent_red));
        }

        TextViewUtils.boldText(missedCallByUserTextView);
    }

}
