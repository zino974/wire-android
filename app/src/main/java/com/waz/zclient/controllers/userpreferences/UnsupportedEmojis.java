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
package com.waz.zclient.controllers.userpreferences;

import com.waz.zclient.utils.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashSet;
import java.util.Set;

public class UnsupportedEmojis {

    private final Set<String> unsupportedEmojis;

    public UnsupportedEmojis(String json) {
        unsupportedEmojis = new HashSet<>();
        if (!StringUtils.isBlank(json)) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    unsupportedEmojis.add(jsonArray.getString(i));
                }
            } catch (JSONException e) {
                // ignore
            }
        }
    }

    public boolean addUnsupportedEmoji(String emoji) {
        return unsupportedEmojis.add(emoji);
    }

    public String getJson() {
        JSONArray array = new JSONArray();
        for (String emoji : unsupportedEmojis) {
            array.put(emoji);
        }
        return array.toString();
    }

    public Set<String> getUnsupportedEmojis() {
        return unsupportedEmojis;
    }
}
