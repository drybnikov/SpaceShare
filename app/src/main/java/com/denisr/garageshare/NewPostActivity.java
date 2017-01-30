package com.denisr.garageshare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.denisr.garageshare.models.Post;
import com.denisr.garageshare.models.User;
import com.denisr.garageshare.view.CircleTransform;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NewPostActivity extends BaseEditActivity implements View.OnClickListener {

    private static final String TAG = "NewPostActivity";
    private static final String REQUIRED = "Required";
    private static final int PLACE_PICKER_REQUEST = 2;

    public LatLng latLng;
    final ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            Log.d(TAG, "writeNewPost:onDataChange" + dataSnapshot.getValue().toString());
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.w(TAG, "writeNewPost:onCancelled", databaseError.toException());
        }
    };
    private DatabaseReference mDatabase;
    private FirebaseStorage storage;
    private Uri imageUri;
    private User currentUser;
    private String newPostKey;

    private ImageView authorImage;
    private ImageView postImage;
    private TextView authorName;
    private EditText mTitleField;
    private EditText mBodyField;
    private TextView postLocationName;
    private TextView postLocationTitle;
    private FloatingActionButton mSubmitButton;
    private final OnCompleteListener<Void> onCompleteListener = new OnCompleteListener<Void>() {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            Log.d(TAG, "OnSuccess:" + task.isComplete());
            setEditingEnabled(true);
            hideProgressDialog();
            finish();
        }
    };
    private final OnFailureListener onFailureListener = new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
            setEditingEnabled(true);
            Log.w(TAG, "onFailure:", e);
            Snackbar.make(findViewById(R.id.coordinator), "Failing:" + e.getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
            hideProgressDialog();
        }
    };

    @Override
    public void updateImageView(Uri imageUri) {
        postImage.setImageURI(imageUri);
        this.imageUri = imageUri;
    }

    @Override
    public StorageReference getPhotoRef() {
        return storage.getReference("post_photos").child(getUid());
    }

    @Override
    public void updateDataRef(String downloadUrl) {
        DatabaseReference mPostReference = FirebaseDatabase.getInstance().getReference().child("posts").child(newPostKey);
        mPostReference.child("imageUri").
                setValue(downloadUrl).
                addOnCompleteListener(onCompleteListener).
                addOnFailureListener(onFailureListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.menu_add_space);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        storage = FirebaseStorage.getInstance();

        mTitleField = (EditText) findViewById(R.id.field_title);
        mBodyField = (EditText) findViewById(R.id.field_body);
        mSubmitButton = (FloatingActionButton) findViewById(R.id.fab_edit_post);
        authorName = (TextView) findViewById(R.id.post_author);
        authorImage = (ImageView) findViewById(R.id.post_author_photo);

        postLocationName = (TextView) findViewById(R.id.field_location);
        postLocationTitle = (TextView) findViewById(R.id.field_location_hint);
        postImage = (ImageView) findViewById(R.id.post_image);

        postLocationName.setOnClickListener(this);
        postImage.setOnClickListener(this);
        mSubmitButton.setOnClickListener(this);
        registerForContextMenu(postImage);

        final int userImageSize = getResources().getDimensionPixelSize(R.dimen.space_image_width);
        final String userId = getUid();
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        currentUser = dataSnapshot.getValue(User.class);

                        if (currentUser == null) {
                            // User is null, error out
                            Log.e(TAG, "User " + userId + " is unexpectedly null");
                            Toast.makeText(NewPostActivity.this,
                                    "Error: could not fetch user.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            authorName.setText(currentUser.username);

                            if (currentUser.userImage != null) {
                                Glide.with(getApplicationContext())
                                        .load(currentUser.userImage)
                                        .override(userImageSize, userImageSize)
                                        .fitCenter()
                                        .transform(new CircleTransform(getApplicationContext()))
                                        .into(authorImage);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                    }
                });
    }

    private void submitPost() {
        final String title = mTitleField.getText().toString();
        final String body = mBodyField.getText().toString();

        // Title is required
        if (TextUtils.isEmpty(title)) {
            mTitleField.setError(REQUIRED);
            return;
        }

        // Body is required
        if (TextUtils.isEmpty(body)) {
            mBodyField.setError(REQUIRED);
            return;
        }

        // Disable button so there are no multi-posts
        setEditingEnabled(false);
        showProgressDialog(R.string.updating);

        // [START single_value_read]
        if (currentUser == null) {
            // User is null, error out
            Log.e(TAG, "User " + getUid() + " is unexpectedly null");
            Toast.makeText(NewPostActivity.this,
                    "Error: could not fetch user.",
                    Toast.LENGTH_SHORT).show();
            finish();
        } else {
            final String userId = getUid();
            Post post = new Post(userId, currentUser.username, currentUser.userImage, title, body, latLng.latitude, latLng.longitude);
            writeNewPost(userId, post);
        }
    }

    private void setEditingEnabled(boolean enabled) {
        mTitleField.setEnabled(enabled);
        mBodyField.setEnabled(enabled);
        if (enabled) {
            mSubmitButton.setVisibility(View.VISIBLE);
        } else {
            mSubmitButton.setVisibility(View.GONE);
        }
    }

    private void writeNewPost(String userId, Post post) {
        mDatabase.addListenerForSingleValueEvent(valueEventListener);

        // Create new post at /user-posts/$userid/$postid and at
        // /posts/$postid simultaneously
        newPostKey = mDatabase.child("posts").push().getKey();
        Map<String, Object> postValues = post.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/posts/" + newPostKey, postValues);
        childUpdates.put("/user-posts/" + userId + "/" + newPostKey, postValues);

        mDatabase.updateChildren(childUpdates).
                addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (imageUri != null) {
                            uploadFromUri(imageUri);
                        } else {
                            onCompleteListener.onComplete(task);
                        }
                    }
                }).addOnFailureListener(onFailureListener);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.post_image:
                openContextMenu(postImage);
                break;
            case R.id.field_location:
                changePostLocation();
                break;
            case R.id.fab_edit_post:
                submitPost();
                break;
        }
    }

    private void changePostLocation() {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK) {
            Place place = PlacePicker.getPlace(this, data);

            latLng = place.getLatLng();

            mBodyField.setText(place.getName() + "," + place.getAddress());
            updateLocationTitle();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateLocationTitle() {
        String locationMsg = String.format(Locale.getDefault(), "[%s , %s]", latLng.latitude, latLng.longitude);
        postLocationTitle.setText(getString(R.string.post_location_title, locationMsg));
    }
}
