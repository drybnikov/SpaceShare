package com.denisr.garageshare.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class PostUser extends User {

    public UserStatus status;

    public PostUser() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public PostUser(User user, UserStatus status, String uid) {
        super(user.username, user.email);

        setUid(uid);
        this.userImage = user.userImage;
        this.status = status;
    }

}