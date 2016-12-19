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
package com.waz.zclient.conversation

import android.content.Context
import android.graphics.Bitmap
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.LinearLayout
import com.waz.model.{AssetData, AssetId}
import com.waz.threading.Threading
import com.waz.utils.events.{EventContext, Signal}
import com.waz.zclient.conversation.CollectionAdapter.CollViewHolder
import com.waz.zclient.pages.main.conversation.views.AspectRatioImageView
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.ui.utils.ResourceUtils
import com.waz.zclient.utils.ViewUtils
import com.waz.zclient.{Injectable, Injector, R}
import org.threeten.bp.temporal.ChronoUnit
import org.threeten.bp.{Instant, LocalDateTime, ZoneId}

//For now just handling images
class CollectionAdapter(val screenWidth: Int, val columns: Int)(implicit context: Context, injector: Injector, eventContext: EventContext) extends RecyclerView.Adapter[CollectionAdapter.CollViewHolder] with Injectable {

  val ctrler = new CollectionController(Seq(CollectionController.Images), 3)
  val images = ctrler.collectionMessages.flatMap(_.getOrElse(CollectionController.Images, Signal.empty))

  images.onChanged.on(Threading.Ui) { _ => notifyDataSetChanged() }

  override def getItemCount: Int = images.currentValue.map(_.size).getOrElse(0)

  override def onBindViewHolder(holder: CollViewHolder, position: Int): Unit =
    holder.setAsset(images.currentValue.getOrElse(Seq.empty)(position)._1, ctrler.bitmapSignal, screenWidth / columns, ResourceUtils.getRandomAccentColor(context))

  override def onCreateViewHolder(parent: ViewGroup, viewType: Int): CollViewHolder =
    CollViewHolder(LayoutInflater.from(parent.getContext).inflate(R.layout.row_collection_image, parent, false).asInstanceOf[AspectRatioImageView])

  def getHeaderId(position: Int): Int = {
    val time = images.currentValue.getOrElse(Seq.empty)(position)._2
    val now = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toLocalDate

    // TODO just testing headers here...
    if (now == LocalDateTime.ofInstant(time, ZoneId.systemDefault()).toLocalDate())
      Header.today
    else if (now.minus(1, ChronoUnit.DAYS) == LocalDateTime.ofInstant(time, ZoneId.systemDefault()).toLocalDate())
      Header.yesterday
    else
      Header.agesAgo
  }

  def getHeaderView(parent: RecyclerView, position: Int): View = {
    val header = new LinearLayout(parent.getContext)
    if (header.getLayoutParams == null) {
      header.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
    header.setBackgroundColor(ContextCompat.getColor(parent.getContext, R.color.light_graphite_24))

    val textView = new TypefaceTextView(parent.getContext)
    textView.setText(getHeaderText(getHeaderId(position)))
    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources.getDimensionPixelSize(R.dimen.wire__text_size__small))
    textView.setTypeface(context.getString(R.string.wire__typeface__bold))
    val padding = parent.getContext.getResources.getDimensionPixelSize(R.dimen.wire__padding__small)
    textView.setPadding(padding, padding, padding, padding)
    header.addView(textView)

    val heightSpec: Int = View.MeasureSpec.makeMeasureSpec(parent.getHeight, View.MeasureSpec.EXACTLY)
    val childHeight: Int = ViewGroup.getChildMeasureSpec(heightSpec, parent.getPaddingTop + parent.getPaddingBottom, header.getLayoutParams.height)
    header.measure(LayoutParams.MATCH_PARENT, childHeight)
    header.layout(0, 0, parent.getMeasuredWidth, header.getMeasuredHeight)
    header
  }

  private def getHeaderText(headerId: Int): String = {
    headerId match {
      case Header.today => "TODAY"
      case Header.yesterday => "YESTERDAY"
      case Header.agesAgo => "AGES AGO"
      case _ => "Whatever"
    }
  }

}

object Header {
  val today: Int = 0
  val yesterday: Int = 1
  val agesAgo: Int = 2
}

object CollectionAdapter {

  case class CollViewHolder(view: AspectRatioImageView)(implicit eventContext: EventContext) extends RecyclerView.ViewHolder(view) {

    def setAsset(asset: Signal[AssetData], bitmap: (AssetId, Int) => Signal[Option[Bitmap]], width: Int, color: Int) = asset.on(Threading.Ui) { a =>

      view.setAspectRatio(1)
      view.setImageBitmap(null)
      view.setBackgroundColor(color)
      ViewUtils.setWidth(view, width)
      ViewUtils.setHeight(view, width)
      bitmap(a.id, view.getWidth).on(Threading.Ui) {
        case Some(b) => view.setImageBitmap(b)
        case None => //TODO bitmap didn't load
      }

    }
  }

}
