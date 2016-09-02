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
package com.waz.zclient.controllers.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import com.waz.api.GcmNotificationsList;
import com.waz.api.ImageAsset;
import com.waz.api.KindOfCall;
import com.waz.api.LoadHandle;
import com.waz.api.VoiceChannelState;
import com.waz.zclient.R;
import com.waz.zclient.controllers.userpreferences.UserPreferencesController;
import com.waz.zclient.controllers.vibrator.VibratorController;
import com.waz.zclient.utils.IntentUtils;
import com.waz.zclient.utils.RingtoneUtils;
import hugo.weaving.DebugLog;
import timber.log.Timber;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class NotificationsController implements INotificationsController {

    public static final int ZETA_MESSAGE_NOTIFICATION_ID = 1339272;
    public static final int ZETA_CALL_INCOMING_NOTIFICATION_ID = 1339273;
    public static final int ZETA_CALL_ONGOING_NOTIFICATION_ID = 1339276;
    public static final int ZETA_SAVE_IMAGE_NOTIFICATION_ID = 1339274;
    private Context context;
    private NotificationManager notificationManager;
    private SharedPreferences sharedPreferences;
    private LoadHandle imageSavedBitmapLoadHandle;

    public NotificationsController(Context context) {
        this.context = context;
        notificationManager = ((NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE));
        sharedPreferences = this.context.getApplicationContext()
                                        .getSharedPreferences(UserPreferencesController.USER_PREFS_TAG,
                                                              Context.MODE_PRIVATE);
    }

    @Override
    @DebugLog
    public void updateGcmNotification(GcmNotificationsList gcmNotificationsList) {
        Collection<GcmNotification> gcmNotifications = gcmNotificationsList.getNotifications();
        if (gcmNotifications == null || gcmNotifications.size() < 1) {
            notificationManager.cancel(ZETA_MESSAGE_NOTIFICATION_ID);
            return;
        }

        final Notification notification;
        if (gcmNotifications.size() == 1) {
            notification = getSingleMesssageNotification(gcmNotifications);
        } else {
            // TODO: Would be nice to show the {@link #getSingleMesssageNotification} as a HeadsUpNotification
            notification = getMultipleMessagesNotification(gcmNotifications);
        }

        if (notification == null) {
            return;
        }

        notification.priority = Notification.PRIORITY_HIGH;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.deleteIntent = gcmNotificationsList.getClearNotificationsIntent();

        attachNotificationLed(notification);
        attachNotificationSound(gcmNotifications, notification);

        notificationManager.notify(ZETA_MESSAGE_NOTIFICATION_ID, notification);
    }

    @Override
    @DebugLog
    public void updateOngoingCallNotification(ActiveChannel ongoing, ActiveChannel incoming, boolean uiActive) {
        Timber.i("Call notification: uiActive=%b, incoming=%s, ongoing=%s", uiActive, incoming, ongoing);
        if (incoming == null) {
            notificationManager.cancel(ZETA_CALL_INCOMING_NOTIFICATION_ID);
        } else {
            showCallNotification(incoming, ZETA_CALL_INCOMING_NOTIFICATION_ID, false);
        }

        if (ongoing == null) {
            notificationManager.cancel(ZETA_CALL_ONGOING_NOTIFICATION_ID);
        } else {
            showCallNotification(ongoing, ZETA_CALL_ONGOING_NOTIFICATION_ID, true);
        }
    }

    private void showCallNotification(ActiveChannel activeChannel, int id, boolean noClear) {
        Notification notification = getCallNotification(activeChannel);
        notification.priority = Notification.PRIORITY_MAX;
        if (noClear) {
            notification.flags |= Notification.FLAG_NO_CLEAR;
        }
        notificationManager.notify(id, notification);
    }

    //////////////////////////////////////////////////////
    //
    // Android Notification Magic
    //
    //////////////////////////////////////////////////////

    private Notification getCallNotification(ActiveChannel activeChannel) {
        String message = getCallState(activeChannel.getState(), activeChannel.isVideoCall());
        String title = getCallTitle(activeChannel);

        PendingIntent leavePendingIntent = activeChannel.getLeaveActionIntent();
        PendingIntent silencePendingIntent = activeChannel.getSilenceActionIntent();
        PendingIntent joinPendingIntent = activeChannel.getJoinActionIntent();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.ic_menu_logo)
                                                                                    .setLargeIcon(activeChannel.getPicture())
                                                                                    .setContentTitle(title)
                                                                                    .setContentText(message)
                                                                                    .setContentIntent(IntentUtils.getNotificationAppLaunchIntent(context));
        // TODO Use our own icons for the notification actions, pending design
        if (silencePendingIntent != null) {
            builder.addAction(R.drawable.ic_menu_silence_call_w,
                              context.getString(R.string.system_notification__silence_call),
                              silencePendingIntent);
            builder.setDeleteIntent(silencePendingIntent);
        } else if (leavePendingIntent != null) {
            builder.addAction(R.drawable.ic_menu_end_call_w,
                              context.getString(R.string.system_notification__leave_call),
                              leavePendingIntent);
        }

        if (joinPendingIntent != null) {
            builder.addAction(R.drawable.ic_menu_join_call_w,
                              context.getString(R.string.system_notification__join_call),
                              joinPendingIntent);
        }

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(activeChannel.getConversationName());
        bigTextStyle.bigText(message);
        builder.setStyle(bigTextStyle);
        builder.setCategory(NotificationCompat.CATEGORY_CALL);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        return builder.build();
    }

    private Notification getSingleMesssageNotification(Collection<GcmNotification> gcmNotifications) {
        GcmNotification gcmNotification = null;
        Iterator<GcmNotification> iterator = gcmNotifications.iterator();
        while (iterator.hasNext()) {
            GcmNotification notification = iterator.next();
            if (!iterator.hasNext()) {
                gcmNotification = notification;
            }
        }

        if (gcmNotification == null) {
            return null;
        }

        final SpannableString spannableString = getMessage(gcmNotification, false, true, true);
        if (spannableString == null) {
            return null;
        }
        final String title = getMessageTitle(gcmNotification);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        final String conversationId = gcmNotification.getConversationId();
        final int requestBase = (int) System.currentTimeMillis();
        builder.setSmallIcon(R.drawable.ic_menu_logo)
               .setLargeIcon(getAppIcon())
               .setContentTitle(title)
               .setContentText(spannableString)
               .setContentIntent(IntentUtils.getNotificationAppLaunchIntent(context, conversationId, requestBase));
        if (gcmNotification.getType() != GcmNotification.Type.CONNECT_REQUEST) {
            builder.addAction(R.drawable.ic_action_call,
                              context.getString(R.string.notification__action__call),
                              IntentUtils.getNotificationCallIntent(context, conversationId, requestBase + 1))
                   .addAction(R.drawable.ic_action_reply,
                              context.getString(R.string.notification__action__reply),
                              IntentUtils.getNotificationReplyIntent(context, conversationId, requestBase + 2));
        }

        final NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(title);
        bigTextStyle.bigText(spannableString);
        builder.setStyle(bigTextStyle);
        builder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        if (VibratorController.isEnabledInPreferences(context)) {
            builder.setVibrate(VibratorController.resolveResource(context.getResources(), R.array.new_message_gcm));
        }
        return builder.build();
    }

    private Notification getMultipleMessagesNotification(Collection<GcmNotification> gcmNotifications) {
        final Set<String> conversationIds = new HashSet<>();
        final Set<String> users = new HashSet<>();
        for (GcmNotification notification : gcmNotifications) {
            conversationIds.add(notification.getConversationId());
            users.add(notification.getUserId());
        }
        final List<SpannableString> items = new LinkedList<>();
        final boolean singleConversation = conversationIds.size() == 1;
        final boolean singleUser = users.size() == 1 && singleConversation;
        for (GcmNotification notification : gcmNotifications) {
            SpannableString spannableString = getMessage(notification,
                                                         true,
                                                         singleConversation,
                                                         singleUser);
            if (spannableString != null) {
                items.add(0, spannableString);
            }
        }

        final GcmNotification firstNotification = gcmNotifications.iterator().next();
        final String conversationDescription;
        final int headerResource;
        if (singleConversation) {
            if (firstNotification.isGroupConversation()) {
                conversationDescription = firstNotification.getConversationName();
                headerResource = R.plurals.notification__new_messages_groups;
            } else {
                conversationDescription = firstNotification.getUserName();
                headerResource = R.plurals.notification__new_messages;
            }
        } else {
            conversationDescription = String.valueOf(conversationIds.size());
            headerResource = R.plurals.notification__new_messages__multiple;
        }
        final String title = context.getResources().getQuantityString(headerResource,
                                                                      gcmNotifications.size(),
                                                                      gcmNotifications.size(),
                                                                      conversationDescription);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.ic_menu_logo)
                                                                                          .setLargeIcon(getAppIcon())
                                                                                          .setNumber(gcmNotifications.size());
        final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(title);
        builder.setContentTitle(title)
               .setStyle(inboxStyle)
               .setCategory(NotificationCompat.CATEGORY_MESSAGE)
               .setPriority(NotificationCompat.PRIORITY_HIGH);
        if (VibratorController.isEnabledInPreferences(context)) {
            builder.setVibrate(VibratorController.resolveResource(context.getResources(), R.array.new_message_gcm));
        }

        if (singleConversation) {
            final int requestBase = (int) System.currentTimeMillis();
            final String conversationId = conversationIds.iterator().next();
            builder.setContentIntent(IntentUtils.getNotificationAppLaunchIntent(context, conversationId, requestBase));
            builder.addAction(R.drawable.ic_action_call, context.getString(R.string.notification__action__call), IntentUtils.getNotificationCallIntent(context, conversationId, requestBase + 1));
            builder.addAction(R.drawable.ic_action_reply, context.getString(R.string.notification__action__reply), IntentUtils.getNotificationReplyIntent(context, conversationId, requestBase + 2));
        } else {
            builder.setContentIntent(IntentUtils.getNotificationAppLaunchIntent(context));
        }

        SpannableString line;
        for (int i = 0, length = Math.min(items.size(), 5); i < length; i++) {
            line = items.get(i);
            if (i == 0) {
                builder.setContentText(line);
            }
            inboxStyle.addLine(line);
        }

        return builder.build();
    }

    private String getMessageTitle(GcmNotification gcmNotification) {
        boolean groupConversation = gcmNotification.isGroupConversation();
        final String username = gcmNotification.getUserName();
        if (groupConversation) {
            String conversationName = gcmNotification.getConversationName();
            if (TextUtils.isEmpty(conversationName)) {
                conversationName = context.getString(R.string.notification__message__group__default_conversation_name);
            }
            return context.getString(R.string.notification__message__group__prefix__other, username, conversationName);
        } else {
            return username;
        }
    }

    private SpannableString getMessage(GcmNotification gcmNotification,
                                       boolean multiple,
                                       boolean singleConversationInBatch,
                                       boolean singleUserInBatch) {

        boolean groupConversation = gcmNotification.isGroupConversation();
        String header;
        String body;

        // Replace all linebreaks with a space
        String message = gcmNotification.getMessage().replaceAll("\\r\\n|\\r|\\n", " ");
        switch (gcmNotification.getType()) {
            case TEXT:
            case CONNECT_REQUEST: //TODO i18n this for connect requests.
                header = getDefaultNotificationMessageLineHeader(gcmNotification, multiple, true, singleConversationInBatch, singleUserInBatch);
                body = message;
                break;

            case MISSED_CALL:
                header = getDefaultNotificationMessageLineHeader(gcmNotification, multiple, false, singleConversationInBatch, false);
                body = context.getString(R.string.notification__message__one_to_one__wanted_to_talk);
                break;

            case KNOCK:
                header = getDefaultNotificationMessageLineHeader(gcmNotification,
                                                                 multiple,
                                                                 false,
                                                                 singleConversationInBatch,
                                                                 false);
                if (groupConversation) {
                    body = context.getString(R.string.notification__message__group__pinged);
                } else {
                    body = context.getString(R.string.notification__message__one_to_one__pinged);
                }
                break;
            case ANY_ASSET:
                header = getDefaultNotificationMessageLineHeader(gcmNotification, multiple, false, singleConversationInBatch, false);
                if (groupConversation) {
                    body = context.getString(R.string.notification__message__group__shared_file);
                } else {
                    body = context.getString(R.string.notification__message__one_to_one__shared_file);
                }
                break;
            case ASSET:
                header = getDefaultNotificationMessageLineHeader(gcmNotification, multiple, false, singleConversationInBatch, false);
                if (groupConversation) {
                    body = context.getString(R.string.notification__message__group__shared_picture);
                } else {
                    body = context.getString(R.string.notification__message__one_to_one__shared_picture);
                }
                break;
            case VIDEO_ASSET:
                header = getDefaultNotificationMessageLineHeader(gcmNotification, multiple, false, singleConversationInBatch, false);
                if (groupConversation) {
                    body = context.getString(R.string.notification__message__group__shared_video);
                } else {
                    body = context.getString(R.string.notification__message__one_to_one__shared_video);
                }
                break;
            case AUDIO_ASSET:
                header = getDefaultNotificationMessageLineHeader(gcmNotification, multiple, false, singleConversationInBatch, false);
                if (groupConversation) {
                    body = context.getString(R.string.notification__message__group__shared_audio);
                } else {
                    body = context.getString(R.string.notification__message__one_to_one__shared_audio);
                }
                break;
            case LOCATION:
                header = getDefaultNotificationMessageLineHeader(gcmNotification, multiple, false, singleConversationInBatch, false);
                if (groupConversation) {
                    body = context.getString(R.string.notification__message__group__shared_location);
                } else {
                    body = context.getString(R.string.notification__message__one_to_one__shared_location);
                }
                break;
            case RENAME:
                header = getDefaultNotificationMessageLineHeader(gcmNotification, multiple, false, singleConversationInBatch, false);
                body = context.getString(R.string.notification__message__group__renamed_conversation,
                                         message);
                break;

            case MEMBER_LEAVE:
                header = getDefaultNotificationMessageLineHeader(gcmNotification, multiple, false, singleConversationInBatch, false);
                body = context.getString(R.string.notification__message__group__remove);
                break;

            case MEMBER_JOIN:
                header = getDefaultNotificationMessageLineHeader(gcmNotification, multiple, false, singleConversationInBatch, false);
                body = context.getString(R.string.notification__message__group__add);
                break;

            case CONNECT_ACCEPTED:
                header = getConnectAcceptedHeader(gcmNotification, multiple);
                if (multiple) {
                    body = context.getString(R.string.notification__message__multiple__accept_request);
                } else {
                    body = context.getString(R.string.notification__message__single__accept_request);
                }
                break;

            case LIKE:
                header = getDefaultNotificationMessageLineHeader(gcmNotification, multiple, false, singleConversationInBatch, false);
                if (multiple) {
                    body = context.getString(R.string.notification__message__group__liked);
                } else {
                    body = context.getString(R.string.notification__message__one_to_one__liked);
                }
                break;

            default:
                return null;

        }
        return getMessageSpannable(header, body);
    }

    private String getConnectAcceptedHeader(GcmNotification gcmNotification, boolean multiple) {
        if (multiple) {
            return context.getString(R.string.notification__message__name__prefix__other,
                                     gcmNotification.getConversationName());
        }
        return "";
    }

    private String getDefaultNotificationMessageLineHeader(GcmNotification gcmNotification,
                                                           boolean multiple,
                                                           boolean textPrefix,
                                                           boolean singleConversationInBatch,
                                                           boolean singleUser) {
        boolean currentNotificationForGroupConversation = gcmNotification.isGroupConversation();
        String conversationName = gcmNotification.getConversationName();
        if (TextUtils.isEmpty(conversationName)) {
            conversationName = context.getString(R.string.notification__message__group__default_conversation_name);
        }
        int prefixId;
        if (multiple) {
            if (currentNotificationForGroupConversation &&
                !singleConversationInBatch) {
                prefixId = textPrefix ? R.string.notification__message__group__prefix__text
                                      : R.string.notification__message__group__prefix__other;
            } else if (!singleUser || currentNotificationForGroupConversation) {
                prefixId = textPrefix ? R.string.notification__message__name__prefix__text
                                      : R.string.notification__message__name__prefix__other;
            } else {
                return "";
            }
        } else {
            return "";
        }
        return context.getString(prefixId, gcmNotification.getUserName(), conversationName);
    }

    @TargetApi(21)
    private SpannableString getMessageSpannable(String header, String body) {
        SpannableString messageSpannable = new SpannableString(header + body);
        final int textAppearance;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textAppearance = android.R.style.TextAppearance_Material_Notification_Title;
        } else {
            textAppearance = android.R.style.TextAppearance_StatusBar_EventContent_Title;
        }
        final TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan(context, textAppearance);
        messageSpannable.setSpan(new ForegroundColorSpan(textAppearanceSpan.getTextColor().getDefaultColor()),
                                 0,
                                 header.length(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return messageSpannable;
    }

    private void attachNotificationLed(Notification notification) {
        int color = sharedPreferences.getInt(UserPreferencesController.USER_PREFS_LAST_ACCENT_COLOR, -1);
        if (color == -1) {
            color = context.getResources().getColor(R.color.accent_default);
        }
        notification.ledARGB = color;
        notification.ledOnMS = context.getResources().getInteger(R.integer.notifications__system__led_on);
        notification.ledOffMS = context.getResources().getInteger(R.integer.notifications__system__led_off);
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
    }

    private void attachNotificationSound(Collection<GcmNotification> gcmNotifications, Notification notification) {
        String soundSetting = sharedPreferences.getString(context.getString(R.string.pref_options_sounds_key),
                                                          context.getString(R.string.pref_options_sounds_default));
        Uri sound;
        if (context.getString(R.string.pref_sound_value_none).equals(soundSetting)) {
            sound = null;
        } else if (context.getString(R.string.pref_sound_value_some).equals(soundSetting) && gcmNotifications.size() > 1) {
            sound = null;
        } else {
            GcmNotification lastNotification = getLastGcmNotification(gcmNotifications);
            sound = getMessageSoundUri(lastNotification);
        }
        notification.sound = sound;
    }

    private GcmNotification getLastGcmNotification(Collection<GcmNotification> gcmNotifications) {
        if (gcmNotifications == null || gcmNotifications.size() == 0) {
            return null;
        }
        GcmNotification last = null;
        for (GcmNotification gcmNotification : gcmNotifications) {
            last = gcmNotification;
        }
        return last;
    }

    private Uri getMessageSoundUri(GcmNotification notification) {
        if (context == null) {
            return null;
        }
        String value;
        switch (notification.getType()) {
            case ASSET:
            case ANY_ASSET:
            case VIDEO_ASSET:
            case AUDIO_ASSET:
            case LOCATION:
            case TEXT:
            case CONNECT_ACCEPTED:
            case CONNECT_REQUEST:
            case RENAME:
            case LIKE:
                value = sharedPreferences.getString(context.getString(R.string.pref_options_ringtones_text_key), null);
                return getSelectedSoundUri(value, R.raw.new_message_gcm);
            case KNOCK:
                value = sharedPreferences.getString(context.getString(R.string.pref_options_ringtones_ping_key), null);
                if (notification.isHotKnock()) {
                    return getSelectedSoundUri(value, R.raw.ping_from_them, R.raw.hotping_from_them);
                } else {
                    return getSelectedSoundUri(value, R.raw.ping_from_them);
                }
            default:
                return null;
        }
    }

    private Uri getSelectedSoundUri(String value, @RawRes int defaultResId) {
        return getSelectedSoundUri(value, defaultResId, defaultResId);
    }

    private Uri getSelectedSoundUri(String value, @RawRes int preferenceDefault, @RawRes int returnDefault) {
        if (!TextUtils.isEmpty(value) && !RingtoneUtils.isDefaultValue(context, value, preferenceDefault)) {
            return Uri.parse(value);
        } else {
            return RingtoneUtils.getUriForRawId(context, returnDefault);
        }
    }

    private String getCallTitle(ActiveChannel activeChannel) {
        if (activeChannel == null) {
            return null;
        }
        if (activeChannel.getKindOfCall() == KindOfCall.GROUP) {
            return context.getString(R.string.system_notification__group_call_title,
                                     activeChannel.getCallerName(),
                                     activeChannel.getConversationName());
        } else {
            return activeChannel.getConversationName();
        }
    }

    private String getCallState(VoiceChannelState state, boolean isVideoCall) {
        switch (state) {
            case SELF_CALLING:
            case SELF_JOINING:
                return isVideoCall ? context.getString(R.string.system_notification__outgoing_video)
                                   : context.getString(R.string.system_notification__outgoing);
            case OTHER_CALLING:
                return isVideoCall ? context.getString(R.string.system_notification__incoming_video)
                                   : context.getString(R.string.system_notification__incoming);
            case SELF_CONNECTED:
                return context.getString(R.string.system_notification__ongoing);
            default:
                return "";
        }
    }

    private Bitmap getAppIcon() {
        try {
            Drawable icon = context.getPackageManager().getApplicationIcon(context.getPackageName());
            if (icon instanceof BitmapDrawable) {
                return ((BitmapDrawable) icon).getBitmap();
            }
            Bitmap bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(),
                                                icon.getIntrinsicHeight(),
                                                Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            icon.draw(canvas);
            return bitmap;
        } catch (PackageManager.NameNotFoundException e) {
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_wire);
        }
    }

    @Override
    public void tearDown() {
        context = null;
        notificationManager = null;
        sharedPreferences = null;

        if (imageSavedBitmapLoadHandle != null) {
            imageSavedBitmapLoadHandle.cancel();
        }
        imageSavedBitmapLoadHandle = null;
    }

    @Override
    public void showImageSavedNotification(@NonNull final ImageAsset image, @NonNull final Uri uri) {
        if (context == null ||
            notificationManager == null) {
            return;
        }

        if (imageSavedBitmapLoadHandle != null) {
            imageSavedBitmapLoadHandle.cancel();
        }

        final ImageAsset.BitmapCallback bitmapCallback = new ImageAsset.BitmapCallback() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, boolean isPreview) {
                if (imageSavedBitmapLoadHandle != null) {
                    imageSavedBitmapLoadHandle.cancel();
                    imageSavedBitmapLoadHandle = null;
                }
                if (!isPreview) {
                    showSavedImageNotification(bitmap, uri);
                }
                showSavedImageNotification(bitmap, uri);
            }

            @Override
            public void onBitmapLoadingFailed() {
                showSavedImageNotification(null, uri);
                if (imageSavedBitmapLoadHandle != null) {
                    imageSavedBitmapLoadHandle.cancel();
                    imageSavedBitmapLoadHandle = null;
                }

            }
        };
        imageSavedBitmapLoadHandle = image.getSingleBitmap(context.getResources().getDimensionPixelSize(R.dimen.notification__image_saving__image_width),
                                                           bitmapCallback);
    }

    private void showSavedImageNotification(Bitmap bitmap, @NonNull Uri uri) {
        if (context == null ||
            notificationManager == null) {
            return;
        }

        final String summaryText = context.getString(R.string.notification__image_saving__content__subtitle);
        final String notificationTitle = context.getString(R.string.notification__image_saving__content__title);

        final NotificationCompat.BigPictureStyle notificationStyle = new NotificationCompat.BigPictureStyle()
            .bigPicture(bitmap)
            .setSummaryText(summaryText);

        final Notification notification = new NotificationCompat.Builder(context)
            .setContentTitle(notificationTitle)
            .setContentText(summaryText)
            .setSmallIcon(R.drawable.ic_menu_save_image_gallery)
            .setLargeIcon(bitmap)
            .setStyle(notificationStyle)
            .setContentIntent(getGalleryIntent(uri))
            .addAction(R.drawable.ic_menu_share,
                       context.getString(R.string.notification__image_saving__action__share),
                       getPendingShareIntent(uri))
            .setLocalOnly(true)
            .setAutoCancel(true)
            .build();

        notificationManager.notify(ZETA_SAVE_IMAGE_NOTIFICATION_ID, notification);
    }

    private PendingIntent getGalleryIntent(Uri uri) {
        // TODO: AN-2276 - Replace with ShareCompat.IntentBuilder
        Intent galleryIntent = new Intent(Intent.ACTION_VIEW);
        galleryIntent.setDataAndTypeAndNormalize(uri, "image/*");
        galleryIntent.setClipData(new ClipData(null, new String[] {"image/*"}, new ClipData.Item(uri)));
        galleryIntent.putExtra(Intent.EXTRA_STREAM, uri);
        return PendingIntent.getActivity(context,
                                         0,
                                         galleryIntent,
                                         0);
    }

    private PendingIntent getPendingShareIntent(Uri uri) {
        Intent shareIntent = new Intent(context, ShareSavedImageActivity.class);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(IntentUtils.EXTRA_LAUNCH_FROM_SAVE_IMAGE_NOTIFICATION, true);
        return PendingIntent.getActivity(context,
                                         0,
                                         shareIntent,
                                         0);
    }

    @Override
    public void dismissImageSavedNotification(Uri uri) {
        if (notificationManager == null) {
            return;
        }
        notificationManager.cancel(ZETA_SAVE_IMAGE_NOTIFICATION_ID);
    }
}
