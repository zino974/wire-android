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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import com.waz.api.ImageAsset;
import com.waz.api.LoadHandle;
import com.waz.zclient.OnBackPressedListener;
import com.waz.zclient.R;
import com.waz.zclient.controllers.drawing.IDrawingController;
import com.waz.zclient.controllers.permission.RequestPermissionsObserver;
import com.waz.zclient.pages.BaseFragment;
import com.waz.zclient.pages.main.conversation.views.image.ImageDragViewContainer;
import com.waz.zclient.ui.animation.interpolators.penner.Expo;
import com.waz.zclient.ui.animation.interpolators.penner.Quart;
import com.waz.zclient.ui.text.GlyphTextView;
import com.waz.zclient.ui.text.TypefaceTextView;
import com.waz.zclient.ui.utils.KeyboardUtils;
import com.waz.zclient.ui.utils.MathUtils;
import com.waz.zclient.utils.LayoutSpec;
import com.waz.zclient.utils.PermissionUtils;
import com.waz.zclient.utils.ViewUtils;
import com.waz.zclient.views.images.TouchImageView;
import timber.log.Timber;

public abstract class SingleImageFragment extends BaseFragment<SingleImageFragment> implements ImageAsset.SaveCallback,
                                                                                               ImageDragViewContainer.Callback,
                                                                                               OnBackPressedListener,
                                                                                               RequestPermissionsObserver {

    private static final Interpolator ALPHA_INTERPOLATOR = new Quart.EaseOut();
    public static final float MIN_BACKGROUND_ALPHA = 0.64f;
    public static final float MIN_DRAG_DISTANCE_FADE_CONTROL = 0.1f;

    private static final String[] SAVE_IMAGE_PERMISSIONS = new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int SAVE_IMAGE_PERMISSION_REQUEST_ID = 6;

    private TypefaceTextView nameTextView;
    private TypefaceTextView timeTextView;
    private TouchImageView animatingImageView;
    private TouchImageView messageTouchImageView;
    private LoadHandle bitmapLoadHandle;
    private LoadHandle animatingSaveBitmapLoadHandle;
    private int clickedImageWidth;
    private int clickedImageHeight;
    private Point clickedImageLocation;
    private View background;
    private int openAnimationDuration;
    private int openAnimationBackgroundDuration;
    private int zoomOutAndRotateBackOnCloseDuration;
    private int closeAnimationBackgroundDelay;
    private int activityOrientation;
    private ImageView saveImageView;
    private ImageView animatingSaveImageView;
    private View saveImageViewContainer;
    private GlyphTextView saveButton;
    private GlyphTextView sketchButton;
    private View headerControls;
    private View footerControl;
    private ImageDragViewContainer dragViewContainer;
    private OnClickListener imageViewOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean fadeIn = !MathUtils.floatEqual(headerControls.getAlpha(), 1f);
            fadeControls(fadeIn);
        }
    };
    private float flingImageTop;
    private float flingImageLeft;
    private float flingRotation;
    private float flingImagePivotX;
    private float flingImagePivotY;
    private boolean controlsVisibleOnStartDrag;
    private boolean isFading;
    private boolean isClosing;
    private final OnClickListener actionButtonsOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.gtv__single_image_message__close:
                    backToConversation(false);
                    break;
                case R.id.gtv__single_image_message__sketch:
                    backToConversation(false);
                    getControllerFactory().getDrawingController().showDrawing(getImage(),
                                                                              IDrawingController.DrawingDestination.SINGLE_IMAGE_VIEW);
                    break;
                case R.id.gtv__single_image_message__love:
                    break;
                case R.id.gtv__single_image_message__save:
                    if (PermissionUtils.hasSelfPermissions(getActivity(), SAVE_IMAGE_PERMISSIONS)) {
                        saveImageToDevice();
                    } else {
                        ActivityCompat.requestPermissions(getActivity(), SAVE_IMAGE_PERMISSIONS, SAVE_IMAGE_PERMISSION_REQUEST_ID);
                    }
                    break;
                default:
                    //nothing
                    break;
            }

        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getControllerFactory().getSingleImageController().updateViewReferences();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    protected void onPostAttach(Activity activity) {
        super.onPostAttach(activity);
        activityOrientation = activity.getRequestedOrientation();
        activity.setRequestedOrientation(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ? ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                                                                        : ActivityInfo.SCREEN_ORIENTATION_USER);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_single_image, container, false);

        dragViewContainer = ViewUtils.getView(view, R.id.dvc__single_image_message__drag_container);
        dragViewContainer.setCallback(this);

        messageTouchImageView = ViewUtils.getView(view, R.id.tiv__single_image_message__image);
        messageTouchImageView.setOnClickListener(imageViewOnClickListener);
        messageTouchImageView.setOnZoomLevelChangedListener(new TouchImageView.OnZoomLevelListener() {
            @Override
            public void onZoomLevelChanged(float scale) {
                if (messageTouchImageView == null ||
                    dragViewContainer == null) {
                    return;
                }
                boolean draggingEnabled = !messageTouchImageView.isZoomed();
                dragViewContainer.setDraggingEnabled(draggingEnabled);
            }
        });

        animatingImageView = ViewUtils.getView(view, R.id.tiv__single_image_message__animating_image);
        background = ViewUtils.getView(view, R.id.v__single_image_message__background);
        openAnimationDuration = getResources().getInteger(R.integer.single_image_message__open_animation__duration);
        openAnimationBackgroundDuration = getResources().getInteger(R.integer.framework_animation_duration_short);
        zoomOutAndRotateBackOnCloseDuration = getResources().getInteger(R.integer.single_image_message__zoom_out_and_rotate_back_on_close_animation__duration);
        closeAnimationBackgroundDelay = getResources().getInteger(R.integer.single_image_message__close_animation_background__delay);

        saveImageViewContainer = ViewUtils.getView(view, R.id.fl__single_image_message__save_image_container);
        saveImageView = ViewUtils.getView(view, R.id.iv__single_image_message__save_image);
        animatingSaveImageView = ViewUtils.getView(view, R.id.iv__single_image_message__save_image_blurred);
        nameTextView = ViewUtils.getView(view, R.id.ttv__single_image_message__name);
        timeTextView = ViewUtils.getView(view, R.id.ttv__single_image_message__time);

        headerControls = ViewUtils.getView(view, R.id.ll__single_image_message__header);
        footerControl = ViewUtils.getView(view, R.id.ll__single_image_message__footer);

        GlyphTextView closeGlyphText = ViewUtils.getView(view, R.id.gtv__single_image_message__close);
        closeGlyphText.setOnClickListener(actionButtonsOnClickListener);

        GlyphTextView loveGlyphText = ViewUtils.getView(view, R.id.gtv__single_image_message__love);
        loveGlyphText.setOnClickListener(actionButtonsOnClickListener);

        saveButton = ViewUtils.getView(view, R.id.gtv__single_image_message__save);
        saveButton.setOnClickListener(actionButtonsOnClickListener);

        sketchButton = ViewUtils.getView(view, R.id.gtv__single_image_message__sketch);
        sketchButton.setOnClickListener(actionButtonsOnClickListener);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        getControllerFactory().getRequestPermissionsController().addObserver(this);
        KeyboardUtils.closeKeyboardIfShown(getActivity());
        displayImage(getImage());
        animatingImageView.setScaleType(getScaleType());
        nameTextView.setTransformedText(getNameText());
        timeTextView.setTransformedText(getTimeText());
    }

    protected abstract ImageAsset getImage();

    protected abstract String getNameText();

    protected abstract String getTimeText();

    protected abstract ImageView.ScaleType getScaleType();

    @Override
    public void onStop() {
        getControllerFactory().getRequestPermissionsController().removeObserver(this);
        restoreRotation();
        if (bitmapLoadHandle != null) {
            bitmapLoadHandle.cancel();
            bitmapLoadHandle = null;
        }

        if (animatingSaveBitmapLoadHandle != null) {
            animatingSaveBitmapLoadHandle.cancel();
            animatingSaveBitmapLoadHandle = null;
        }

        super.onStop();
    }

    @SuppressWarnings("WrongConstant")
    private void restoreRotation() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.setRequestedOrientation(activityOrientation);
        }
    }

    @Override
    public void onDestroyView() {
        messageTouchImageView.setImageDrawable(null);
        messageTouchImageView = null;
        animatingImageView.setImageDrawable(null);
        animatingImageView = null;
        dragViewContainer.setCallback(null);
        dragViewContainer = null;
        super.onDestroyView();
    }

    private void displayImage(ImageAsset imageAsset) {
        if (bitmapLoadHandle != null) {
            bitmapLoadHandle.cancel();
            bitmapLoadHandle = null;
        }
        if (imageAsset == null) {
            getFragmentManager().popBackStack();
            return;
        }
        if (imageAsset.getWidth() > 0) {
            fadeControls(true);
            bitmapLoadHandle = imageAsset.getBitmap(ViewUtils.getOrientationIndependentDisplayWidth(getActivity()),
                                                    new ImageAsset.BitmapCallback() {
                                                        @Override
                                                        public void onBitmapLoaded(Bitmap bitmap, boolean isPreview) {
                                                            if (getActivity() == null || messageTouchImageView == null) {
                                                                return;
                                                            }
                                                            if (!isPreview &&
                                                                messageTouchImageView.getDrawable() != null) {
                                                                // means we display the preview
                                                                showBitmap(bitmap);
                                                                return;
                                                            }
                                                            loadClickedImageSizeAndPosition();
                                                            positionViewAtAnimationStart();
                                                            showBitmap(bitmap);
                                                            animateOpeningTransition();
                                                        }

                                                        @Override
                                                        public void onBitmapLoadingFailed() {
                                                            // show error?
                                                            if (messageTouchImageView == null ||
                                                                // means we display nothing
                                                                messageTouchImageView.getDrawable() == null) {
                                                                getFragmentManager().popBackStack();
                                                            }
                                                        }
                                                    });
        } else {
            getFragmentManager().popBackStack();
        }
    }

    private void showBitmap(Bitmap bitmap) {
        messageTouchImageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
        animatingImageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
    }

    private void loadClickedImageSizeAndPosition() {
        final View clickedImage = getControllerFactory().getSingleImageController().getImageContainer();
        clickedImageHeight = clickedImage.getMeasuredHeight();
        clickedImageWidth = clickedImage.getMeasuredWidth();
        if (clickedImageHeight == 0 || clickedImageWidth == 0) {
            View parent = (View) clickedImage.getParent();
            final int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getMeasuredWidth()
                                                                   - parent.getPaddingLeft()
                                                                   - parent.getPaddingRight(),
                                                                   View.MeasureSpec.EXACTLY);
            final int heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getMeasuredHeight()
                                                                    - parent.getPaddingTop()
                                                                    - parent.getPaddingBottom(),
                                                                    View.MeasureSpec.EXACTLY);
            clickedImage.measure(widthSpec, heightSpec);
            clickedImageHeight = clickedImage.getMeasuredHeight();
            clickedImageWidth = clickedImage.getMeasuredWidth();
        }
        clickedImageLocation = ViewUtils.getLocationOnScreen(clickedImage);
        int dx = 0;
        int dy = -ViewUtils.getStatusBarHeight(getActivity());
        clickedImageLocation.offset(dx, dy);
    }

    private void positionViewAtAnimationStart() {
        ViewGroup.LayoutParams layoutParams = animatingImageView.getLayoutParams();
        layoutParams.width = clickedImageWidth;
        layoutParams.height = clickedImageHeight;
        animatingImageView.setLayoutParams(layoutParams);
        animatingImageView.setX(clickedImageLocation.x);
        animatingImageView.setY(clickedImageLocation.y);
    }

    private void animateOpeningTransition() {
        final int displayHeight = ViewUtils.getOrientationDependentDisplayHeight(getActivity()) - ViewUtils.getStatusBarHeight(
            getActivity());
        final int displayWidth = ViewUtils.getOrientationDependentDisplayWidth(getActivity());

        int fullImageHeight = displayHeight;
        int fullImageWidth = (int) (fullImageHeight * (float) clickedImageWidth / (float) clickedImageHeight);
        if (fullImageWidth > displayWidth) {
            fullImageWidth = displayWidth;
            fullImageHeight = (int) (fullImageWidth * (float) clickedImageHeight / (float) clickedImageWidth);
        }
        final float scale = Math.min(fullImageWidth / (float) clickedImageWidth,
                                     fullImageHeight / (float) clickedImageHeight);
        final int finalTop = (displayHeight - clickedImageHeight) / 2;
        final int finalLeft = (displayWidth - clickedImageWidth) / 2;

        animatingImageView.animate()
                          .y(finalTop)
                          .x(finalLeft)
                          .scaleX(scale)
                          .scaleY(scale)
                          .setInterpolator(new Expo.EaseOut())
                          .setDuration(openAnimationDuration)
                          .withStartAction(new Runnable() {
                              @Override
                              public void run() {
                                  getControllerFactory().getSingleImageController().getImageContainer().setVisibility(
                                      View.INVISIBLE);
                              }
                          })
                          .withEndAction(new Runnable() {
                              @Override
                              public void run() {
                                  if (messageTouchImageView == null || animatingImageView == null) {
                                      return;
                                  }
                                  messageTouchImageView.setVisibility(View.VISIBLE);
                                  animatingImageView.setVisibility(View.GONE);
                              }
                          })
                          .start();

        background.animate()
                  .alpha(1f)
                  .setDuration(openAnimationBackgroundDuration)
                  .setInterpolator(new Quart.EaseOut())
                  .start();
    }

    private void backToConversation(boolean afterFling) {
        if (isClosing) {
            return;
        }
        isClosing = true;
        loadClickedImageSizeAndPosition();
        initAnimatingImageView(afterFling);

        restoreRotation();
        getControllerFactory().getSingleImageController().hideSingleImage();
        fadeControls(false);

        PointF currentFocusPoint = messageTouchImageView.getScrollPosition();
        if (currentFocusPoint == null) {
            getControllerFactory().getSingleImageController().clearReferences();
            getFragmentManager().popBackStack();
            return;
        }
        TouchImageView.FocusAndScale startFocusAndScale = new TouchImageView.FocusAndScale(currentFocusPoint.x,
                                                                                           currentFocusPoint.y,
                                                                                           messageTouchImageView.getCurrentZoom());
        TouchImageView.FocusAndScale finishFocusAndScale = new TouchImageView.FocusAndScale(0.5f, 0.5f, 1f);
        if ((MathUtils.floatEqual(currentFocusPoint.x, 0.5f) || MathUtils.floatEqual(currentFocusPoint.y, 0.5f)) &&
            MathUtils.floatEqual(messageTouchImageView.getCurrentZoom(), 1f)) {
            zoomOutAndRotateBackOnCloseDuration = 1;
        }
        ObjectAnimator.ofObject(messageTouchImageView,
                                "focusAndScale",
                                new TouchImageView.FocusAndScaleEvaluator(),
                                startFocusAndScale,
                                finishFocusAndScale)
                      .setDuration(zoomOutAndRotateBackOnCloseDuration)
                      .start();

        final boolean imageOffScreenInList = getControllerFactory().getSingleImageController().isContainerOutOfScreen();
        ViewPropertyAnimator exitAnimation = animatingImageView.animate();
        if (imageOffScreenInList) {
            exitAnimation.alpha(0);
        } else {
            exitAnimation.x(clickedImageLocation.x)
                         .y(clickedImageLocation.y)
                         .rotation(0f)
                         .scaleX(1f)
                         .scaleY(1f);
        }
        exitAnimation.setDuration(openAnimationDuration)
                     .setStartDelay(zoomOutAndRotateBackOnCloseDuration)
                     .setInterpolator(new Expo.EaseOut())
                     .withStartAction(new Runnable() {
                         @Override
                         public void run() {
                             animatingImageView.setVisibility(View.VISIBLE);
                             messageTouchImageView.setVisibility(View.GONE);
                             if (imageOffScreenInList) {
                                 getControllerFactory().getSingleImageController().getImageContainer().setVisibility(
                                     View.VISIBLE);
                             } else {
                                 getControllerFactory().getSingleImageController().getImageContainer().setVisibility(
                                     View.INVISIBLE);
                             }
                         }
                     })
                     .withEndAction(new Runnable() {
                         @Override
                         public void run() {
                             getControllerFactory().getSingleImageController().getImageContainer().setVisibility(View.VISIBLE);
                             getControllerFactory().getSingleImageController().clearReferences();
                             getFragmentManager().popBackStack();
                         }
                     });


        exitAnimation.start();

        background.animate()
                  .alpha(0f)
                  .setStartDelay(zoomOutAndRotateBackOnCloseDuration + closeAnimationBackgroundDelay)
                  .setDuration(openAnimationBackgroundDuration)
                  .setInterpolator(new Quart.EaseOut())
                  .start();
    }

    private void initAnimatingImageView(boolean afterFling) {
        if (getView() == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) animatingImageView.getParent();
        parent.removeView(animatingImageView);

        final int displayHeight = ViewUtils.getOrientationDependentDisplayHeight(getActivity()) - ViewUtils.getStatusBarHeight(
            getActivity());
        final int displayWidth = ViewUtils.getOrientationDependentDisplayWidth(getActivity());
        final int finalTop;
        final int finalLeft;
        if (afterFling) {
            finalLeft = (int) flingImageLeft;
            finalTop = (int) flingImageTop;
            animatingImageView.setPivotX(flingImagePivotX);
            animatingImageView.setPivotY(flingImagePivotY);
            animatingImageView.setRotation(flingRotation);
        } else {
            finalTop = (displayHeight - clickedImageHeight) / 2;
            finalLeft = (displayWidth - clickedImageWidth) / 2;
        }
        final float scale = Math.min(displayWidth / (float) clickedImageWidth,
                                     displayHeight / (float) clickedImageHeight);

        ViewGroup.LayoutParams layoutParams = new FrameLayout.LayoutParams(clickedImageWidth, clickedImageHeight);
        animatingImageView.setLayoutParams(layoutParams);
        animatingImageView.setX(finalLeft);
        animatingImageView.setY(finalTop);
        animatingImageView.setScaleX(scale);
        animatingImageView.setScaleY(scale);

        parent.addView(animatingImageView);
    }

    private void saveImageToDevice() {
        enableSaveImageButton(false);

        if (animatingSaveBitmapLoadHandle != null) {
            animatingSaveBitmapLoadHandle.cancel();
        }

        final ImageAsset image = getImage();
        final ImageAsset.BitmapCallback bitmapCallback = new ImageAsset.BitmapCallback() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, boolean b) {
                Timber.i("Got bitmap for save animation.");

                if (animatingSaveBitmapLoadHandle != null) {
                    animatingSaveBitmapLoadHandle.cancel();
                    animatingSaveBitmapLoadHandle = null;
                }

                animatingSaveImageView.setImageBitmap(bitmap);
                saveImageView.setImageDrawable(messageTouchImageView.getDrawable());

                saveImageViewContainer.setVisibility(View.VISIBLE);
                AnimatorSet animation = getSaveImageAnimation(image);
                animation.start();
            }

            @Override
            public void onBitmapLoadingFailed() {
                Timber.i("Failed loading bitmap for save animation.");
                if (animatingSaveBitmapLoadHandle != null) {
                    animatingSaveBitmapLoadHandle.cancel();
                    animatingSaveBitmapLoadHandle = null;
                }

                image.saveImageToGallery(SingleImageFragment.this);
            }
        };

        animatingSaveBitmapLoadHandle = image.getSingleBitmap(ViewUtils.getOrientationIndependentDisplayHeight(
            getActivity()) / 2, bitmapCallback);
    }

    private void unableToSaveImageNoPermissions() {
        Toast.makeText(getActivity(), R.string.single_image__save_to_disk__error, Toast.LENGTH_SHORT).show();
    }

    private void enableSaveImageButton(boolean enabled) {
        saveButton.setEnabled(enabled);
        saveButton.setClickable(enabled);
    }

    private AnimatorSet getSaveImageAnimation(final ImageAsset image) {
        int totalAnimationDuration = getResources().getInteger(R.integer.framework_animation_duration_long);

        ObjectAnimator fadeOutAnimator = ObjectAnimator.ofFloat(saveImageViewContainer, View.ALPHA, 1, 0);
        fadeOutAnimator.setDuration(totalAnimationDuration);

        ObjectAnimator scaleDownXAnimator = ObjectAnimator.ofFloat(saveImageViewContainer, View.SCALE_X, 0f);
        ObjectAnimator scaleDownYAnimator = ObjectAnimator.ofFloat(saveImageViewContainer, View.SCALE_Y, 0f);
        scaleDownXAnimator.setDuration(totalAnimationDuration);
        scaleDownYAnimator.setDuration(totalAnimationDuration);

        int moveY = ViewUtils.getOrientationIndependentDisplayHeight(getActivity()) / 2;
        int moveX;
        if (ViewUtils.isInLandscape(getActivity())) {
            moveX = ViewUtils.getOrientationIndependentDisplayWidth(getActivity());
        } else {
            moveX = ViewUtils.getOrientationIndependentDisplayWidth(getActivity()) / 2;
        }
        ObjectAnimator moveXAnimator = ObjectAnimator.ofFloat(saveImageViewContainer, View.TRANSLATION_X, -moveX);
        ObjectAnimator moveYAnimator = ObjectAnimator.ofFloat(saveImageViewContainer, View.TRANSLATION_Y, moveY);
        moveXAnimator.setDuration(totalAnimationDuration);
        moveYAnimator.setDuration(totalAnimationDuration);

        // Fade out top image view for blur effect
        ObjectAnimator fadeToBlurredAnimator = ObjectAnimator.ofFloat(saveImageView, View.ALPHA, 1, 0);
        fadeToBlurredAnimator.setDuration(getResources().getInteger(R.integer.framework_animation_duration_medium));

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setInterpolator(new Expo.EaseIn());
        animatorSet.playTogether(fadeOutAnimator,
                                 scaleDownXAnimator,
                                 scaleDownYAnimator,
                                 moveXAnimator,
                                 moveYAnimator,
                                 fadeToBlurredAnimator);

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (getStoreFactory() == null ||
                    getStoreFactory().isTornDown()) {
                    return;
                }

                resetSaveImageView();

                // Image saving is called here, as an image save triggers a notification which contains a bitmap;
                // currently that bitmap is quite large, android.app.Notification writes the bitmap to a parcel
                // on the main thread, dropping a large number of frames and breaking the save animation
                image.saveImageToGallery(SingleImageFragment.this);

                enableSaveImageButton(true);
            }
        });

        return animatorSet;
    }

    private void resetSaveImageView() {
        saveImageViewContainer.setScaleX(1f);
        saveImageViewContainer.setScaleY(1f);
        saveImageViewContainer.setTranslationX(0f);
        saveImageViewContainer.setTranslationY(0f);
        saveImageView.setAlpha(1f);

        animatingSaveImageView.setImageBitmap(null);
        saveImageView.setImageDrawable(null);
    }

    private void fadeControls(boolean fadeIn) {
        fadeView(headerControls, fadeIn);
        if (getImage() != null) {
            fadeView(footerControl, fadeIn);
        } else {
            footerControl.setVisibility(View.GONE);
        }
    }

    private void fadeView(final View view, final boolean fadeIn) {
        isFading = true;
        float toAlpha = fadeIn ? 1f : 0f;

        view.setClickable(fadeIn);
        view.animate()
            .alpha(toAlpha)
            .setInterpolator(new Quart.EaseOut())
            .setDuration(getResources().getInteger(R.integer.single_image_message__overlay__fade_duration))
            .withStartAction(new Runnable() {
                @Override
                public void run() {
                    if (fadeIn) {
                        view.setVisibility(View.VISIBLE);
                    }
                }
            })
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    if (!fadeIn) {
                        view.setVisibility(View.GONE);
                    }
                    isFading = false;
                }
            })
            .start();
    }

    @Override
    public boolean onBackPressed() {
        backToConversation(false);
        return true;
    }

    @Override
    public void imageSaved(Uri uri) {
        if (getContainer() == null ||
            getActivity() == null) {
            return;
        }

        getControllerFactory()
            .getNotificationsController()
            .showImageSavedNotification(getImage(), uri);
    }

    @Override
    public void imageSavingFailed(Exception ex) {

    }

    @Override
    public void onFlingRequested(float left, float top, float rotation, float pivotX, float pivotY) {
        this.flingImageLeft = left;
        this.flingImageTop = top;
        this.flingImagePivotX = pivotX;
        this.flingImagePivotY = pivotY;
        this.flingRotation = rotation;
        backToConversation(true);
    }

    @Override
    public void onDragDistance(float distance) {
        if (ViewUtils.isInPortrait(getActivity()) || LayoutSpec.isTablet(getActivity())) {
            background.setAlpha(1f - (1f - MIN_BACKGROUND_ALPHA) * ALPHA_INTERPOLATOR.getInterpolation(distance));
        }
        if (distance >= MIN_DRAG_DISTANCE_FADE_CONTROL &&
            footerControl.getVisibility() != View.GONE &&
            !isFading) {
            fadeControls(false);
        }
    }

    @Override
    public void onStartDrag() {
        this.controlsVisibleOnStartDrag = MathUtils.floatEqual(footerControl.getAlpha(), 1f);
    }

    @Override
    public void onEndDrag() {
        if (controlsVisibleOnStartDrag) {
            fadeControls(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, int[] grantResults) {
        switch (requestCode) {
            case SAVE_IMAGE_PERMISSION_REQUEST_ID:
                if (PermissionUtils.verifyPermissions(grantResults)) {
                    saveImageToDevice();
                } else {
                    unableToSaveImageNoPermissions();
                }
                break;
        }
    }

    public interface Container {
    }
}
