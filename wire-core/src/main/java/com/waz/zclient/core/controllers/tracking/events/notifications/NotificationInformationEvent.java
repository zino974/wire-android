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
package com.waz.zclient.core.controllers.tracking.events.notifications;

import android.support.annotation.NonNull;
import com.waz.service.push.PushTrackingService;
import com.waz.zclient.core.controllers.tracking.attributes.Attribute;
import com.waz.zclient.core.controllers.tracking.events.Event;
import org.threeten.bp.temporal.ChronoField;

public class NotificationInformationEvent extends Event {

    public NotificationInformationEvent(PushTrackingService.NotificationsEvent ev) {
        attributes.put(Attribute.DAY, Integer.toString(ev.time().get(ChronoField.DAY_OF_MONTH)));
        attributes.put(Attribute.MONTH, Integer.toString(ev.time().get(ChronoField.MONTH_OF_YEAR)));
        attributes.put(Attribute.YEAR, Integer.toString(ev.time().get(ChronoField.YEAR)));

        attributes.put(Attribute.GCM_SUCCESS, Integer.toString(ev.successfulGcmNotifs()));
        attributes.put(Attribute.GCM_FAILED, Integer.toString(ev.failedGcmNotifs()));
        attributes.put(Attribute.GCM_RE_REGISTER, Integer.toString(ev.registrationRetries()));

        attributes.put(Attribute.TOTAL_PINGS, Integer.toString(ev.totalPings()));
        attributes.put(Attribute.RECEIVED_PONGS, Integer.toString(ev.receivedPongs()));
        attributes.put(Attribute.PING_INTERVAL, Long.toString(ev.pingInterval().toMillis()));
    }

    @NonNull
    @Override
    public String getName() {
        return "notification.information";
    }
}
