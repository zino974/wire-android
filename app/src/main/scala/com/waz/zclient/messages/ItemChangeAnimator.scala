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
package com.waz.zclient.messages

import java.util

import android.animation.Animator.AnimatorListener
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.animation.{Animator, ValueAnimator}
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.View
import com.waz.zclient.messages.parts.footer.FooterPartView
import com.waz.zclient.ui.animation.interpolators.penner.Elastic.EaseIn

class ItemChangeAnimator extends DefaultItemAnimator {

  private var pendingChanges = Set.empty[MessageViewHolder]
  private var anims = Set.empty[FooterHideAnimator]

  // always reuse view holder, we will handle animations ourselves
  override def canReuseUpdatedViewHolder(viewHolder: ViewHolder, payloads: util.List[AnyRef]): Boolean = true

  override def animateChange(oldHolder: ViewHolder, newHolder: ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean = {
    newHolder match {
      case vh: MessageViewHolder if vh.view.isFooterHiding =>
        pendingChanges += vh
      case _ =>
    }
    super.animateChange(oldHolder, newHolder, fromX, fromY, toX, toY)
  }

  override def runPendingAnimations(): Unit = {
    super.runPendingAnimations()
    anims ++= pendingChanges flatMap { vh =>
      vh.view.getFooter map { footer =>
        footer.setVisibility(View.VISIBLE)
        FooterHideAnimator(vh, footer)
      }
    }
    pendingChanges = Set.empty
    anims foreach { _.start() }
  }

  override def endAnimations(): Unit = {
    anims.foreach(_.end())
    pendingChanges = Set.empty
    anims = Set.empty
    super.endAnimations() //dispatchAnimationsFinished called by super method
  }

  override def isRunning: Boolean = super.isRunning || pendingChanges.nonEmpty || anims.nonEmpty

  /**
    * Animates footer size to hide it.
    * Note: this animator changes view layout params, this is ugly, inefficient, causes layout pass on every frame,
    * but I could't find any better way to achieve desired result in RecyclerView.
    */
  case class FooterHideAnimator(holder: MessageViewHolder, footer: FooterPartView) extends ValueAnimator with AnimatorListener with AnimatorUpdateListener {
    setFloatValues(0f, 1f)
    addListener(this)
    addUpdateListener(this)
    val height = footer.getHeight
    val lp = footer.getLayoutParams

    override def onAnimationUpdate(animation: ValueAnimator): Unit = {
      lp.height = height - (height * animation.getAnimatedFraction).toInt
      footer.setLayoutParams(lp)
    }

    override def onAnimationStart(animation: Animator): Unit =
      dispatchChangeStarting(holder, false)

    override def onAnimationEnd(animation: Animator): Unit = {
      anims -= this
      holder.view.removeListPart(footer)
      lp.height = height
      footer.setLayoutParams(lp)
      dispatchChangeFinished(holder, false)
    }

    override def onAnimationRepeat(animation: Animator): Unit = ()

    override def onAnimationCancel(animation: Animator): Unit = onAnimationEnd(animation)
  }
}
