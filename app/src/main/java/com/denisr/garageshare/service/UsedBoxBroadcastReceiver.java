package com.denisr.garageshare.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.denisr.garageshare.models.Comment;
import com.denisr.garageshare.models.CommentStatus;
import com.denisr.garageshare.models.Post;

public class UsedBoxBroadcastReceiver extends BroadcastReceiver {
    public static final String EXTRA_POST_KEY = "postKey";
    public static final String EXTRA_POST_TITLE = "postTitle";
    public static final String EXTRA_COMMENT_TIME = "commentTime";
    public static final String EXTRA_END_COMMENT_TIME = "endCommentTime";
    public static final String EXTRA_COMMENT_TEXT = "commentText";
    public static final String EXTRA_COMMENT_STATUS = "commentStatus";
    private static final String TAG = "UsedBoxReceiver";

    public static Intent createBroadcastIntent(Context context, String postKey, Post post, Comment userComment) {
        Intent newIntent = new Intent(context, UsedBoxBroadcastReceiver.class);
        newIntent.putExtra(EXTRA_POST_KEY, postKey);
        newIntent.putExtra(EXTRA_POST_TITLE, post.title);
        newIntent.putExtra(EXTRA_COMMENT_TIME, userComment.time);
        newIntent.putExtra(EXTRA_END_COMMENT_TIME, userComment.endTime);
        newIntent.putExtra(EXTRA_COMMENT_TEXT, userComment.text);
        newIntent.putExtra(EXTRA_COMMENT_STATUS, userComment.status);

        return newIntent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String postKey = intent.getStringExtra(EXTRA_POST_KEY);
        Log.d(TAG, "onReceive :" + intent.getExtras());

        if (postKey != null) {
            Post post = new Post();
            post.title = intent.getStringExtra(EXTRA_POST_TITLE);

            Comment comment = new Comment();
            comment.time = intent.getLongExtra(EXTRA_COMMENT_TIME, 0);
            comment.endTime = intent.getLongExtra(EXTRA_END_COMMENT_TIME, 0);
            comment.text = intent.getStringExtra(EXTRA_COMMENT_TEXT);
            comment.status = (CommentStatus) intent.getSerializableExtra(EXTRA_COMMENT_STATUS);

            NotificationBuilder notificationBuilder = new NotificationBuilder();
            if (comment.status == CommentStatus.USED) {
                notificationBuilder.showBoxUsedNotification(context, postKey, post, comment, null, true);
            } else if (comment.status == CommentStatus.BOOKED) {
                notificationBuilder.showBookedNotification(context, postKey, post, comment);
            }
        }
    }
}
