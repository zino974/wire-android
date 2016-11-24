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
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.waz.api.BitmapCallback;
import com.waz.api.ImageAsset;
import com.waz.api.LoadHandle;
import com.waz.api.Message;
import com.waz.api.UpdateListener;
import com.waz.zclient.R;
import com.waz.zclient.controllers.accentcolor.AccentColorObserver;
import com.waz.zclient.controllers.drawing.IDrawingController;
import com.waz.zclient.controllers.singleimage.ISingleImageController;
import com.waz.zclient.controllers.tracking.events.conversation.ReactedToMessageEvent;
import com.waz.zclient.controllers.userpreferences.IUserPreferencesController;
import com.waz.zclient.core.api.scala.ModelObserver;
import com.waz.zclient.pages.main.conversation.views.MessageViewsContainer;
import com.waz.zclient.pages.main.conversation.views.row.message.MessageViewController;
import com.waz.zclient.pages.main.conversation.views.row.separator.Separator;
import com.waz.zclient.ui.theme.ThemeUtils;
import com.waz.zclient.ui.utils.ColorUtils;
import com.waz.zclient.ui.utils.ResourceUtils;
import com.waz.zclient.ui.views.EphemeralDotAnimationView;
import com.waz.zclient.ui.views.OnDoubleClickListener;
import com.waz.zclient.utils.LayoutSpec;
import com.waz.zclient.utils.ViewUtils;
import timber.log.Timber;

public class ImageMessageViewController extends MessageViewController implements AccentColorObserver,
                                                                                 View.OnClickListener {

    private static final String FULL_IMAGE_LOADED = "FULL_IMAGE_LOADED";

    private View view;
    private FrameLayout imageContainer;
    private ImageView gifImageView;
    private ProgressDotsView progressDotsView;
    private ImageAsset imageAsset;
    private TextView textViewChangeSetting;
    private View imageActionContainer;
    private TextView sketchDrawButton;
    private View sketchEmojiButton;
    private View sketchTextButton;
    private TextView fullScreenButton;
    private View wifiContainer;
    private EphemeralDotAnimationView ephemeralDotAnimationView;
    private View ephemeralTypeView;
    private UpdateListener imageAssetUpdateListener;
    private LoadHandle bitmapLoadHandle;
    private boolean tapButtonsVisible;
    private int paddingLeft;
    private int paddingRight;
    private int minImageContainerWidth;

    private final OnDoubleClickListener containerOnDoubleClickListener = new OnDoubleClickListener() {
        @Override
        public void onDoubleClick() {
            if (message == null || message.isEphemeral()) {
                return;
            } else if (message.isLikedByThisUser()) {
                message.unlike();
                messageViewsContainer.getControllerFactory().getTrackingController().tagEvent(ReactedToMessageEvent.unlike(message.getConversation(),
                                                                                                                           message,
                                                                                                                           ReactedToMessageEvent.Method.DOUBLE_TAP));
            } else {
                message.like();
                messageViewsContainer.getControllerFactory().getUserPreferencesController().setPerformedAction(IUserPreferencesController.LIKED_MESSAGE);
                messageViewsContainer.getControllerFactory().getTrackingController().tagEvent(ReactedToMessageEvent.like(message.getConversation(),
                                                                                                                         message,
                                                                                                                         ReactedToMessageEvent.Method.DOUBLE_TAP));
            }
        }

        @Override
        public void onSingleClick() {
            if (message == null) {
                return;
            }
            boolean shouldOpenTapButtons = false;
            if (footerActionCallback != null) {
                boolean footerVisible = footerActionCallback.toggleVisibility();
                shouldOpenTapButtons = !tapButtonsVisible && footerVisible;
            }
            messageViewsContainer.closeMessageViewsExtras();
            tapButtonsVisible = shouldOpenTapButtons;
            int visibility = tapButtonsVisible ? View.VISIBLE : View.GONE;
            if (!(message.isEphemeral() && message.isExpired())) {
               imageActionContainer.setVisibility(visibility);
            }
        }
    };

    private ModelObserver<Message> messageModelObserver = new ModelObserver<Message>() {
        @Override
        public void updated(Message model) {
            checkMessageExpired();
        }
    };

    @SuppressLint("InflateParams")
    public ImageMessageViewController(Context context, MessageViewsContainer messageViewContainer) {
        super(context, messageViewContainer);
        view = View.inflate(context, R.layout.row_conversation_image, null);
        imageContainer = ViewUtils.getView(view, R.id.fl__row_conversation__message_image_container);
        imageContainer.setOnClickListener(containerOnDoubleClickListener);
        imageContainer.setOnLongClickListener(this);
        gifImageView = ViewUtils.getView(view, R.id.iv__row_conversation__message_image);
        progressDotsView = ViewUtils.getView(view, R.id.pdv__row_conversation__image_placeholder_dots);
        textViewChangeSetting = ViewUtils.getView(view, R.id.ttv__conversation_row__image__change_settings);
        wifiContainer = ViewUtils.getView(view, R.id.ll__conversation_row__image__wifi_warning);
        wifiContainer.setVisibility(View.GONE);
        imageActionContainer = ViewUtils.getView(view, R.id.fl__row_conversation__image_actions);
        fullScreenButton = ViewUtils.getView(view, R.id.gtv__row_conversation__image_fullscreen);
        sketchDrawButton = ViewUtils.getView(view, R.id.gtv__row_conversation__drawing_button__sketch);
        sketchEmojiButton = ViewUtils.getView(view, R.id.gtv__row_conversation__drawing_button__emoji);
        sketchTextButton = ViewUtils.getView(view, R.id.gtv__row_conversation__drawing_button__text);

        imageActionContainer.setVisibility(View.GONE);
        fullScreenButton.setOnClickListener(this);
        sketchDrawButton.setOnClickListener(this);
        sketchEmojiButton.setOnClickListener(this);
        sketchTextButton.setOnClickListener(this);

        tapButtonsVisible = false;
        ephemeralDotAnimationView = ViewUtils.getView(view, R.id.edav__ephemeral_view);
        ephemeralTypeView = ViewUtils.getView(view, R.id.gtv__row_conversation__image__ephemeral_type);
        ephemeralTypeView.setVisibility(View.GONE);

        paddingLeft = (int) context.getResources().getDimension(R.dimen.content__padding_left);
        paddingRight = (int) context.getResources().getDimension(R.dimen.content__padding_right);
        minImageContainerWidth = (int) context.getResources().getDimension(R.dimen.content__min_container_width);

        afterInit();
    }

    @Override
    public void onSetMessage(Separator separator) {
        containerOnDoubleClickListener.reset();
        wifiContainer.setVisibility(View.GONE);
        gifImageView.setTag(message.getId());
        imageAsset = message.getImage();
        ephemeralDotAnimationView.setMessage(message);

        int displayWidth;
        if (ViewUtils.isInPortrait(context)) {
            displayWidth = ViewUtils.getOrientationIndependentDisplayWidth(context);
        } else {
            displayWidth = ViewUtils.getOrientationIndependentDisplayHeight(context) - context.getResources().getDimensionPixelSize(R.dimen.framework__sidebar_width);
        }
        int originalWidth = ViewUtils.toPx(context, message.getImageWidth());
        int originalHeight = ViewUtils.toPx(context, message.getImageHeight());

        // no left/right padding for full width images
        boolean imageViewSidePadding = originalWidth < displayWidth;

        final int finalWidth = computeFinalWidth(originalWidth, displayWidth, imageViewSidePadding);
        final int finalHeight = getScaledHeight(originalWidth, originalHeight, finalWidth);

        if (!imageViewSidePadding) {
            ViewUtils.setPaddingLeftRight(imageContainer, 0);
        } else {
            if (LayoutSpec.isTablet(imageContainer.getContext()) &&
                ViewUtils.isInPortrait(imageContainer.getContext())) {
                ViewUtils.setPaddingLeft(imageContainer, 0);
                ViewUtils.setPaddingRight(imageContainer, 0);
                ViewUtils.setWidth(imageContainer, imageContainer.getContext().getResources().getDimensionPixelSize(R.dimen.content__width));
            } else {
                ViewUtils.setPaddingLeft(imageContainer, paddingLeft);
                ViewUtils.setPaddingRight(imageContainer, getAdjustedRightPadding(displayWidth, finalWidth));
                imageContainer.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            }
        }

        ViewGroup.LayoutParams layoutParams = gifImageView.getLayoutParams();
        layoutParams.width = finalWidth;
        layoutParams.height = finalHeight;
        gifImageView.setLayoutParams(layoutParams);
        progressDotsView.setLayoutParams(layoutParams);

        if (imageAsset == null) {
            Timber.e("No imageAsset for message with id='%s' available.", message.getId());
            return;
        }

        imageAssetUpdateListener = new UpdateListener() {
            @Override
            public void updated() {
                loadBitmap(finalWidth);
            }
        };
        imageAsset.addUpdateListener(imageAssetUpdateListener);
        loadBitmap(finalWidth);

        messageViewsContainer.getControllerFactory().getAccentColorController().addAccentColorObserver(this);
        messageModelObserver.setAndUpdate(message);
    }

    private void loadBitmap(int finalViewWidth) {
        if (bitmapLoadHandle != null) {
            bitmapLoadHandle.cancel();
        }
        if (message.isEphemeral() && message.isExpired()) {
            return;
        }

        showProgressDots();
        bitmapLoadHandle = imageAsset.getBitmap(finalViewWidth, new BitmapCallback() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap) {
                if (gifImageView == null ||
                    message == null ||
                    !gifImageView.getTag().equals(message.getId()) ||
                    messageViewsContainer.isTornDown()) {
                    return;
                }

                view.setTag(FULL_IMAGE_LOADED);
                if (gifImageView.getDrawable() != null) {
                    gifImageView.setImageBitmap(bitmap);
                } else {
                    showFinalImage(bitmap);
                }
            }

            @Override public void onBitmapLoadingFailed(BitmapLoadingFailed reason) {
                if (reason == BitmapLoadingFailed.DOWNLOAD_ON_WIFI_ONLY) {
                    setWifiContainerVisible(true);
                }
            }
        });
    }

    private void showProgressDots() {
        progressDotsView.setAlpha(1f);
        progressDotsView.setVisibility(View.VISIBLE);
        gifImageView.setVisibility(View.GONE);
    }

    private void setWifiContainerVisible(boolean show) {
        boolean hasWifi = messageViewsContainer.getStoreFactory().getNetworkStore().hasWifiConnection();
        boolean downloadPolicyWifiOnly = messageViewsContainer.getControllerFactory().getUserPreferencesController().isImageDownloadPolicyWifiOnly();

        if (!hasWifi && downloadPolicyWifiOnly && show) {
            wifiContainer.setVisibility(View.VISIBLE);
            textViewChangeSetting.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!messageViewsContainer.isTornDown()) {
                        messageViewsContainer.openSettings();
                    }
                }
            });
        } else {
            wifiContainer.setVisibility(View.GONE);
        }
    }

    private void showFinalImage(final Bitmap bitmap) {
        if (message.isEphemeral() && message.isExpired()) {
            return;
        }
        gifImageView.setImageBitmap(bitmap);
        gifImageView.setAlpha(0f);
        gifImageView.setVisibility(View.VISIBLE);

        int fadingDuration = context.getResources().getInteger(R.integer.content__image__directly_final_duration);
        int startDelay = 0;

        setWifiContainerVisible(false);

        gifImageView.animate()
                    .alpha(1f)
                    .setDuration(fadingDuration)
                    .setStartDelay(startDelay)
                    .start();

        progressDotsView.animate()
                        .alpha(0f)
                        .setDuration(fadingDuration)
                        .setStartDelay(startDelay)
                        .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            progressDotsView.setVisibility(View.GONE);
                        }
                    })
                        .start();

    }

    private int computeFinalWidth(int originalWidth, int displayWidth, boolean imageViewSidePadding) {
        if (imageViewSidePadding) {
            return Math.min(originalWidth, displayWidth - paddingLeft - paddingRight);
        } else {
            return displayWidth;
        }
    }

    private int getScaledHeight(int originalWidth, int originalHeight, double finalWidth) {
        double scaleFactor = finalWidth / originalWidth;
        return (int) (originalHeight * scaleFactor);
    }

    private int getAdjustedRightPadding(int displayWidth, int finalWidth) {
        int containerWidth = Math.max(finalWidth, minImageContainerWidth);
        return displayWidth - containerWidth - paddingLeft;
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void recycle() {
        if (!messageViewsContainer.isTornDown()) {
            messageViewsContainer.getControllerFactory().getAccentColorController().removeAccentColorObserver(this);
        }
        messageModelObserver.clear();
        containerOnDoubleClickListener.reset();
        ephemeralDotAnimationView.setMessage(null);
        gifImageView.animate().cancel();
        imageActionContainer.setVisibility(View.GONE);
        gifImageView.setVisibility(View.INVISIBLE);
        gifImageView.setImageDrawable(null);
        tapButtonsVisible = false;
        textViewChangeSetting.setOnClickListener(null);
        wifiContainer.setVisibility(View.GONE);
        view.setTag(null);
        progressDotsView.setExpired(false);
        if (imageAsset != null) {
            imageAsset.removeUpdateListener(imageAssetUpdateListener);
        }

        if (bitmapLoadHandle != null) {
            bitmapLoadHandle.cancel();
            bitmapLoadHandle = null;
        }
        super.recycle();
    }

    @Override
    public void onAccentColorHasChanged(Object sender, int color) {
        if (message != null &&
            message.isEphemeral() &&
            message.isExpired()) {
            imageContainer.setBackgroundColor(ColorUtils.injectAlpha(ThemeUtils.getEphemeralBackgroundAlpha(context),
                                                                     messageViewsContainer.getControllerFactory().getAccentColorController().getColor()));
        }
        ephemeralDotAnimationView.setPrimaryColor(color);
        ephemeralDotAnimationView.setSecondaryColor(ColorUtils.injectAlpha(ResourceUtils.getResourceFloat(context.getResources(), R.dimen.ephemeral__accent__timer_alpha),
                                                                           color));
        progressDotsView.setAccentColor(ColorUtils.injectAlpha(ThemeUtils.getEphemeralBackgroundAlpha(context), color));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.gtv__row_conversation__image_fullscreen:
                boolean fullImageLoaded = this.view != null && ImageMessageViewController.FULL_IMAGE_LOADED.equals(this.view.getTag());
                if (!fullImageLoaded) {
                    return;
                }
                final ISingleImageController singleImageController = messageViewsContainer.getControllerFactory().getSingleImageController();
                singleImageController.setViewReferences(gifImageView);
                singleImageController.showSingleImage(message);
                break;
            case R.id.gtv__row_conversation__drawing_button__sketch:
                messageViewsContainer.getControllerFactory().getDrawingController().showDrawing(message.getImage(),
                                                                                                IDrawingController.DrawingDestination.SINGLE_IMAGE_VIEW,
                                                                                                IDrawingController.DrawingMethod.DRAW);
                break;
            case R.id.gtv__row_conversation__drawing_button__emoji:
                messageViewsContainer.getControllerFactory().getDrawingController().showDrawing(message.getImage(),
                                                                                                IDrawingController.DrawingDestination.SINGLE_IMAGE_VIEW,
                                                                                                IDrawingController.DrawingMethod.EMOJI);
                break;
            case R.id.gtv__row_conversation__drawing_button__text:
                messageViewsContainer.getControllerFactory().getDrawingController().showDrawing(message.getImage(),
                                                                                                IDrawingController.DrawingDestination.SINGLE_IMAGE_VIEW,
                                                                                                IDrawingController.DrawingMethod.TEXT);
                break;

        }
    }

    private void checkMessageExpired() {
        if (message.isEphemeral() && message.isExpired()) {
            imageContainer.setBackgroundColor(ColorUtils.injectAlpha(ThemeUtils.getEphemeralBackgroundAlpha(context),
                                                                     messageViewsContainer.getControllerFactory().getAccentColorController().getColor()));
            gifImageView.setVisibility(View.INVISIBLE);
            gifImageView.setImageBitmap(null);
            progressDotsView.setVisibility(View.INVISIBLE);
            if (bitmapLoadHandle != null) {
                bitmapLoadHandle.cancel();
            }
            imageActionContainer.setVisibility(View.GONE);
            tapButtonsVisible = false;
            ephemeralTypeView.setVisibility(View.VISIBLE);
            progressDotsView.setExpired(true);
        } else {
            imageContainer.setBackground(null);
            gifImageView.setVisibility(View.VISIBLE);
            progressDotsView.setVisibility(View.VISIBLE);
            ephemeralTypeView.setVisibility(View.GONE);
            progressDotsView.setExpired(false);
        }
    }

    public void closeExtras() {
        imageActionContainer.setVisibility(View.GONE);
        tapButtonsVisible = false;
    }
}
