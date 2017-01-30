package com.denisr.garageshare.presentation;

import android.view.View;

import com.denisr.garageshare.models.Comment;
import com.denisr.garageshare.models.Post;
import com.denisr.garageshare.view.DialogUtil;

public interface PostDetailView {
    void initialiseUserLocationButton(Post post);

    void updateAddNewSpace(boolean visible);

    void updatePost(Post post);

    void initAdapter();

    void failLoadData(String message);

    void hideProgressDialog();

    void showInfoMessage(String itemValue);

    void setBookedNotificationAlarm(String mPostKey, Post currentPost, Comment userComment);

    void hideBoxUsedNotification();

    void notifyItemInserted(int position);

    void updateStatistic(String message);

    void notifyDataSetChanged();

    void notifyItemRemoved(int commentIndex);

    void showEditCommentDialog(String text, DialogUtil.DialogClickListiner listiner);

    void showDeleteCommentDialog(String text, View.OnClickListener onClickListener);
}
