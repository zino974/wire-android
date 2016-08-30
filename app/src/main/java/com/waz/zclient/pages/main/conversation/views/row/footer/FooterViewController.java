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
import com.waz.zclient.utils.ViewUtils;
import com.waz.zclient.utils.ZTimeFormatter;
import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.Instant;

public class FooterViewController implements ConversationItemViewController, FooterActionCallback {

    private static final int LIKE_HINT_VISIBILITY_MIL_SEC = 3000;
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

    private final ModelObserver<Message> messageModelObserver = new ModelObserver<Message>() {
        @Override
        public void updated(Message message) {
            if (message.isLiked()) {
                showLikeDetails();
            } else {
                showMessageStatus();
            }
            updateLikeButton();
            updateMessageStatusLabel();
            likeDetails.setUsers(message.isLiked() ? message.getLikes() : null);
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
        messageModelObserver.setAndUpdate(message);
        if (message.isLiked()) {
            view.setVisibility(View.VISIBLE);
            likeDetails.setVisibility(View.VISIBLE);
            messageStatusTextView.setVisibility(View.INVISIBLE);
        } else if (message.getId().equals(container.getExpandedMessageId())) {
            if (container.getExpandedView() != null && container.getExpandedView() != this) {
                container.getExpandedView().close();
                container.setExpandedView(this);
            }
            view.setVisibility(View.VISIBLE);
            likeDetails.setVisibility(View.INVISIBLE);
            messageStatusTextView.setVisibility(View.VISIBLE);
        } else {
            // handle refresh
            view.setVisibility(View.GONE);
            likeDetails.setVisibility(View.INVISIBLE);
            messageStatusTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void recycle() {
        view.setVisibility(View.GONE);
        messageModelObserver.clear();
        likeDetails.setUsers(null);
        message = null;
        animateLikeButton = false;
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
        Instant messageTime = message.isEdited() ?
                              message.getEditTime() :
                              message.getTime();
        String timestamp = ZTimeFormatter.getSingleMessageTime(getView().getContext(), DateTimeUtils.toDate(messageTime));
        messageStatusTextView.setText(timestamp);
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

        View parent = (View) view.getParent();
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getMeasuredWidth()
                                                               - parent.getPaddingLeft()
                                                               - parent.getPaddingRight(),
                                                               View.MeasureSpec.AT_MOST);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthSpec, heightSpec);
        ValueAnimator animator = createHeightAnimator(view, 0, view.getMeasuredHeight());


        if (!message.isLiked() &&
            !message.getUser().isMe() &&
            !container.getControllerFactory().getUserPreferencesController().hasPerformedAction(IUserPreferencesController.LIKED_MESSAGE)) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showMessageStatus();
                        }
                    }, LIKE_HINT_VISIBILITY_MIL_SEC);
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
        if (message.isLiked()) {
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

    public ValueAnimator createHeightAnimator(final View view, final int start, final int end) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.setDuration(250);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(final ValueAnimator valueAnimator) {
                ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                layoutParams.height = (Integer) valueAnimator.getAnimatedValue();
                view.setLayoutParams(layoutParams);
            }
        });
        return animator;
    }


    @Override
    public void toggleVisibility() {
        if (view.getVisibility() == View.GONE) {
            expand();
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
