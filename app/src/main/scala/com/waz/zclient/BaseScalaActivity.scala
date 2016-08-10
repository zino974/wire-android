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

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.waz.model.ConvId
import com.waz.threading.Threading
import com.waz.zclient.calling.{CallPermissionsController, CallingActivity}

class BaseScalaActivity extends AppCompatActivity with PermissionActivity {

  //TODO all this stuff here is ugly and creates extra controllers in Activities that don't need them. Move all this
  //TODO to a java friendly method in a controller that we can inject into a Java class
  lazy val callPermissionsController = inject[CallPermissionsController]
  
  def injectJava[T](cls: Class[T]) = inject[T](reflect.Manifest.classType(cls), injector)

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    callPermissionsController.globController.activeCall.on(Threading.Ui) {
      case true =>
        //This is needed to drag the user back to the calling activity if they open the app again during a call
        CallingActivity.start(this)
      case _ =>
    }
  }

  def startCall(convId: String, withVideo: Boolean): Unit = {
    callPermissionsController.startCall(ConvId(convId), withVideo)
  }

}
