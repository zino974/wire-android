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
package com.waz.zclient.notifications.controllers

import android.annotation.TargetApi
import android.app.{Notification, NotificationManager, PendingIntent}
import android.content.pm.PackageManager
import android.graphics.drawable.{BitmapDrawable, Drawable}
import android.graphics.{Bitmap, BitmapFactory, Canvas}
import android.net.Uri
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.text.style.{ForegroundColorSpan, TextAppearanceSpan}
import android.text.{SpannableString, Spanned}
import com.waz.api.NotificationsHandler.GcmNotification
import com.waz.api.NotificationsHandler.GcmNotification.Type._
import com.waz.service.ZMessaging
import com.waz.service.push.NotificationService.Notification2
import com.waz.threading.Threading
import com.waz.utils.events.Signal
import com.waz.zclient._
import com.waz.zclient.controllers.vibrator.VibratorController
import com.waz.zclient.utils.IntentUtils
import com.waz.zms.GcmHandlerService

//TODO rename when old class deleted
class NewNotificationsController(cxt: WireContext)(implicit inj: Injector) extends Injectable {

  import NotificationsController._
  implicit val eventContext = cxt.eventContext

  val zms = inject[Signal[Option[ZMessaging]]].collect { case Some(z) => z }

  val notsService = zms.map(_.notifications)

  val notManager = inject[NotificationManager]

  val notifications = notsService.flatMap(_.getNotifications2)

  lazy val clearIntent = PendingIntent.getService(cxt, 9730, GcmHandlerService.clearNotificationsIntent(cxt), PendingIntent.FLAG_UPDATE_CURRENT)

  notifications.on(Threading.Ui) { nots =>
    val n =
      if (nots.size == 1) {
        getSingleMessageNotification(nots.head)
      } else {
        getMultipleMessagesNotification(nots)
      }

    n.priority = Notification.PRIORITY_HIGH
    n.flags |= Notification.FLAG_AUTO_CANCEL
    n.deleteIntent = clearIntent
    notManager.notify(ZETA_MESSAGE_NOTIFICATION_ID, n)
  }

  private def getSingleMessageNotification(n: Notification2): Notification = {

    val spannableString = getMessage(n, multiple = false, singleConversationInBatch = true, singleUserInBatch = true)
    val title = getMessageTitle(n)

    val builder = new NotificationCompat.Builder(cxt)
    val requestBase = System.currentTimeMillis.toInt

    val bigTextStyle = new NotificationCompat.BigTextStyle
    bigTextStyle.setBigContentTitle(title)
    bigTextStyle.bigText(spannableString)

    builder
      .setSmallIcon(R.drawable.ic_menu_logo)
      .setLargeIcon(getAppIcon)
      .setContentTitle(title)
      .setContentText(spannableString)
      .setContentIntent(IntentUtils.getNotificationAppLaunchIntent(cxt, n.convId.str, requestBase))
      .setStyle(bigTextStyle)
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
      .setPriority(NotificationCompat.PRIORITY_HIGH)


    if (n.tpe ne GcmNotification.Type.CONNECT_REQUEST) {
      builder
        .addAction(R.drawable.ic_action_call, cxt.getString(R.string.notification__action__call), IntentUtils.getNotificationCallIntent(cxt, n.convId.str, requestBase + 1))
        .addAction(R.drawable.ic_action_reply, cxt.getString(R.string.notification__action__reply), IntentUtils.getNotificationReplyIntent(cxt, n.convId.str, requestBase + 2))
    }

    if (VibratorController.isEnabledInPreferences(cxt)) {
      builder.setVibrate(VibratorController.resolveResource(cxt.getResources, R.array.new_message_gcm))
    }
    builder.build
  }

  private def getMultipleMessagesNotification(ns: Seq[Notification2]): Notification = {

    val convIds = ns.map(_.convId).toSet
    val users = ns.map(_.userName).toSet

    val isSingleConv = convIds.size == 1
    val items = ns.map(n => getMessage(n, multiple = true, singleConversationInBatch = isSingleConv, singleUserInBatch = users.size == 1)).reverse

    val (convDesc, headerRes) =
      if (isSingleConv) {
        if (ns.head.isGroupConv) (ns.head.convName, R.plurals.notification__new_messages_groups)
        else (ns.head.userName, R.plurals.notification__new_messages)
      }
      else (String.valueOf(convIds.size), R.plurals.notification__new_messages__multiple)

    val title = cxt.getResources.getQuantityString(headerRes, ns.size, ns.size, convDesc)

    val inboxStyle = new NotificationCompat.InboxStyle
    inboxStyle.setBigContentTitle(title)

    val builder = new NotificationCompat.Builder(cxt)
      .setSmallIcon(R.drawable.ic_menu_logo)
      .setLargeIcon(getAppIcon).setNumber(ns.size)
      .setContentTitle(title)
      .setStyle(inboxStyle)
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
      .setPriority(NotificationCompat.PRIORITY_HIGH)

    if (VibratorController.isEnabledInPreferences(cxt)) {
      builder.setVibrate(VibratorController.resolveResource(cxt.getResources, R.array.new_message_gcm))
    }
    if (isSingleConv) {
      val requestBase = System.currentTimeMillis.toInt
      val conversationId = convIds.head.str
      builder
        .setContentIntent(IntentUtils.getNotificationAppLaunchIntent(cxt, conversationId, requestBase))
        .addAction(R.drawable.ic_action_call, cxt.getString(R.string.notification__action__call), IntentUtils.getNotificationCallIntent(cxt, conversationId, requestBase + 1))
        .addAction(R.drawable.ic_action_reply, cxt.getString(R.string.notification__action__reply), IntentUtils.getNotificationReplyIntent(cxt, conversationId, requestBase + 2))
    }
    else builder.setContentIntent(IntentUtils.getNotificationAppLaunchIntent(cxt))

    items.take(5).foreach { line =>
      builder.setContentText(line)
      inboxStyle.addLine(line)
    }

    builder.build
  }

  private def getMessage(n: Notification2, multiple: Boolean, singleConversationInBatch: Boolean, singleUserInBatch: Boolean): SpannableString = {
    val message = n.message.replaceAll("\\r\\n|\\r|\\n", " ")

    def getHeader(testPrefix: Boolean = false, singleUser: Boolean = false) = getDefaultNotificationMessageLineHeader(n, multiple, textPrefix = testPrefix, singleConversationInBatch = singleConversationInBatch, singleUser = singleUser)

    val header = n.tpe match {
      case TEXT | CONNECT_REQUEST => getHeader(testPrefix = true, singleUser = singleUserInBatch)
      case CONNECT_ACCEPTED => if (multiple) cxt.getString(R.string.notification__message__name__prefix__other, n.convName) else ""
      case _ => getHeader()
    }

    val body = n.tpe match {
      case TEXT | CONNECT_REQUEST => message
      case MISSED_CALL => cxt.getString(R.string.notification__message__one_to_one__wanted_to_talk)
      case KNOCK => if (n.isGroupConv) cxt.getString(R.string.notification__message__group__pinged) else cxt.getString(R.string.notification__message__one_to_one__pinged)
      case ANY_ASSET => if (n.isGroupConv) cxt.getString(R.string.notification__message__group__shared_file) else cxt.getString(R.string.notification__message__one_to_one__shared_file)
      case ASSET => if (n.isGroupConv) cxt.getString(R.string.notification__message__group__shared_picture) else cxt.getString(R.string.notification__message__one_to_one__shared_picture)
      case VIDEO_ASSET => if (n.isGroupConv) cxt.getString(R.string.notification__message__group__shared_video) else cxt.getString(R.string.notification__message__one_to_one__shared_video)
      case AUDIO_ASSET => if (n.isGroupConv) cxt.getString(R.string.notification__message__group__shared_audio) else cxt.getString(R.string.notification__message__one_to_one__shared_audio)
      case LOCATION => if (n.isGroupConv) cxt.getString(R.string.notification__message__group__shared_location) else cxt.getString(R.string.notification__message__one_to_one__shared_location)
      case RENAME => cxt.getString(R.string.notification__message__group__renamed_conversation, message)
      case MEMBER_LEAVE => cxt.getString(R.string.notification__message__group__remove)
      case MEMBER_JOIN => cxt.getString(R.string.notification__message__group__add)
      case CONNECT_ACCEPTED => if (multiple) cxt.getString(R.string.notification__message__multiple__accept_request) else cxt.getString(R.string.notification__message__single__accept_request)
      case _ => ""
    }
    getMessageSpannable(header, body)
  }

  private def getMessageTitle(n: Notification2): String = {
    val userName = n.userName.getOrElse("")
    if (n.isGroupConv) {
      val convName = n.convName.filterNot(_.isEmpty).getOrElse(cxt.getString(R.string.notification__message__group__default_conversation_name))
      cxt.getString(R.string.notification__message__group__prefix__other, userName, convName)
    }
    else userName
  }

  @TargetApi(21)
  private def getMessageSpannable(header: String, body: String): SpannableString = {
    val messageSpannable = new SpannableString(header + body)
    val textAppearance =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) android.R.style.TextAppearance_Material_Notification_Title
      else android.R.style.TextAppearance_StatusBar_EventContent_Title
    messageSpannable.setSpan(new ForegroundColorSpan(new TextAppearanceSpan(cxt, textAppearance).getTextColor.getDefaultColor), 0, header.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    messageSpannable
  }

  private def getDefaultNotificationMessageLineHeader(n: Notification2, multiple: Boolean, textPrefix: Boolean, singleConversationInBatch: Boolean, singleUser: Boolean) = {
    val prefixId = if (multiple) {
      if (n.isGroupConv && !singleConversationInBatch) if (textPrefix) R.string.notification__message__group__prefix__text else R.string.notification__message__group__prefix__other
      else if (!singleUser || n.isGroupConv) if (textPrefix) R.string.notification__message__name__prefix__text else R.string.notification__message__name__prefix__other
      else 0
    }
    else 0

    if (prefixId == 0) ""
    else cxt.getString(prefixId, n.userName.getOrElse(""), n.convName.filterNot(_.isEmpty).getOrElse(cxt.getString(R.string.notification__message__group__default_conversation_name)))
  }

  def dismissImageSavedNotification(uri: Uri) = {
  }

  private def getAppIcon: Bitmap = {
    try {
      val icon: Drawable = cxt.getPackageManager.getApplicationIcon(cxt.getPackageName)
      icon match {
        case drawable: BitmapDrawable =>
          drawable.getBitmap
        case _ =>
          val bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth, icon.getIntrinsicHeight, Bitmap.Config.ARGB_8888)
          val canvas = new Canvas(bitmap)
          icon.setBounds(0, 0, canvas.getWidth, canvas.getHeight)
          icon.draw(canvas)
          bitmap
      }
    }
    catch {
      case e: PackageManager.NameNotFoundException => {
        BitmapFactory.decodeResource(cxt.getResources, R.drawable.ic_launcher_wire)
      }
    }
  }
}

object NotificationsController {
  val ZETA_MESSAGE_NOTIFICATION_ID: Int = 1339272
}
