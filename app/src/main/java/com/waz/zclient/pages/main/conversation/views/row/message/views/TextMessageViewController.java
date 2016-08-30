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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.waz.api.Message;
import com.waz.zclient.R;
import com.waz.zclient.core.api.scala.ModelObserver;
import com.waz.zclient.pages.main.conversation.views.MessageViewsContainer;
import com.waz.zclient.pages.main.conversation.views.row.message.RetryMessageViewController;
import com.waz.zclient.pages.main.conversation.views.row.separator.Separator;
import com.waz.zclient.ui.utils.ResourceUtils;
import com.waz.zclient.utils.ViewUtils;

public class TextMessageViewController extends RetryMessageViewController {

    private View view;
    private TextView textView;

    private ModelObserver<Message> messageModelObserver = new ModelObserver<Message>() {
        @Override
        public void updated(Message message) {
            textView.setText(message.getBody());
        }
    };

    @SuppressLint("InflateParams")
    public TextMessageViewController(Context context, final MessageViewsContainer messageViewContainer) {
        super(context, messageViewContainer);
        LayoutInflater inflater = LayoutInflater.from(context);
        view = inflater.inflate(R.layout.row_conversation_text_message, null);
        textView = ViewUtils.getView(view, R.id.ltv__row_conversation__message);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (footerActionCallback != null) {
                    footerActionCallback.toggleVisibility(true);
                }
            }
        });
        afterInit();
    }

    @Override
    public void onSetMessage(Separator separator) {
        super.onSetMessage(separator);
        messageModelObserver.setAndUpdate(message);
    }

    @Override
    protected void updateMessageEditingStatus() {
        super.updateMessageEditingStatus();
        float opacity = messageViewsContainer.getControllerFactory().getConversationScreenController().isMessageBeingEdited(message) ?
                        ResourceUtils.getResourceFloat(context.getResources(), R.dimen.content__youtube__alpha_overlay) :
                        1f;
        textView.setAlpha(opacity);
    }

    @Override
    protected void onHeaderClick() {
        if (message.getMessageType() == Message.Type.RECALLED && footerActionCallback != null) {
            footerActionCallback.toggleVisibility(true);
        }
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void recycle() {
        messageModelObserver.clear();
        super.recycle();
    }
}
