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
package com.waz.zclient.pages.main.conversation.views.row.footer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.TextView;
import com.waz.api.Message;
import com.waz.zclient.R;
import com.waz.zclient.controllers.userpreferences.IUserPreferencesController;
import com.waz.zclient.core.api.scala.ModelObserver;
import com.waz.zclient.pages.main.conversation.views.MessageViewsContainer;
import com.waz.zclient.pages.main.conversation.views.row.footer.views.FooterLikeDetailsLayout;
import com.waz.zclient.pages.main.conversation.views.row.message.ConversationItemViewController;
import com.waz.zclient.ui.utils.TextViewUtils;
import com.waz.zclient.utils.ViewUtils;
import com.waz.zclient.utils.ZTimeFormatter;
import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.Instant;

public class FooterViewController implements ConversationItemViewController, FooterActionCallback {

    private static final int LIKE_HINT_VISIBILITY_MIL_SEC = 3000;
    private static final int TIMESTAMP_VISIBILITY_MIL_SEC = 5000;
    private final Context context;
    private final MessageViewsContainer container;
    private View view;
    private TextView likeButton;
    private TextView messageStatusTextView;
    private FooterLikeDetailsLayout likeDetails;

    private Handler mainHandler;

    private int likeButtonColorLiked;
    private int likeButtonColorUnliked;
    private Message message;

    private boolean animateLikeButton;
    private boolean isMyLastMessage;

    private final ModelObserver<Message> messageModelObserver = new ModelObserver<Message>() {
        @Override
        public void updated(Message message) {
            if (message.isLiked()) {
                showLikeDetails();
            } else {
                showMessageStatus();
            }

            if (shouldBeExpanded() && (view.getVisibility() == View.GONE || view.getMeasuredHeight() == 0)) {
                if (container.getExpandedView() != null && container.getExpandedView() != FooterViewController.this) {
                    container.getExpandedView().close();
                }
                container.setExpandedView(FooterViewController.this);
                container.setExpandedMessageId(message.getId());
                expand();
                if (message.isLastMessageFromSelf()) {
                    likeButton.setVisibility(View.GONE);
                }
            } else if (isMyLastMessage && !message.isLastMessageFromSelf()) {
                collapse();
            }

            isMyLastMessage = message.isLastMessageFromSelf();

            updateLikeButton();
            updateMessageStatusLabel();
            likeDetails.setUsers(message.isLiked() ? message.getLikes() : null);
        }
    };

    private Runnable showLikeDetailsRunnable = new Runnable() {
        @Override
        public void run() {
            showLikeDetails();
        }
    };

    private Runnable showMessageStatusRunnable = new Runnable() {
        @Override
        public void run() {
            showMessageStatus();
        }
    };

    public FooterViewController(Context context, MessageViewsContainer container) {
        this.context = context;
        this.container = container;
        mainHandler = new Handler(Looper.getMainLooper());
        view = View.inflate(context, R.layout.row_conversation_footer, null);
        likeButton = ViewUtils.getView(view, R.id.gtv__footer__like__button);
        messageStatusTextView = ViewUtils.getView(view, R.id.tv__footer__message_status);
        likeDetails = ViewUtils.getView(view, R.id.fldl_like_details);

        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLike(true);
            }
        });

        likeButtonColorLiked = ContextCompat.getColor(context, R.color.accent_red);
        likeButtonColorUnliked = ContextCompat.getColor(context, R.color.text__secondary_light);
    }

    public void setMessage(Message message) {
        this.message = message;
        isMyLastMessage = message.isLastMessageFromSelf();
        if (shouldBeExpanded() || message.getId().equals(container.getExpandedMessageId())) {
            view.setVisibility(View.VISIBLE);
            if (message.isLiked()) {
                likeDetails.setVisibility(View.VISIBLE);
                messageStatusTextView.setVisibility(View.INVISIBLE);
            } else {
                likeDetails.setVisibility(View.INVISIBLE);
                messageStatusTextView.setVisibility(View.VISIBLE);
            }
        } else {
            view.setVisibility(View.GONE);
            likeDetails.setVisibility(View.INVISIBLE);
            messageStatusTextView.setVisibility(View.VISIBLE);
        }
        messageModelObserver.setAndUpdate(message);
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void recycle() {
        mainHandler.removeCallbacksAndMessages(null);
        view.setVisibility(View.GONE);
        likeButton.setVisibility(View.VISIBLE);
        messageModelObserver.clear();
        likeDetails.setUsers(null);
        message = null;
        animateLikeButton = false;
        isMyLastMessage = false;
    }

    private void toggleLike(boolean animate) {
        if (message.isLikedByThisUser()) {
            message.unlike();
            animateLikeButton = false;
        } else {
            animateLikeButton = animate;
            message.like();
            if (!container.getControllerFactory().getUserPreferencesController().hasPerformedAction(IUserPreferencesController.LIKED_MESSAGE)) {
                container.getControllerFactory().getUserPreferencesController().setPerformedAction(IUserPreferencesController.LIKED_MESSAGE);
            }
        }
    }

    private boolean shouldBeExpanded() {
        return message.isLiked() ||
               message.isLastMessageFromSelf() ||
               message.getMessageStatus() == Message.Status.FAILED ||
               message.getMessageStatus() == Message.Status.FAILED_READ;
    }

    private void updateLikeButton() {
        boolean likedByThisUser = message.isLikedByThisUser();
        boolean showLikeAnimation = likedByThisUser &&
                                    animateLikeButton &&
                                    likeButton.getTag() != null &&
                                    !(boolean) likeButton.getTag();
        animateLikeButton = false;
        likeButton.setText(context.getText(likedByThisUser ? R.string.glyph__liked : R.string.glyph__love));
        likeButton.setTextColor(likedByThisUser ? likeButtonColorLiked : likeButtonColorUnliked);
        likeButton.setTag(likedByThisUser);
        if (showLikeAnimation) {
            showLikeAnimation();
        }
    }

    private void updateMessageStatusLabel() {
        Instant messageTime = message.isEdited() || message.isDeleted() ?
                              message.getEditTime() :
                              message.getTime();
        String timestamp = ZTimeFormatter.getSingleMessageTime(getView().getContext(), DateTimeUtils.toDate(messageTime));
        String status;
        if (message.getUser().isMe()) {
            switch (message.getMessageStatus()) {
                case PENDING:
                    status = context.getString(R.string.message_footer__status__sending);
                    break;
                case SENT:
                    status = context.getString(R.string.message_footer__status__sent, timestamp);
                    break;
                case DELETED:
                    status = context.getString(R.string.message_footer__status__deleted, timestamp);
                    break;
                case DELIVERED:
                    status = context.getString(R.string.message_footer__status__delivered, timestamp);
                    break;
                case FAILED:
                case FAILED_READ:
                    status = context.getString(R.string.message_footer__status__failed);
                    break;
                default:
                    status = timestamp;
                    break;
            }
        } else {
            status = timestamp;
        }
        messageStatusTextView.setText(status);
        TextViewUtils.linkifyText(messageStatusTextView,
                                  ContextCompat.getColor(context, R.color.accent_red),
                                  false,
                                  new Runnable() {
                                      @Override
                                      public void run() {
                                          message.retry();
                                      }
                                  });
    }

    private void showLikeAnimation() {
        final Interpolator interpolator = new AccelerateDecelerateInterpolator();
        likeButton.animate()
                  .scaleX(2f)
                  .scaleY(2f)
                  .setDuration(100)
                  .setInterpolator(interpolator).withEndAction(new Runnable() {
            @Override
            public void run() {
                if (likeButton != null) {
                    likeButton.animate()
                              .scaleX(1f)
                              .scaleY(1f)
                              .setDuration(100)
                              .setInterpolator(interpolator);
                }
            }
        });
        container.getControllerFactory().getVibratorController().vibrate(new long[]{10, 20, 10, 20, 10, 30, 10, 20, 10, 20, 10, 30});
    }

    private void expand() {
        if (container.getExpandedView() != null && container.getExpandedView() != this) {
            container.getExpandedView().close();
        }
        container.setExpandedMessageId(message.getId());
        container.setExpandedView(this);
        view.setVisibility(View.VISIBLE);
        likeButton.setVisibility(View.VISIBLE);

        View parent = (View) view.getParent();
        final int widthSpec;
        if (parent == null) {
            // needed for tests
            widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        } else {
             widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getMeasuredWidth()
                                                                   - parent.getPaddingLeft()
                                                                   - parent.getPaddingRight(),
                                                                   View.MeasureSpec.AT_MOST);
        }
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthSpec, heightSpec);
        ValueAnimator animator = createHeightAnimator(view, 0, view.getMeasuredHeight());

        if (!message.isLiked() &&
            !message.getUser().isMe() &&
            !container.getControllerFactory().getUserPreferencesController().hasPerformedAction(IUserPreferencesController.LIKED_MESSAGE)) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mainHandler.removeCallbacksAndMessages(null);
                    mainHandler.postDelayed(showMessageStatusRunnable, LIKE_HINT_VISIBILITY_MIL_SEC);
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    showLikeDetails();
                }
            });
        }

        animator.start();
    }


    private void collapse() {
        if (shouldBeExpanded()) {
            return;
        }
        if (message.getId().equals(container.getExpandedMessageId())) {
            container.setExpandedMessageId(null);
        }
        int origHeight = view.getHeight();

        ValueAnimator animator = createHeightAnimator(view, origHeight, 0);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final Animator animator) {
                view.setVisibility(View.GONE);
            }
        });
        animator.start();
    }

    // TODO will add animations here
    private void showLikeDetails() {
        likeDetails.setVisibility(View.VISIBLE);
        messageStatusTextView.setVisibility(View.INVISIBLE);
    }

    // TODO will add animations here
    private void showMessageStatus() {
        likeDetails.setVisibility(View.INVISIBLE);
        messageStatusTextView.setVisibility(View.VISIBLE);
    }

    private void showTimestampForABit() {
        mainHandler.removeCallbacksAndMessages(null);
        // TODO check this again when adding animations
        if (likeDetails.getVisibility() == View.VISIBLE) {
            showMessageStatus();
            mainHandler.postDelayed(showLikeDetailsRunnable, TIMESTAMP_VISIBILITY_MIL_SEC);
        } else {
            showLikeDetails();
        }
    }

    public ValueAnimator createHeightAnimator(final View view, final int start, final int end) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.setDuration(250);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(final ValueAnimator valueAnimator) {
                ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                if (layoutParams == null) {
                    // needed for tests
                    return;
                }
                layoutParams.height = (Integer) valueAnimator.getAnimatedValue();
                view.setLayoutParams(layoutParams);
            }
        });
        return animator;
    }


    @Override
    public void toggleVisibility() {
        if (view.getVisibility() == View.GONE || view.getMeasuredHeight() == 0) {
            expand();
        } else if (likeButton.getVisibility() == View.GONE) {
            likeButton.setVisibility(View.VISIBLE);
        } else if (message.isLiked()) {
            showTimestampForABit();
        } else {
            collapse();
        }
    }

    @Override
    public void close() {
        if (message != null) {
            collapse();
        } else {
            view.setVisibility(View.GONE);
        }
        if (container.getExpandedView() == this) {
            container.setExpandedView(null);
        }
    }

}
