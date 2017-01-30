package com.denisr.garageshare.service;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.denisr.garageshare.models.Comment;
import com.denisr.garageshare.models.PostUser;
import com.google.firebase.analytics.FirebaseAnalytics;

public class FirebaseAnalyticsManager {
    private static final String EVENT_ON_USER_ADDED = "on_user_added";
    private static final String EVENT_ON_USER_BLOCKED = "on_user_blocked";
    private static final String EVENT_ON_USER_DELETED = "on_user_deleted";

    private final FirebaseAnalytics mFirebaseAnalytics;

    public FirebaseAnalyticsManager(@NonNull Context context) {
        this.mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    public void trackUserAdded(PostUser postUser) {
        mFirebaseAnalytics.logEvent(EVENT_ON_USER_ADDED, getPostUserBundle(postUser));
    }

    private Bundle getPostUserBundle(PostUser postUser) {
        Bundle bundle = new Bundle();

        bundle.putString("uid", postUser.getUid());
        bundle.putString("username", postUser.username);
        bundle.putString("email", postUser.email);
        bundle.putString("user_status", postUser.status.name());

        return bundle;
    }

    public void trackUserBlocked(PostUser postUser) {
        mFirebaseAnalytics.logEvent(EVENT_ON_USER_BLOCKED, getPostUserBundle(postUser));
    }

    public void trackUserDeleted(PostUser postUser) {
        mFirebaseAnalytics.logEvent(EVENT_ON_USER_DELETED, getPostUserBundle(postUser));
    }

    public void trackOnCommentClicked(Comment userComment, boolean isUserHasUsedSpace, boolean isUserUseThisComment) {
        Bundle bundle = new Bundle();

        bundle.putString("uid", userComment.uid);
        bundle.putString("text", userComment.text);
        bundle.putString("status", userComment.status.name());
        bundle.putString("author", userComment.author);
        bundle.putString("time", Long.toString(userComment.time));

        bundle.putString("isUserHasUsedSpace", Boolean.toString(isUserHasUsedSpace));
        bundle.putString("isUserUseThisComment", Boolean.toString(isUserUseThisComment));

        mFirebaseAnalytics.logEvent("on_comment_clicked", bundle);
    }

    public void trackOnCommentChanged(Comment userComment) {
        Bundle bundle = new Bundle();

        bundle.putString("uid", userComment.uid);
        bundle.putString("text", userComment.text);
        bundle.putString("status", userComment.status.name());
        bundle.putString("author", userComment.author);
        bundle.putString("time", Long.toString(userComment.time));

        mFirebaseAnalytics.logEvent("on_comment_updated", bundle);
    }
}
