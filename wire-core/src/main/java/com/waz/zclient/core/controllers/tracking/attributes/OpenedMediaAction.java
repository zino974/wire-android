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
package com.waz.zclient.core.controllers.tracking.attributes;


public enum OpenedMediaAction {
    AUDIO_CALL("audio_call"),
    VIDEO_CALL("video_call"),
    SKETCH("sketch"),
    PING("ping"),
    PHOTO("photo"),
    GIPHY("giphy"),
    FILE("file_transfer"),
    LOCATION("location"),
    EPHEMERAL("ephemeral"),
    VIDEO_MESSAGE("video_message"),
    AUDIO_MESSAGE("audio_message");

    public final String nameString;

    OpenedMediaAction(String nameString) {
        this.nameString = nameString;
    }
}
