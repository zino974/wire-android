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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.waz.api.Message;
import com.waz.zclient.R;
import com.waz.zclient.core.api.scala.ModelObserver;
import com.waz.zclient.pages.main.conversation.views.MessageViewsContainer;
import com.waz.zclient.pages.main.conversation.views.row.message.MessageViewController;
import com.waz.zclient.pages.main.conversation.views.row.separator.Separator;
import com.waz.zclient.ui.utils.ResourceUtils;
import com.waz.zclient.utils.ViewUtils;

public class TextMessageViewController extends MessageViewController {

    private View view;
    private TextView textView;

    private final float textSizeRegular;
    private final float textSizeEmoji;

    private ModelObserver<Message> messageModelObserver = new ModelObserver<Message>() {
        @Override
        public void updated(Message message) {
            resizeIfEmoji(message);
            textView.setText(message.getBody());
        }
    };

    @SuppressLint("InflateParams")
    public TextMessageViewController(Context context, final MessageViewsContainer messageViewContainer) {
        super(context, messageViewContainer);
        LayoutInflater inflater = LayoutInflater.from(context);
        view = inflater.inflate(R.layout.row_conversation_text_message, null);
        textView = ViewUtils.getView(view, R.id.ltv__row_conversation__message);
        View textContainer = ViewUtils.getView(view, R.id.ll__row_conversation__message_container);
        textContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (footerActionCallback != null) {
                    footerActionCallback.toggleVisibility();
                }
            }
        });
        textContainer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                messageViewContainer.onItemLongClick(message);
                return true;
            }
        });

        textSizeRegular = context.getResources().getDimensionPixelSize(R.dimen.wire__text_size__regular);
        textSizeEmoji = context.getResources().getDimensionPixelSize(R.dimen.wire__text_size__emoji);

        afterInit();
    }

    @Override
    public void onSetMessage(Separator separator) {
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
            footerActionCallback.toggleVisibility();
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

    private void resizeIfEmoji(Message message) {
        if (message.getMessageType() == Message.Type.TEXT_EMOJI_ONLY) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeEmoji);
        } else {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeRegular);
        }
    }

}
