package com.denisr.garageshare.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

// [START post_class]
@IgnoreExtraProperties
public class Post {

    public static final String ITEMS_COUNT = "itemsCount";
    public static final String USED_ITEMS = "usedItemsCount";

    public String uid;
    public String author;
    public String title;
    public String body;
    public String imageUri;
    public String authorImageUri;
    public double latitude;
    public double longitude;
    public Map<String, UserStatus> users = new HashMap<>();
    public int itemsCount;
    public int usedItemsCount;
    public boolean isPublic;

    private String key;

    public Post() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public Post(String uid, String author, String authorImageUri, String title, String body, double latitude, double longitude) {
        this.uid = uid;
        this.author = author;
        this.title = title;
        this.body = body;
        this.authorImageUri = authorImageUri;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("author", author);
        result.put("title", title);
        result.put("body", body);
        result.put("authorImageUri", authorImageUri);
        result.put("imageUri", imageUri);
        result.put("latitude", latitude);
        result.put("longitude", longitude);
        result.put(ITEMS_COUNT, itemsCount);
        result.put(USED_ITEMS, usedItemsCount);
        result.put("users", users);

        return result;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "Post{" +
                "uid='" + uid + '\'' +
                ", author='" + author + '\'' +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", imageUri='" + imageUri + '\'' +
                ", authorImageUri='" + authorImageUri + '\'' +
                '}';
    }
}
