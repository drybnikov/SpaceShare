package com.denisr.garageshare.models;

import com.google.firebase.database.IgnoreExtraProperties;

// [START comment_class]
@IgnoreExtraProperties
public class Comment {

    public String uid;
    public String author;
    public String text;
    public String userImage;
    public CommentStatus status = CommentStatus.FREE;
    public long time;
    public long endTime;

    public Comment() {
        // Default constructor required for calls to DataSnapshot.getValue(Comment.class)
    }

    public Comment(String uid, String author, String text, CommentStatus status) {
        this.uid = uid;
        this.author = author;
        this.text = text;
        this.status = status;
    }

}
// [END comment_class]
