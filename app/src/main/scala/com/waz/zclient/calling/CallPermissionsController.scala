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
package com.waz.zclient.calling

import android.content.DialogInterface
import com.waz.api.NetworkMode
import com.waz.api.VoiceChannelState.OTHER_CALLING
import com.waz.model.{ConvId, VoiceChannelData}
import com.waz.threading.Threading
import com.waz.utils.events.{EventStream, EventStreamWithAuxSignal, Signal}
import com.waz.zclient._
import com.waz.zclient.utils.PhoneUtils.PhoneState
import com.waz.zclient.utils.{PhoneUtils, ViewUtils}
import timber.log.Timber
/**
  * This class is intended to be a relatively small controller that every PermissionsActivity can have access to in order
  * to start and accept calls. This controller requires a PermissionsActivity so that it can request and display the
  * related permissions dialogs, that's why it can't be in the GlobalCallController
  */
class CallPermissionsController(implicit inj: Injector, cxt: WireContext) extends Injectable {

  Timber.d(s"CallPermissionsController starting, context: $cxt")

  implicit val eventContext = cxt.eventContext

  val globController = inject[GlobalCallingController]
  val permissionsController = inject[PermissionsController]

  val voiceService = globController.voiceService
  val currentConvAndVoiceService = globController.voiceServiceAndCurrentConvId
  val videoCall = globController.videoCall

  val zms = globController.zms.collect { case Some(v) => v }
  val networkMode = zms.flatMap(_.network.networkMode)
  val autoAnswerPreference = zms.flatMap(_.prefs.uiPreferenceBooleanSignal(cxt.getResources.getString(R.string.pref_dev_auto_answer_call_key)).signal)

  val currentChannel = globController.currentChannel //FIXME will probably not update if there is no previous channel...

  val incomingCall = currentChannel.collect { case Some(c) => c }.map(_.state).map {
    case OTHER_CALLING => true
    case _ => false
  }

  incomingCall.zip(autoAnswerPreference) {
    case (true, true) => acceptCall()
    case _ =>
  }

  def acceptCall(): Unit = {
    (videoCall.currentValue.getOrElse(false), currentConvAndVoiceService.currentValue.getOrElse(None)) match {
      case (withVideo, Some((vcs, id))) =>
        permissionsController.requiring(if (withVideo) Set(CameraPermission, RecordAudioPermission) else Set(RecordAudioPermission)) {
          vcs.joinVoiceChannel(id, withVideo)
        }(R.string.calling__cannot_start__title,
          if (withVideo) R.string.calling__cannot_start__no_video_permission__message else R.string.calling__cannot_start__no_permission__message,
          vcs.silenceVoiceChannel(id))
      case _ =>
    }
  }

  val startCallRequest = EventStream[(ConvId, Boolean)]

  def startCall(convId: String, withVideo: Boolean) = startCall(ConvId(convId), withVideo)

  def startCall(convId: ConvId, withVideo: Boolean) = startCallRequest !(convId, withVideo)

  new EventStreamWithAuxSignal[(ConvId, Boolean), (NetworkMode, Option[VoiceChannelData])](startCallRequest, Signal(networkMode, currentChannel)).on(Threading.Ui) {
    case ((convId, withVideo), (nMode, prCh)) =>
      nMode match {
        case NetworkMode.OFFLINE =>
          ViewUtils.showAlertDialog(cxt,
            R.string.alert_dialog__no_network__header,
            R.string.calling__call_drop__message,
            R.string.alert_dialog__confirmation,
            new DialogInterface.OnClickListener() {
              def onClick(dialog: DialogInterface, which: Int) {
              }
            }, false)
        case NetworkMode._2G =>
          ViewUtils.showAlertDialog(cxt,
            R.string.calling__slow_connection__title,
            R.string.calling__slow_connection__message,
            R.string.calling__slow_connection__button,
            null, true)
        case NetworkMode.EDGE if withVideo =>
          ViewUtils.showAlertDialog(cxt,
            R.string.calling__slow_connection__title,
            R.string.calling__video_call__slow_connection__message,
            R.string.calling__slow_connection__button,
            new DialogInterface.OnClickListener() {
              def onClick(dialogInterface: DialogInterface, i: Int) {
                finallyStartCall(convId, withVideo, prCh)
              }
            }, true)
        case NetworkMode.EDGE if !withVideo =>
          finallyStartCall(convId, withVideo, prCh)
        case NetworkMode._3G | NetworkMode._4G | NetworkMode.WIFI =>
          finallyStartCall(convId, withVideo, prCh)
      }
  }

  private def cannotStartAlreadyHaveVoiceActive(convId: ConvId, withVideo: Boolean) = {
    ViewUtils.showAlertDialog(cxt,
      R.string.calling__cannot_start__ongoing_voice__title,
      R.string.calling__cannot_start__ongoing_voice__message,
      R.string.calling__cannot_start__ongoing_voice__button_positive,
      R.string.calling__cannot_start__ongoing_voice__button_negative,
      new DialogInterface.OnClickListener() {
        def onClick(dialog: DialogInterface, which: Int) {
          voiceService.currentValue.flatten.foreach(_.leaveVoiceChannel(convId).andThen {
            case _ => finallyStartCall(convId, withVideo, None)
          }(Threading.Ui))
        }
      },
      null)
  }

  private def cannotStartGSMDialog(): Unit = ViewUtils.showAlertDialog(cxt,
    R.string.calling__cannot_start__title,
    R.string.calling__cannot_start__message,
    R.string.calling__cannot_start__button,
    null, true)

  private def differentCallTypeDialog(convName: String): Unit = ViewUtils.showAlertDialog(cxt,
    cxt.getString(R.string.calling__cannot_start__ongoing_different_kind__title, convName),
    cxt.getString(R.string.calling__cannot_start__ongoing_different_kind__message),
    cxt.getString(R.string.calling__cannot_start__button),
    null, true)

  private def finallyStartCall(convId: ConvId, withVideo: Boolean, prevChannel: Option[VoiceChannelData]) = {
    if (PhoneUtils.getPhoneState(cxt) == PhoneState.IDLE && prevChannel.exists(_.ongoing)) {
      cannotStartAlreadyHaveVoiceActive(convId, withVideo)
    }
    else if (PhoneUtils.getPhoneState(cxt) != PhoneState.IDLE) {
      cannotStartGSMDialog()
    }

    permissionsController.requiring(if (withVideo) Set(CameraPermission, RecordAudioPermission) else Set(RecordAudioPermission)) {
      voiceService.currentValue.foreach(_.foreach(_.joinVoiceChannel(convId, withVideo)))
    }(R.string.calling__cannot_start__title,
      if (withVideo) R.string.calling__cannot_start__no_video_permission__message else R.string.calling__cannot_start__no_permission__message)
  }
}
