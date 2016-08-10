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
package com.waz.zclient

import _root_.com.waz.api.VoiceChannelState._
import _root_.com.waz.service.ZMessaging
import _root_.com.waz.utils.RichSignalOpt
import _root_.com.waz.utils.events.Signal
import android.os.PowerManager
import com.waz.service.call.VoiceChannelService

class GlobalCallingController(cxt: WireContext)(implicit inj: Injector) extends Injectable {

  implicit val eventContext = cxt.eventContext

  val zms = inject[Signal[Option[ZMessaging]]]

  private val screenManager = new ScreenManager

  val voiceService = zms.map(_.fold(Option.empty[VoiceChannelService])(zms => Some(zms.voice)))

  val callExists = voiceService.flatMapSome(_.callExists)
  val convId = voiceService.flatMapSome(_.convId)
  val currentChannel = voiceService.flatMapSome(_.currentChannel)
  val videoCall = voiceService.flatMapSome(_.videoCall)


  val voiceServiceAndCurrentConvId = voiceService.zip(currentChannel) map {
    case (Some(vcs), vd) => vd.map(data => (vcs, data.id))
    case _ => None
  }

  val callState = currentChannel map {
    case Some(ch) => Some(ch.state)
    case _ => None
  }

  val activeCall = callState.map {
    case Some(SELF_CALLING | SELF_JOINING | SELF_CONNECTED | OTHER_CALLING | OTHERS_CONNECTED) => true
    case _ => false
  }

  var wasUiActiveOnCallStart = false

  val onCallStarted = activeCall.onChanged.filter(_ == true).map { _ =>
    val active = zms.flatMap(_.fold(Signal.const(false))(_.lifecycle.uiActive)).currentValue.getOrElse(false)
    wasUiActiveOnCallStart = active
    active
  }

  videoCall.zip(callState) {
    case (true, _) => screenManager.setStayAwake()
    case (false, Some(st)) if st == OTHER_CALLING => screenManager.setStayAwake()
    case (false, Some(st)) if st == SELF_CALLING | st == SELF_JOINING || st == SELF_CONNECTED => screenManager.setProximitySensorEnabled()
    case _ => screenManager.releaseWakeLock()
  }
}

private class ScreenManager(implicit injector: Injector) extends Injectable {

  private val TAG = "CALLING_WAKE_LOCK"

  private val powerManager = Option(inject[PowerManager])

  private var stayAwake = false
  private var wakeLock: Option[PowerManager#WakeLock] = None

  def setStayAwake() = {
    (stayAwake, wakeLock) match {
      case (_, None) | (false, Some(_)) =>
        this.stayAwake = true
        createWakeLock();
      case _ => //already set
    }
  }

  def setProximitySensorEnabled() = {
    (stayAwake, wakeLock) match {
      case (_, None) | (true, Some(_)) =>
        this.stayAwake = false
        createWakeLock();
      case _ => //already set
    }
  }

  private def createWakeLock() = {
    val flags = if (stayAwake)
      PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
    else PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK
    releaseWakeLock()
    wakeLock = powerManager.map(_.newWakeLock(flags, TAG))
    wakeLock.foreach(_.acquire())
  }

  def releaseWakeLock() = {
    for (wl <- wakeLock if wl.isHeld) wl.release()
    wakeLock = None
  }
}

