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
package com.waz.zclient.pages.main.profile.preferences.dialogs;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.RawRes;
import net.xpece.android.support.preference.XpRingtonePreferenceDialogFragment;
import timber.log.Timber;

import java.lang.reflect.Field;

@SuppressLint("com.waz.BaseDialogFragment")
public class WireRingtonePreferenceDialogFragment extends XpRingtonePreferenceDialogFragment {

    public static final String EXTRA_DEFAULT = "EXTRA_DEFAULT";

    public static WireRingtonePreferenceDialogFragment newInstance(String key, @RawRes int rawId) {
        WireRingtonePreferenceDialogFragment fragment = new WireRingtonePreferenceDialogFragment();
        Bundle b = new Bundle(2);
        b.putString("key", key);
        b.putInt(EXTRA_DEFAULT, rawId);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int rawId = getArguments().getInt(EXTRA_DEFAULT);
        if (rawId == 0) {
            return;
        }
        final String value = getPreference().getSharedPreferences().getString(getPreference().getKey(), "");
        try {
            final Uri defaultUri = Uri.parse("android.resource://" + getContext().getPackageName() + "/" + rawId);
            Field fl = XpRingtonePreferenceDialogFragment.class.getDeclaredField("mUriForDefaultItem");
            fl.setAccessible(true);
            fl.set(this, defaultUri);
            fl.setAccessible(false);

            if (defaultUri.compareTo(Uri.parse(value)) == 0) {
                // We selected the default value
                fl = XpRingtonePreferenceDialogFragment.class.getDeclaredField("mClickedPos");
                fl.setAccessible(true);
                fl.setInt(this, 0);
                fl.setAccessible(false);
            }
        } catch (Exception e) {
            Timber.e(e, "Could not set default item");
        }
    }
}
