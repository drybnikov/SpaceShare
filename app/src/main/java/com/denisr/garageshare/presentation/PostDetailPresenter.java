package com.denisr.garageshare.presentation;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.denisr.garageshare.models.Comment;
import com.denisr.garageshare.models.CommentStatus;
import com.denisr.garageshare.models.Post;
import com.denisr.garageshare.models.User;
import com.denisr.garageshare.service.FirebaseAnalyticsManager;
import com.denisr.garageshare.view.DialogUtil;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PostDetailPresenter {
    private static final String TAG = "PostDetailPresenter";

    private final FirebaseAnalyticsManager analyticsManager;
    private final String mUid;

    private PostDetailView view;

    private Comment selectedComment;
    private User mCurrentUser;
    private final ValueEventListener userListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            mCurrentUser = dataSnapshot.getValue(User.class);
            mCurrentUser.setUid(mUid);
            view.hideProgressDialog();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.w(TAG, "loadUser:onCancelled", databaseError.toException());
            view.hideProgressDialog();
        }
    };
    private Post currentPost;
    private DatabaseReference mPostReference;
    private DatabaseReference mCommentsReference;
    private ValueEventListener mPostListener;
    private String mPostKey;
    private boolean isUserPost = false;
    private ChildEventListener mChildEventListener;

    private List<String> mCommentIds = new ArrayList<>();
    private List<Comment> mComments = new ArrayList<>();
    // Create child event listener
    private final ChildEventListener childEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
            Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());

            // A new comment has been added, add it to the displayed list
            Comment comment = dataSnapshot.getValue(Comment.class);

            mCommentIds.add(dataSnapshot.getKey());
            mComments.add(comment);
            view.notifyItemInserted(mComments.size() - 1);
            updateStatistic();
        }

        private void updateStatistic() {
            view.updateStatistic("Free:" + (mComments.size() - getUsedCommentCount()) + " [" + mComments.size() + "]");
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
            Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());

            Comment newComment = dataSnapshot.getValue(Comment.class);
            String commentKey = dataSnapshot.getKey();

            int commentIndex = mCommentIds.indexOf(commentKey);
            if (commentIndex > -1) {
                // Replace with the new data
                mComments.set(commentIndex, newComment);

                // Update the RecyclerView
                //notifyItemChanged(commentIndex);
                analyticsManager.trackOnCommentChanged(newComment);
                view.notifyDataSetChanged(); //We need update all, because it's can affect all items
                updateStatistic();
            } else {
                Log.w(TAG, "onChildChanged:unknown_child:" + commentKey);
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());

            String commentKey = dataSnapshot.getKey();

            int commentIndex = mCommentIds.indexOf(commentKey);
            if (commentIndex > -1) {
                // Remove data from the list
                mCommentIds.remove(commentIndex);
                mComments.remove(commentIndex);

                // Update the RecyclerView
                view.notifyItemRemoved(commentIndex);
                updateStatistic();
            } else {
                Log.w(TAG, "onChildRemoved:unknown_child:" + commentKey);
            }
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
            Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());
            updateStatistic();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.w(TAG, "postComments:onCancelled", databaseError.toException());
            view.failLoadData("Failed to load comments.");
        }
    };

    public PostDetailPresenter(FirebaseAnalyticsManager analyticsManager, String uid) {
        this.analyticsManager = analyticsManager;
        mUid = uid;
    }

    public void attachView(PostDetailView view) {
        this.view = view;
    }

    public void setPostKey(String postKey) {
        mPostKey = postKey;

        mPostReference = FirebaseDatabase.getInstance().getReference().child("posts").child(mPostKey);
        mCommentsReference = FirebaseDatabase.getInstance().getReference().child("post-comments").child(mPostKey);
    }

    public void onDetach() {
        this.view = null;
    }

    public void onStart() {
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                currentPost = dataSnapshot.getValue(Post.class);

                if (currentPost == null) {
                    return;
                }

                isUserPost = currentPost.uid.equals(mUid);
                view.initialiseUserLocationButton(currentPost);
                view.updateAddNewSpace(isUserPost);
                view.updatePost(currentPost);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                view.failLoadData("Failed to load currentPost.");
            }
        };
        mPostReference.addValueEventListener(postListener);

        // Keep copy of currentPost listener so we can remove it when app stops
        mPostListener = postListener;

        FirebaseDatabase.getInstance().getReference().child("users").child(mUid)
                .addListenerForSingleValueEvent(userListener);

        // Listen for comments
        view.initAdapter();

        mCommentsReference.addChildEventListener(childEventListener);

        // Store reference to listener so it can be removed on app stop
        mChildEventListener = childEventListener;
    }

    public void onStop() {
        // Remove currentPost value event listener
        if (mPostListener != null) {
            mPostReference.removeEventListener(mPostListener);
        }

        cleanupListener();
    }

    private void cleanupListener() {
        if (mChildEventListener != null) {
            mCommentsReference.removeEventListener(mChildEventListener);
            mCommentIds.clear();
            mComments.clear();
        }
    }

    public void postComment(String itemValue) {
        if (validateAddNewItem(itemValue)) {
            // Get user information
            String authorName = mCurrentUser.username;

            // Create new comment object
            Comment comment = new Comment(mUid, authorName, itemValue, CommentStatus.FREE);

            // Push the comment, it will appear in the list
            mCommentsReference.push().setValue(comment);
            mPostReference.child(Post.ITEMS_COUNT).setValue(currentPost.itemsCount + 1);

            view.showInfoMessage("Item '" + itemValue + "' added");
        }
    }

    private boolean validateAddNewItem(String itemValue) {
        if (TextUtils.isEmpty(itemValue)) {
            view.showInfoMessage("Item name can't be empty");
            return false;
        }

        return true;
    }

    public void onCancelUsage(Comment comment) {
        selectedComment = comment;
    }

    public void onBookedTimeSelected(long startTime, long endTime) {
        if (selectedComment != null) {
            selectedComment.time = startTime;
            selectedComment.endTime = endTime;

            doBookAction(selectedComment);
        }
    }

    public void doBookAction(Comment comment) {
        updateCommentStatus(comment, true);
    }

    private void updateCommentStatus(Comment comment, boolean bookTomorrow) {
        int commentIndex = mComments.indexOf(comment);
        if (commentIndex > -1) {
            DatabaseReference commentRef = mCommentsReference.child(mCommentIds.get(commentIndex));
            onCommentClicked(commentRef, bookTomorrow);
        } else {
            Log.w(TAG, "onClick:unknown_child:" + comment);
        }
    }

    public void onCommentClicked(final DatabaseReference postRef, final boolean bookTomorrow) {
        postRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Comment userComment = mutableData.getValue(Comment.class);
                if (userComment == null) {
                    return Transaction.success(mutableData);
                }

                if (userComment.status == CommentStatus.FREE || userComment.status == CommentStatus.BOOKED) {
                    if (userComment.status == CommentStatus.FREE) {
                        mPostReference.child(Post.USED_ITEMS).setValue(currentPost.usedItemsCount + 1);
                    }

                    userComment.status = CommentStatus.USED;
                    userComment.author = mCurrentUser.username;
                    userComment.uid = mCurrentUser.getUid();
                    userComment.time = System.currentTimeMillis();
                    userComment.userImage = mCurrentUser.userImage;
                } else if (bookTomorrow) {
                    userComment.status = CommentStatus.BOOKED;
                    userComment.time = selectedComment.time;
                    userComment.endTime = selectedComment.endTime;
                    userComment.userImage = mCurrentUser.userImage;

                    view.setBookedNotificationAlarm(mPostKey, currentPost, userComment);
                } else {
                    userComment.status = CommentStatus.FREE;
                    userComment.author = "";
                    userComment.uid = "";
                    userComment.userImage = "";
                    userComment.time = 0;

                    mPostReference.child(Post.USED_ITEMS).setValue(currentPost.usedItemsCount - 1);
                    view.hideBoxUsedNotification();
                }

                // Set value and report transaction success
                mutableData.setValue(userComment);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b,
                                   DataSnapshot dataSnapshot) {
                // Transaction completed
                Log.d(TAG, "postTransaction:onComplete:" + databaseError);
            }
        });
    }

    public void cancerUseAction(Comment comment) {
        updateCommentStatus(comment, false);
    }

    public void startUseAction(Comment comment) {
        updateCommentStatus(comment, false);
    }

    public boolean userHasUsedComment() {
        for (Comment comment : mComments) {
            if (comment.uid.equals(mUid) &&
                    (comment.status.equals(CommentStatus.USED) || comment.status.equals(CommentStatus.BOOKED))) {
                return true;
            }
        }

        return false;
    }

    private int getUsedCommentCount() {
        int count = 0;
        for (Comment comment : mComments) {
            if (CommentStatus.USED.equals(comment.status)) {
                count++;
            }
        }

        return count;
    }

    public int getItemCount() {
        return mComments.size();
    }

    public void onEditCommentClick(final int position) {
        final Comment comment = getCommentByPosition(position);
        view.showEditCommentDialog(comment.text, new DialogUtil.DialogClickListiner() {
            @Override
            public void onClick(DialogInterface dialogInterface, String itemValue) {
                editComment(position, itemValue);
            }
        });
    }

    public Comment getCommentByPosition(int position) {
        return mComments.get(position);
    }

    private void editComment(int position, String itemValue) {
        if (validateAddNewItem(itemValue)) {
            final DatabaseReference commentRef = mCommentsReference.child(mCommentIds.get(position));
            commentRef.child("text").setValue(itemValue);
        }
    }

    public void onDeleteCommentClick(final int position) {
        final Comment comment = getCommentByPosition(position);
        view.showDeleteCommentDialog(comment.text, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteComment(position);
            }
        });
    }

    private void deleteComment(int position) {
        final DatabaseReference commentRef = mCommentsReference.child(mCommentIds.get(position));
        commentRef.removeValue();

        mPostReference.child(Post.ITEMS_COUNT).setValue(currentPost.itemsCount - 1);
    }
}
