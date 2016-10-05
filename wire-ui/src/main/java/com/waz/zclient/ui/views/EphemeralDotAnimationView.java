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
package com.waz.zclient.ui.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.waz.api.Message;
import com.waz.zclient.core.api.scala.ModelObserver;
import com.waz.zclient.ui.R;
import org.threeten.bp.Instant;
import org.threeten.bp.temporal.ChronoUnit;

public class EphemeralDotAnimationView extends View {

    private static final int NUMBER_OF_DOTS = 5;
    private static final int DEFAULT_PRIMARY_COLOR = Color.GRAY;
    private static final int DEFAULT_SECONDARY_COLOR = Color.LTGRAY;
    private int dotHeight;
    private int dotPadding;
    private int primaryColor;
    private int secondaryColor;
    private int selectedCount;
    private Paint paint;
    private ValueAnimator valueAnimator;

    private ModelObserver<Message> messageModelObserver = new ModelObserver<Message>() {
        @Override
        public void updated(Message message) {
            if (message.isEphemeral()) {
                if (message.getExpirationTime() == Instant.MAX) {
                    setVisibility(View.VISIBLE);
                    showAllDots();
                } else {
                    long diff = ChronoUnit.MILLIS.between(Instant.now(), message.getExpirationTime());
                    if (diff > 0) {
                        startAnimation(message.getEphemeralExpiration().milliseconds,
                                       diff);
                    } else {
                        setVisibility(View.GONE);
                    }
                }
            } else {
                setVisibility(View.GONE);
            }
        }
    };

    public EphemeralDotAnimationView(Context context) {
        this(context, null);
    }

    public EphemeralDotAnimationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EphemeralDotAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        primaryColor = DEFAULT_PRIMARY_COLOR;
        secondaryColor = DEFAULT_SECONDARY_COLOR;
        dotPadding = 0;
        if (attrs != null) {
            TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.EphemeralDotAnimationView);
            if (ta != null) {
                primaryColor = ta.getColor(R.styleable.EphemeralDotAnimationView_primaryColor, primaryColor);
                secondaryColor = ta.getColor(R.styleable.EphemeralDotAnimationView_secondaryColor, secondaryColor);
                dotPadding = ta.getDimensionPixelSize(R.styleable.EphemeralDotAnimationView_dotPadding, dotPadding);
                ta.recycle();
            }
        }
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        setVisibility(GONE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (canvas == null || canvas.getHeight() == 0) {
            return;
        }
        int x = canvas.getWidth() / 2;
        int initialY = getPaddingTop() + dotHeight / 2;
        float radius = (float) dotHeight / 2;
        for (int i = 0; i < NUMBER_OF_DOTS; i++) {
            paint.setColor(i + selectedCount >= NUMBER_OF_DOTS ? primaryColor : secondaryColor);
            int y = dotHeight * i + initialY + dotPadding * i;
            canvas.drawCircle(x, y, radius, paint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        int totalDotPadding = dotPadding * (NUMBER_OF_DOTS - 1);
        dotHeight = (height - totalDotPadding) / NUMBER_OF_DOTS;

        invalidate();
    }

    public void setDotPadding(int dotPadding) {
        this.dotPadding = dotPadding;
    }

    public void setPrimaryColor(int primaryColor) {
        this.primaryColor = primaryColor;
    }

    public void setSecondaryColor(int secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    private void cancelAnimation() {
        if (valueAnimator != null) {
            valueAnimator.cancel();
            valueAnimator = null;
        }
    }

    private void showAllDots() {
        selectedCount = NUMBER_OF_DOTS;
        invalidate();
    }

    private void startAnimation(long totalMilliSeconds, long remainingMilliSeconds) {
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        valueAnimator = ObjectAnimator.ofInt(NUMBER_OF_DOTS, 0);
        valueAnimator.setDuration(totalMilliSeconds);
        valueAnimator.setCurrentPlayTime(totalMilliSeconds - remainingMilliSeconds);
        valueAnimator.setInterpolator(null);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int updatedValue = (int) animation.getAnimatedValue();
                if (updatedValue != selectedCount) {
                    selectedCount = updatedValue;
                    invalidate();
                }
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(GONE);
            }
        });
        setVisibility(View.VISIBLE);
        valueAnimator.start();
    }

    public void setMessage(Message message) {
        cancelAnimation();
        if (message == null) {
            messageModelObserver.clear();
            setVisibility(GONE);
        } else {
            messageModelObserver.setAndUpdate(message);
        }
    }
}
