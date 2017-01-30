package com.denisr.garageshare.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.text.format.DateUtils;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.denisr.garageshare.BaseEditActivity;
import com.denisr.garageshare.MainActivity;
import com.denisr.garageshare.PostDetailActivity;
import com.denisr.garageshare.PostUsersActivity;
import com.denisr.garageshare.R;
import com.denisr.garageshare.models.Comment;
import com.denisr.garageshare.models.Post;
import com.denisr.garageshare.models.User;

import java.util.Calendar;

public class NotificationBuilder {
    public static final String TAG = "NotificationBuilder";

    private static final long[] VIBRATION_PATTERN = new long[]{100, 200, 50, 200};
    private static final int NOTIF_ID_DOWNLOAD = 0;
    private static final int NOTIF_ID_BOX_USED = 1;
    private static final int NOTIF_ID_ACCESS_REQUESTED = 2;
    private static final int NOTIF_ID_ACCESS_CHANGED = 3;

    private static final int START_DETAIL_ACTIVITY_ACTION = 0;
    private static final int CANCEL_USED_BOX_ACTION = 1;
    private static final int ADD_USER_ACTION = 2;
    private static final int BLOCK_USER_ACTION = 3;


    private static final String KEY_TEXT_REPLY = "key_text_reply";

    public void showUploadFinishedNotification(@NonNull Context context, @Nullable Uri downloadUrl, @Nullable Uri fileUri) {
        // Make Intent to BaseEditActivity
        Intent intent = new Intent(context, BaseEditActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Make PendingIntent for notification
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 /* requestCode */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Set message and icon based on success or failure
        boolean success = downloadUrl != null;
        String message = success ? "Upload finished" : "Upload failed";
        int icon = success ? R.drawable.ic_check_white_24 : R.drawable.ic_error_white_24dp;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(icon)
                .setContentTitle(context.getResources().getString(R.string.app_name))
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        android.app.NotificationManager manager =
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify(NOTIF_ID_DOWNLOAD, builder.build());
    }

    public void showUploadProgressNotification(@NonNull Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_file_upload_white_24dp)
                .setContentTitle(context.getResources().getString(R.string.app_name))
                .setContentText("Uploading...")
                .setProgress(0, 0, true)
                .setOngoing(true)
                .setAutoCancel(false);

        android.app.NotificationManager manager =
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify(NOTIF_ID_DOWNLOAD, builder.build());
    }

    public void showBoxNotification(@NonNull final Context context, final String postKey, final Post post, final Comment userComment) {
        Log.d(TAG, "showBoxNotification");
        int userImageSize = context.getResources().getDimensionPixelSize(R.dimen.space_image_width);
        Glide.with(context)
                .load(post.imageUri)
                .asBitmap()
                .dontAnimate()
                //.thumbnail(0.1f)
                .centerCrop()
                .into(new SimpleTarget<Bitmap>(userImageSize, userImageSize) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                        Log.d(TAG, "onResourceReady bitmap:" + bitmap.getByteCount());
                        showBoxUsedNotification(context, postKey, post, userComment, bitmap, false);
                    }
                });
    }

    public void showBoxUsedNotification(@NonNull Context context, String postKey, Post post, Comment userComment,
                                        @Nullable Bitmap largeIcon, boolean vibrate) {
        Log.d(TAG, "showBoxUsedNotification");
        Intent intent = PostDetailActivity.getStartIntent(context, postKey);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Make PendingIntent for notification
        PendingIntent pendingIntent = PendingIntent.getActivity(context, START_DETAIL_ACTIVITY_ACTION, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Set message and icon based on success or failure
        String usageTime = DateUtils.formatDateTime(context, userComment.time, DateUtils.FORMAT_SHOW_TIME) +
                " (" + DateUtils.formatElapsedTime((System.currentTimeMillis() - userComment.time) / 1000) + ")";
        String appName = context.getString(R.string.app_name);

        String message = context.getString(R.string.notification_box_used_message, userComment.text, usageTime);
        String title = context.getString(R.string.notification_box_used_title, appName, post.title);

        NotificationCompat.Action action = getAction(context, postKey);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_timer_white_24)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .addAction(action)
                .setLargeIcon(largeIcon)
                .setContentIntent(pendingIntent);
        if (!isNightTime() && vibrate) {
            Log.v(TAG, "isNightTime:false");
            builder.setVibrate(VIBRATION_PATTERN).setPriority(Notification.PRIORITY_HIGH);
        }

        final Notification notification = builder.build();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIF_ID_BOX_USED, notification);

        Intent broadcastIntent = UsedBoxBroadcastReceiver.createBroadcastIntent(context, postKey, post, userComment);

        setNextNotificationAlarm(context, broadcastIntent, System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR);
    }

    private NotificationCompat.Action getAction(@NonNull Context context, String postKey) {
        String actionLabel = context.getString(R.string.finish_use_title);
        PendingIntent actionIntent = PendingIntent.getActivity(context,
                CANCEL_USED_BOX_ACTION,
                PostDetailActivity.getStartIntent(context, postKey, PostDetailActivity.ACTION_FINISH_USE_BOX),
                PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel(actionLabel)
                .build();

        return new NotificationCompat.Action.Builder(R.drawable.ic_timer_off_white_24,
                actionLabel, actionIntent)
                .addRemoteInput(remoteInput)
                .build();
    }

    private boolean isNightTime() {
        boolean isNight;
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        isNight = hour < 7 || hour > 19;

        return isNight;
    }

    private void setNextNotificationAlarm(@NonNull Context context, Intent broadcastIntent, long triggerAtMillis) {
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, sender);
    }

    public void setBookedNotificationAlarm(@NonNull final Context context, final String postKey, final Post post, final Comment userComment) {
        Log.d(TAG, "setBookedNotificationAlarm at:" + DateUtils.formatDateTime(context, userComment.time, DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
        hideBoxUsedNotification(context);

        Intent broadcastIntent = UsedBoxBroadcastReceiver.createBroadcastIntent(context, postKey, post, userComment);
        setNextNotificationAlarm(context, broadcastIntent, System.currentTimeMillis() + 30000 /*userComment.time - AlarmManager.INTERVAL_FIFTEEN_MINUTES*/);
    }

    public void hideBoxUsedNotification(@NonNull Context context) {
        Log.d(TAG, "hideBoxUsedNotification");
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        manager.cancel(NOTIF_ID_BOX_USED);

        Intent newIntent = new Intent(context, UsedBoxBroadcastReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        am.cancel(sender);
    }

    public void showBookedNotification(@NonNull Context context, String postKey, Post post, Comment userComment) {
        Log.d(TAG, "showBookedNotification");
        Intent intent = PostDetailActivity.getStartIntent(context, postKey);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, START_DETAIL_ACTIVITY_ACTION, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        String bookedTime = DateUtils.formatDateTime(context, userComment.time, DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME) +
                " - " + DateUtils.formatDateTime(context, userComment.endTime, DateUtils.FORMAT_SHOW_TIME);

        String appName = context.getString(R.string.app_name);
        String message = context.getString(R.string.notification_box_booked_message, userComment.text, bookedTime);
        String title = context.getString(R.string.notification_box_used_title, appName, post.title);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_error_white_24dp)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        if (!isNightTime()) {
            Log.v(TAG, "isNightTime:false");
            builder.setVibrate(VIBRATION_PATTERN).setPriority(Notification.PRIORITY_HIGH);
        }

        final Notification notification = builder.build();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIF_ID_BOX_USED, notification);
    }

    public void showUserRequestNotification(@NonNull Context context, Post post, User user) {
        Log.d(TAG, "showUserRequestNotification");
        Intent intent = PostUsersActivity.getStartIntent(context, post.getKey());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Make PendingIntent for notification
        PendingIntent pendingIntent = PendingIntent.getActivity(context, START_DETAIL_ACTIVITY_ACTION, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Set message and icon based on success or failure
        String title = context.getString(R.string.notification_access_requested_title, post.title);
        String message = context.getString(R.string.notification_access_requested_message, user.username, post.title);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setSmallIcon(R.drawable.ic_lock_open_24)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_group_add_white_24))
                .setContentText(message)
                .setAutoCancel(true)
                .addAction(getBlockAction(context, post.getKey(), user.getUid()))
                .addAction(getAcceptAction(context, post.getKey(), user.getUid()))
                .setContentIntent(pendingIntent);
        if (!isNightTime()) {
            Log.v(TAG, "isNightTime:false");
            builder.setVibrate(VIBRATION_PATTERN).setPriority(Notification.PRIORITY_HIGH);
        }

        final Notification notification = builder.build();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIF_ID_ACCESS_REQUESTED, notification);
    }

    private NotificationCompat.Action getBlockAction(@NonNull Context context, String postKey, String userUid) {
        String actionLabel = context.getString(R.string.menu_block_user);
        Intent intent = PostUsersActivity.getStartIntent(context, postKey, userUid, PostUsersActivity.ACTION_BLOCK_USER);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent actionIntent = PendingIntent.getActivity(context,
                BLOCK_USER_ACTION,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Action(R.drawable.ic_block_24, actionLabel, actionIntent);
    }

    private NotificationCompat.Action getAcceptAction(@NonNull Context context, String postKey, String userUid) {
        String actionLabel = context.getString(R.string.menu_add_user);
        Intent intent = PostUsersActivity.getStartIntent(context, postKey, userUid, PostUsersActivity.ACTION_ADD_USER);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent actionIntent = PendingIntent.getActivity(context,
                ADD_USER_ACTION,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Action(R.drawable.ic_check_white_24, actionLabel, actionIntent);
    }

    public void showAccessChangedNotification(@NonNull final Context context, final Post post, final String message, final int icon) {
        Log.d(TAG, "showAccessChangedNotification");

        int userImageSize = context.getResources().getDimensionPixelSize(R.dimen.space_image_width);
        Glide.with(context)
                .load(post.imageUri)
                .asBitmap()
                .dontAnimate()
                .centerCrop()
                .into(new SimpleTarget<Bitmap>(userImageSize, userImageSize) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                        Log.d(TAG, "onResourceReady bitmap:" + bitmap.getByteCount());
                        showAccessNotification(context, post, message, icon, bitmap);
                    }
                });
    }

    private void showAccessNotification(@NonNull Context context, Post post, String message, int icon, Bitmap largeIcon) {
        Log.d(TAG, "showAccessChangedNotification");

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Make PendingIntent for notification
        PendingIntent pendingIntent = PendingIntent.getActivity(context, START_DETAIL_ACTIVITY_ACTION, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Set message and icon based on success or failure
        String title = context.getString(R.string.notification_access_changed_title, post.title);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(icon)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        if (!isNightTime()) {
            Log.v(TAG, "isNightTime:false");
            builder.setVibrate(VIBRATION_PATTERN).setPriority(Notification.PRIORITY_HIGH);
        }

        final Notification notification = builder.build();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIF_ID_ACCESS_CHANGED, notification);
    }
}
