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

import android.content.Context;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import com.waz.api.ImageAsset;
import com.waz.api.LoadHandle;
import com.waz.api.Message;
import com.waz.zclient.R;
import com.waz.zclient.controllers.selection.MessageActionModeController;
import com.waz.zclient.core.api.scala.ModelObserver;
import com.waz.zclient.pages.main.conversation.views.MessageViewsContainer;
import com.waz.zclient.pages.main.conversation.views.row.message.MessageViewController;
import com.waz.zclient.pages.main.conversation.views.row.separator.Separator;
import com.waz.zclient.ui.views.TouchFilterableLayout;
import com.waz.zclient.utils.MessageUtils;
import com.waz.zclient.utils.StringUtils;
import com.waz.zclient.utils.ViewUtils;
import com.waz.zclient.views.images.ImageAssetView;

public class LinkPreviewViewController extends MessageViewController implements ImageAssetView.BitmapLoadedCallback,
                                                                                MessageActionModeController.Selectable,
                                                                                View.OnClickListener {

    private TouchFilterableLayout view;
    private TextMessageWithTimestamp textMessageWithTimestamp;
    private View linkPrevieContainerView;
    private TextView titleTextView;
    private TextView urlTextView;
    private ImageAssetView previewImageAssetView;
    private View progressDotsView;
    private View previewImageContainerView;
    private LoadHandle previewImageLoadHandle;

    private final ModelObserver<Message> messageObserver = new ModelObserver<Message>() {
        @Override
        public void updated(Message message) {
            if (messageBodyIsSingleLink()) {
                textMessageWithTimestamp.setVisibility(View.GONE);
            } else {
                textMessageWithTimestamp.setMessage(message);
                textMessageWithTimestamp.setVisibility(View.VISIBLE);
            }

            Message.Part linkPart = MessageUtils.getFirstRichMediaPart(message);
            if (linkPart == null) {
                linkPrevieContainerView.setVisibility(View.GONE);
                return;
            } else {
                linkPrevieContainerView.setVisibility(View.VISIBLE);
            }
            titleTextView.setText(Html.fromHtml(linkPart.getTitle()));
            urlTextView.setText(StringUtils.trimLinkPreviewUrls(linkPart.getContentUri()));
            if (linkPart.getImage() == null ||
                linkPart.getImage().isEmpty()) {
                return;
            }
            previewImageContainerView.setVisibility(View.VISIBLE);
            imageAssetModelObserver.addAndUpdate(linkPart.getImage());
        }
    };

    private final ModelObserver<ImageAsset> imageAssetModelObserver = new ModelObserver<ImageAsset>() {
        @Override
        public void updated(ImageAsset imageAsset) {
            previewImageAssetView.setImageAsset(imageAsset);
        }
    };

    public LinkPreviewViewController(Context context,
                                     final MessageViewsContainer messageViewsContainer) {
        super(context, messageViewsContainer);
        view = (TouchFilterableLayout) View.inflate(context, R.layout.row_conversation_link_preview, null);
        textMessageWithTimestamp = ViewUtils.getView(view.getLayout(),
                                                     R.id.cv__row_conversation__link_preview__text_message);
        linkPrevieContainerView = ViewUtils.getView(view.getLayout(),
                                                    R.id.cv__row_conversation__link_preview__container);
        titleTextView = ViewUtils.getView(view.getLayout(), R.id.ttv__row_conversation__link_preview__title);
        urlTextView = ViewUtils.getView(view.getLayout(), R.id.ttv__row_conversation__link_preview__url);
        previewImageAssetView = ViewUtils.getView(view.getLayout(), R.id.iv__row_conversation__link_preview__image);
        progressDotsView = ViewUtils.getView(view.getLayout(),
                                             R.id.pdv__row_conversation__link_preview__placeholder_dots);
        previewImageContainerView = ViewUtils.getView(view.getLayout(),
                                                      R.id.fl__row_conversation__link_preview__image_container);
        previewImageContainerView.setVisibility(View.GONE);
        previewImageAssetView.setBitmapLoadedCallback(this);

        linkPrevieContainerView.setOnClickListener(this);

        textMessageWithTimestamp.setMessageViewsContainer(messageViewsContainer);
        textMessageWithTimestamp.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (message == null ||
                    messageViewsContainer == null ||
                    messageViewsContainer.getControllerFactory() == null ||
                    messageViewsContainer.getControllerFactory().isTornDown()) {
                    return false;
                }
                messageViewsContainer.getControllerFactory().getMessageActionModeController().selectMessage(message);
                return true;
            }
        });

        linkPrevieContainerView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (message == null ||
                    messageViewsContainer == null ||
                    messageViewsContainer.getControllerFactory() == null ||
                    messageViewsContainer.getControllerFactory().isTornDown()) {
                    return false;
                }
                messageViewsContainer.getControllerFactory().getMessageActionModeController().selectMessage(message);
                return true;
            }
        });
    }

    @Override
    protected void onSetMessage(Separator separator) {
        messageObserver.setAndUpdate(message);
    }

    @Override
    public TouchFilterableLayout getView() {
        return view;
    }

    @Override
    public void recycle() {
        previewImageContainerView.setVisibility(View.GONE);
        progressDotsView.setVisibility(View.VISIBLE);
        messageObserver.clear();
        previewImageAssetView.clearImage();
        imageAssetModelObserver.clear();
        urlTextView.setText("");
        titleTextView.setText("");
        if (previewImageLoadHandle != null) {
            previewImageLoadHandle.cancel();
        }
        previewImageLoadHandle = null;
        textMessageWithTimestamp.recycle();
        super.recycle();
    }

    @Override
    public void onClick(View view) {
        if (TextUtils.isEmpty(urlTextView.getText())) {
            return;
        }
        messageViewsContainer.onOpenUrl(urlTextView.getText().toString());
    }

    private boolean messageBodyIsSingleLink() {
        int numberOfLinks = 0;
        for (int i = 0; i < message.getParts().length; i++) {
            if (message.getParts()[i].getPartType() == Message.Part.Type.WEB_LINK) {
                numberOfLinks++;
            }
        }
        return numberOfLinks == 1 && !message.getBody().trim().contains(" ");
    }

    @Override
    public void onBitmapLoadFinished(boolean bitmapLoaded) {
        previewImageContainerView.setVisibility(bitmapLoaded ? View.VISIBLE : View.GONE);
        if (bitmapLoaded) {
            progressDotsView.setVisibility(View.GONE);
        }
    }
}
