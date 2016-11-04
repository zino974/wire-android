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
package com.waz.zclient.core.controllers.tracking.events.filetransfer;


import android.support.annotation.NonNull;
import com.waz.zclient.core.controllers.tracking.attributes.Attribute;
import com.waz.zclient.core.controllers.tracking.attributes.RangedAttribute;
import com.waz.zclient.core.controllers.tracking.events.Event;
import com.waz.zclient.utils.AssetUtils;

public class InitiatedFileUploadEvent extends Event {

    public InitiatedFileUploadEvent(String fileMimeType, int fileSizeInBytes, String conversationType, boolean isEphemeral, String ephemeralExpiration) {
        attributes.put(Attribute.TYPE, AssetUtils.assetMimeTypeToExtension(fileMimeType));
        attributes.put(Attribute.CONVERSATION_TYPE, conversationType);
        attributes.put(Attribute.FILE_SIZE_BYTES, String.valueOf(fileSizeInBytes));
        rangedAttributes.put(RangedAttribute.FILE_SIZE_MB, AssetUtils.assetSizeToMB(fileSizeInBytes));
        attributes.put(Attribute.IS_EPHEMERAL, String.valueOf(isEphemeral));
        attributes.put(Attribute.EPHEMERAL_EXPIRATION, ephemeralExpiration);
    }

    @NonNull
    @Override
    public String getName() {
        return "file.initiated_file_upload";
    }
}
