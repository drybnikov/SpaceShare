package com.denisr.garageshare;

import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.denisr.garageshare.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Map;

/**
 * Activity to upload and download photos from Firebase Storage.
 */
public class EditProfileActivity extends BaseEditActivity implements View.OnClickListener {

    private static final String TAG = "EditProfileActivity";
    private ImageView userImage;
    private TextView userEmail;
    private TextView userName;
    private final ValueEventListener userListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            User currentUser = dataSnapshot.getValue(User.class);
            userName.setText(currentUser.username);
            userEmail.setText(currentUser.email);

            if (currentUser.userImage != null) {
                Glide.with(getApplicationContext())
                        .load(currentUser.userImage)
                        .placeholder(R.drawable.ic_action_account_circle_40)
                        .thumbnail(0.5f)
                        .centerCrop()
                        .into(userImage);
            } else {
                userImage.setImageResource(R.drawable.ic_user_unknown);
            }

            hideProgressDialog();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.w(TAG, "loadUser:onCancelled", databaseError.toException());
            hideProgressDialog();
        }
    };
    private Uri imageUri;
    private FloatingActionButton mSubmitButton;
    private FirebaseStorage storage;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.menu_edit_profile);

        storage = FirebaseStorage.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference().child("users").child(getUid());

        userEmail = (TextView) findViewById(R.id.user_email);
        userName = (TextView) findViewById(R.id.user_name);
        userImage = (ImageView) findViewById(R.id.user_image);
        mSubmitButton = (FloatingActionButton) findViewById(R.id.fab_edit_post);

        userImage.setOnClickListener(this);
        mSubmitButton.setOnClickListener(this);

        registerForContextMenu(userImage);
        showProgressDialog(R.string.loading);
    }

    @Override
    public void onStart() {
        super.onStart();

        userRef.addValueEventListener(userListener);
    }

    @Override
    public void onStop() {
        super.onStop();

        // Remove post value event listener
        userRef.removeEventListener(userListener);
    }

    @Override
    public void updateImageView(Uri imageUri) {
        userImage.setImageURI(imageUri);
        this.imageUri = imageUri;
    }

    @Override
    public StorageReference getPhotoRef() {
        StorageReference storageRef = storage.getReference("user_photos");
        return storageRef.child(getUid());
    }

    @Override
    public void updateDataRef(String downloadUrl) {
        boolean isComplete = userRef.child("userImage").setValue(downloadUrl).isComplete();
        Log.d(TAG, "OnSuccess:" + isComplete);

        DatabaseReference mPostReference = FirebaseDatabase.getInstance().getReference().child("posts");
        updateUserPosts(mPostReference, downloadUrl);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.post_image:
                openContextMenu(userImage);
                break;
            case R.id.fab_edit_post:
                updateUserProfile();
                break;
        }
    }

    private void updateUserProfile() {
        setEditingEnabled(false);
        showProgressDialog(R.string.updating);
        if (imageUri != null) {
            uploadFromUri(imageUri);
        } else {
            hideProgressDialog();
            finish();
        }
    }

    private void setEditingEnabled(boolean enabled) {
        userEmail.setEnabled(enabled);
        userName.setEnabled(enabled);
        if (enabled) {
            mSubmitButton.setVisibility(View.VISIBLE);
        } else {
            mSubmitButton.setVisibility(View.GONE);
        }
    }

    private void updateUserPosts(DatabaseReference postRef, final String userImageUri) {
        /*postRef.addChildEventListener(new ChildEventListener() {
            public void onChildAdded(DataSnapshot snapshot, String s) {
                Post post = snapshot.getValue(Post.class);
                Log.d(TAG, "updateUserPosts post:" + post + ",s:" + s);

                snapshot.getRef().child("authorImageUri").setValue(userImageUri);
            }

            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            public void onCancelled(DatabaseError databaseError) {
            }
        });*/

        postRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                //Post p = mutableData.getValue(Post.class);
                Map<String, Object> data = (Map) mutableData.getValue();
                Log.d(TAG, "updateUserPosts post:" + data);
                if (data == null) {
                    return Transaction.success(mutableData);
                }

                for (String key : data.keySet()) {
                    Map<String, Object> postValues = (Map) data.get(key);
                    Log.d(TAG, "updateUserPosts postValues:" + postValues);
                    if (postValues != null) {
                        postValues.put("authorImageUri", userImageUri);
                    }
                }

                Log.d(TAG, "updateUserPosts post:" + data);
                // Set value and report transaction success
                mutableData.setValue(data);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b,
                                   DataSnapshot dataSnapshot) {
                // Transaction completed
                Log.d(TAG, "postTransaction:onComplete:" + databaseError);
                hideProgressDialog();
                finish();
            }
        });
    }
}
