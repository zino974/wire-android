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

import com.waz.ZLog._
import com.waz.api.{IConversation, Message}
import com.waz.bitmap.BitmapUtils
import com.waz.content.MessagesStorage
import com.waz.model.MessageData.MessageDataDao
import com.waz.model.{AssetData, AssetId, ConvId}
import com.waz.service.ZMessaging
import com.waz.service.assets.AssetService.BitmapResult
import com.waz.service.assets.AssetService.BitmapResult.BitmapLoaded
import com.waz.service.images.BitmapSignal
import com.waz.threading.SerialDispatchQueue
import com.waz.ui.MemoryImageCache.BitmapRequest.Single
import com.waz.utils.events.Signal
import com.waz.zclient.{Injectable, Injector}

//testable!
protected class CollectionController(implicit injector: Injector) extends Injectable {

  private implicit val tag: LogTag = logTagFor[CollectionController]

  private implicit val dispatcher = new SerialDispatchQueue(name = "CollectionController")

  val zms = inject[Signal[ZMessaging]]

  val currentConv = zms.flatMap(_.convsStats.selectedConversationId).collect { case Some(id) => id }

  val msgStorage = zms.map(_.messagesStorage)

  val assetStorage = zms.map(_.assetsStorage)

  val images = assetStorage.zip(currentConv.zip(msgStorage).flatMap {
    case (id, msgs) => Signal.future(loadImages(id, msgs))
  }).map {
    case (as, ids) => ids.map {
      case (a, t) => (as.signal(a), t)
    }
  }

  val conversation = zms.zip(currentConv) flatMap { case (zms, convId) => zms.convsStorage.signal(convId) }

  val conversationName = conversation map (data => if (data.convType == IConversation.Type.GROUP) data.name.filter(!_.isEmpty).getOrElse(data.generatedName) else data.generatedName)

  private def loadImages(conv: ConvId, storage: MessagesStorage) = {
    verbose(s"loadCursor for $conv")
    storage.find(m => m.convId == conv && m.msgType == Message.Type.ASSET, MessageDataDao.findByType(conv, Message.Type.ASSET)(_), m => (m.assetId, m.time)).map(_.sortBy(_._2).reverse)
  }

  def bitmapSignal(assetId: AssetId, width: Int) = zms.flatMap { zms =>
    zms.assetsStorage.signal(assetId).flatMap {
      case data@AssetData.IsImage() => BitmapSignal(data, Single(width), zms.imageLoader, zms.imageCache)
      case _ => Signal.empty[BitmapResult]
    }
  }.map {
    case BitmapLoaded(bmp, _) => Option(BitmapUtils.cropRect(bmp, width))
    case _ => None
  }

}
