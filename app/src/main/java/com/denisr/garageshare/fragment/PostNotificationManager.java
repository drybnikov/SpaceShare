package com.denisr.garageshare.fragment;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.util.Log;

import com.denisr.garageshare.R;
import com.denisr.garageshare.models.Post;
import com.denisr.garageshare.models.User;
import com.denisr.garageshare.models.UserStatus;
import com.denisr.garageshare.service.NotificationBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class PostNotificationManager {
    private static final String TAG = "PostNotificationManager";

    private final WeakReference<Context> weakContext;
    private final NotificationBuilder notificationBuilder;
    private final String mUid;

    private Map<String, UserStatus> postUsers = new HashMap<>();
    private Map<String, UserStatus> userPosts = new HashMap<>();//PostKey/current UserStatus for this post

    public PostNotificationManager(Activity activity, String mUid) {
        this.weakContext = new WeakReference<>(activity.getApplicationContext());
        this.mUid = mUid;
        notificationBuilder = new NotificationBuilder();

    }

    public void onChildAdded(Post post) {
        if (post.uid.equals(mUid)) {
            Log.d(TAG, "onChildAdded post.users:" + post.users.size() + ", current postUsers:" + postUsers.size());
            postUsers = post.users;
        } else if (post.users.containsKey(mUid)) {
            userPosts.put(post.getKey(), post.users.get(mUid));
        }
    }

    public void onPostChanged(Post post) {
        //We want update only current user posts
        Log.d(TAG, "onPostChanged post.users:" + post.users.size() + ", current postUsers:" + postUsers.size());

        if (post.uid.equals(mUid)) {
            for (String userKey : post.users.keySet()) {
                UserStatus currentStatus = postUsers.get(userKey);
                UserStatus newStatus = post.users.get(userKey);
                Log.d(TAG, "onPostChanged userKey:" + userKey + ",currentStatus:" + currentStatus + ",newStatus:" + newStatus);

                if (currentStatus == null && newStatus == UserStatus.REQUESTED) {
                    showNewUserRequestNotification(post, userKey);
                }
            }

            postUsers = post.users;
        }

        Log.d(TAG, "onUserPostChanged userPosts:" + userPosts.toString() + ", post updated:" + post.getKey());

        if (userPosts.containsKey(post.getKey()) || isCurrentUserPresentInPost(post)) {
            UserStatus currentStatus = userPosts.get(post.getKey());
            UserStatus newStatus = post.users.get(mUid);

            Log.d(TAG, "onUserPostChanged post.key:" + post.getKey() + "use:" + mUid + ",currentStatus:" + currentStatus + ",newStatus:" + newStatus);
            if (currentStatus == null) {
                return;
            }

            String message = null;
            int icon = R.drawable.ic_delete_forever_white_24;
            if (newStatus == null) {
                message = getString(R.string.notification_access_deleted);
                userPosts.remove(post.getKey());
            } else if (newStatus == UserStatus.ALLOWED && currentStatus != UserStatus.ALLOWED) {
                message = getString(R.string.notification_access_granted);
                icon = R.drawable.ic_check_white_24;
                userPosts.put(post.getKey(), newStatus);
            } else if (newStatus == UserStatus.BLOCKED && currentStatus != UserStatus.BLOCKED) {
                message = getString(R.string.notification_access_blocked);
                icon = R.drawable.ic_block_24;
                userPosts.put(post.getKey(), newStatus);
            } else {
                userPosts.put(post.getKey(), newStatus);
            }

            Context context = weakContext.get();
            if (context != null && !TextUtils.isEmpty(message)) {
                notificationBuilder.showAccessChangedNotification(context, post, message, icon);
            }
        }
    }

    private void showNewUserRequestNotification(final Post post, final String userKey) {
        Log.d(TAG, "showNewUserRequestNotification post:" + post + ",user:" + userKey);

        FirebaseDatabase.getInstance().getReference().child("users").child(userKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User newUser = dataSnapshot.getValue(User.class);
                        newUser.setUid(userKey);
                        Context context = weakContext.get();
                        if (context != null) {
                            notificationBuilder.showUserRequestNotification(context, post, newUser);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "showNewUserRequestNotification loadUser:onCancelled", databaseError.toException());
                    }
                });
    }

    private boolean isCurrentUserPresentInPost(Post post) {
        return post.users.containsKey(mUid);
    }

    private String getString(@StringRes int resId) {
        Context context = weakContext.get();
        if (context != null) {
            return context.getString(resId);
        }

        return "";
    }
}
