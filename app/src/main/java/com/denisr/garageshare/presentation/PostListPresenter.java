package com.denisr.garageshare.presentation;

import android.util.Log;

import com.denisr.garageshare.R;
import com.denisr.garageshare.models.Post;
import com.denisr.garageshare.models.UserStatus;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

public class PostListPresenter {
    private static final String TAG = "PostListPresenter";

    private final String mUid;
    private PostListView view;

    public PostListPresenter(String mUid) {
        this.mUid = mUid;
    }

    public void attachView(PostListView view) {
        this.view = view;
    }

    public void onDetach() {
        this.view = null;
    }

    public void onRequestAccessClicked(DatabaseReference postRef) {
        postRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Post p = mutableData.getValue(Post.class);
                if (p == null) {
                    return Transaction.success(mutableData);
                }

                if (p.users.containsKey(mUid)) {
                    view.showAccessInfo(R.string.post_access_already_requested_text, p.title);
                } else {
                    p.users.put(mUid, UserStatus.REQUESTED);
                }

                // Set value and report transaction success
                mutableData.setValue(p);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b,
                                   DataSnapshot dataSnapshot) {
                // Transaction completed
                Log.d(TAG, "postTransaction:onComplete:" + databaseError + "," + b);
            }
        });
    }

    public void onItemClick(Post model, DatabaseReference postRef) {
        final boolean isCurrentUserPost = model.uid.equals(mUid);
        final UserStatus currentUserStatus = model.users.get(mUid);
        final boolean isUserAllowedToPost = currentUserStatus != null && currentUserStatus == UserStatus.ALLOWED;

        // Launch PostDetailActivity
        if (isCurrentUserPost || isUserAllowedToPost) {
            view.openPostDetail(model.getKey());
        } else if (currentUserStatus == null) {
            view.showRequestAccessDialog(postRef);
        } else if (currentUserStatus == UserStatus.REQUESTED) {
            view.showAccessInfo(R.string.post_access_already_requested_text, model.title);
        } else {
            view.showAccessInfo(R.string.post_access_blocked_text, model.title);
        }
    }
}
