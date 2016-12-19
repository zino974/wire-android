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
import android.support.v7.widget.RecyclerView.ViewHolder
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{LinearLayout, TextView}
import com.waz.ZLog._
import com.waz.model.{AssetData, AssetId, MessageData}
import com.waz.threading.Threading
import com.waz.utils.events.{EventContext, Signal}
import com.waz.utils.returning
import com.waz.zclient.conversation.CollectionAdapter.{CollViewHolder, FileViewHolder}
import com.waz.zclient.pages.main.conversation.views.AspectRatioImageView
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.ui.utils.ResourceUtils
import com.waz.zclient.utils.ViewUtils
import com.waz.zclient.{Injectable, Injector, R}
import org.threeten.bp.temporal.ChronoUnit
import org.threeten.bp.{Instant, LocalDateTime, ZoneId}

//For now just handling images
class CollectionAdapter(screenWidth: Int, columns: Int, ctrler: CollectionController)(implicit context: Context, injector: Injector, eventContext: EventContext) extends RecyclerView.Adapter[ViewHolder] with Injectable {

  private implicit val tag: LogTag = logTagFor[CollectionAdapter]

  /**
    * If signals don't have any subscribers, then by default they don't bother computing their values whenever changes are published to them,
    * until they get their first subscriber. If we then try to call Signal#getCurrentValue on such a signal, we'll probably get None or something undefined.
    * There are two ways around this, either call Signal#disableAutoWiring on any signals you wish to be able to access, or have a temporary var that keeps
    * track of the current value, and set listeners to update that var.
    *
    * I'm starting to prefer the second way, as it's a little bit more explicit as to what's happening. Both ways should be used cautiously!!
    */

  val all = ctrler.messagesByType(CollectionController.All)
  private var _all = Seq.empty[MessageData]
  all(_all = _)

  val images = ctrler.messagesByType(CollectionController.Images)
  private var _images = Seq.empty[MessageData]
  images(_images = _)

  val files = ctrler.messagesByType(CollectionController.Files)
  private var _files = Seq.empty[MessageData]
  files(_files = _)

  var contentMode = CollectionAdapter.VIEW_MODE_IMAGES

  images.onChanged.on(Threading.Ui) { _ => notifyDataSetChanged() }

  override def getItemCount: Int = {
    contentMode match {
      case CollectionAdapter.VIEW_MODE_ALL => 0 //TODO
      case CollectionAdapter.VIEW_MODE_FILES => files.currentValue.map(_.size).getOrElse(0)
      case CollectionAdapter.VIEW_MODE_IMAGES => images.currentValue.map(_.size).getOrElse(0)
      case _ => 0
    }
  }

  override def getItemViewType(position: Int): Int = {
    contentMode match {
      case CollectionAdapter.VIEW_MODE_ALL => 0 //TODO
      case CollectionAdapter.VIEW_MODE_FILES => CollectionAdapter.VIEW_TYPE_FILE
      case CollectionAdapter.VIEW_MODE_IMAGES => CollectionAdapter.VIEW_TYPE_IMAGE
      case _ => 0
    }
  }

  override def onBindViewHolder(holder: ViewHolder, position: Int): Unit = {
    verbose(s"ASDF loadCursor for $position")
    holder match {
      case f: FileViewHolder => assetSignal(_files, position).foreach(f.setAsset)
      case c: CollViewHolder => assetSignal(_images, position).foreach(s => c.setAsset(s, ctrler.bitmapSignal, screenWidth / columns, ResourceUtils.getRandomAccentColor(context)))
    }
  }

  private def assetSignal(col: Seq[MessageData], pos: Int): Option[Signal[AssetData]] = col.lift(pos).map(m => ctrler.assetSignal(m.assetId))

  override def onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
    viewType match {
//      case CollectionAdapter.VIEW_TYPE_FILE => FileViewHolder(LayoutInflater.from(parent.getContext).inflate(R.layout.row_collection_file, parent, false).asInstanceOf[TextView])
      case CollectionAdapter.VIEW_TYPE_FILE => FileViewHolder(new TextView(parent.getContext))
      case CollectionAdapter.VIEW_TYPE_IMAGE => CollViewHolder(LayoutInflater.from(parent.getContext).inflate(R.layout.row_collection_image, parent, false).asInstanceOf[AspectRatioImageView])
      case _ => returning(null.asInstanceOf[ViewHolder])(_ => error(s"Unexpected ViewType: $viewType"))
    }

  def getHeaderId(position: Int): Int = {
    val time = images.currentValue.getOrElse(Seq.empty)(position).time
    val now = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toLocalDate

    // TODO just testing headers here...
    if (now == LocalDateTime.ofInstant(time, ZoneId.systemDefault()).toLocalDate())
      DetailsHeader.today
    else if (now.minus(1, ChronoUnit.DAYS) == LocalDateTime.ofInstant(time, ZoneId.systemDefault()).toLocalDate())
      DetailsHeader.yesterday
    else
      DetailsHeader.agesAgo
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
      case DetailsHeader.today => "TODAY"
      case DetailsHeader.yesterday => "YESTERDAY"
      case DetailsHeader.agesAgo => "AGES AGO"
      case _ => "Whatever"
    }
  }

}

object DetailsHeader {
  val today: Int = 0
  val yesterday: Int = 1
  val agesAgo: Int = 2
}

object CollectionAdapter {

  val VIEW_MODE_ALL: Int = 0
  val VIEW_MODE_IMAGES: Int = 1
  val VIEW_MODE_FILES: Int = 2

  val VIEW_TYPE_IMAGE = 0
  val VIEW_TYPE_FILE = 1

  case class FileViewHolder(view: TextView)(implicit eventContext: EventContext) extends RecyclerView.ViewHolder(view) {

    def setAsset(asset: Signal[AssetData]) = asset.on(Threading.Ui) { a =>
      view.setText(a.fileExtension)
    }

  }

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
