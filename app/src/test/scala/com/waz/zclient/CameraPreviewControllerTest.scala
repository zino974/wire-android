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

import android.view.OrientationEventListener
import com.waz.zclient.camera._
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.scalatest.junit.JUnitSuite

@RunWith(classOf[RobolectricTestRunner])
@Config(manifest = Config.NONE)
class CameraPreviewControllerTest extends JUnitSuite {

  @Test
  def testOrientationCalculation(): Unit = {
    assertEquals(Portrait_0, Orientation(0))
    assertEquals(Portrait_0, Orientation(OrientationEventListener.ORIENTATION_UNKNOWN))
    assertEquals(Portrait_0, Orientation(2000))
    assertEquals(Landscape_90, Orientation(90))
    assertEquals(Portrait_180, Orientation(180))
    assertEquals(Landscape_270, Orientation(270))
  }
}
