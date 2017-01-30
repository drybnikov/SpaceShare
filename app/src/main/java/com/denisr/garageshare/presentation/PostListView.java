package com.denisr.garageshare.presentation;

import android.support.annotation.StringRes;

import com.google.firebase.database.DatabaseReference;

public interface PostListView {
    void showAccessInfo(@StringRes int messageId, String title);

    void openPostDetail(String key);

    void showRequestAccessDialog(DatabaseReference postRef);
}
