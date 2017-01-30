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
import com.denisr.garageshare.view.CircleTransform;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
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

import static com.denisr.garageshare.PostDetailActivity.EXTRA_POST_KEY;

public class EditPostActivity extends BaseEditActivity implements View.OnClickListener {
    private static final int PLACE_PICKER_REQUEST = 2;
    private static final String TAG = "EditPostActivity";
    private DatabaseReference mPostReference;
    private FirebaseStorage storage;

    private String mPostKey;
    private int userImageSize;
    private Post currentPost;
    private String fieldRequired;
    private Uri imageUri;

    private ImageView authorImage;
    private ImageView postImage;
    private TextView authorName;

    private EditText postTitle;
    private EditText postBody;
    private TextView postLocationName;
    private TextView postLocationTitle;
    ValueEventListener postListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            currentPost = dataSnapshot.getValue(Post.class);
            authorName.setText(currentPost.author);

            if (imageUri == null && currentPost.imageUri != null) {
                loadPostImage(currentPost.imageUri);
            }
            if (currentPost.authorImageUri != null) {
                Glide.with(getApplicationContext())
                        .load(currentPost.authorImageUri)
                        .override(userImageSize, userImageSize)
                        .fitCenter()
                        .transform(new CircleTransform(getApplicationContext()))
                        .into(authorImage);
            }

            postTitle.setText(currentPost.title);
            postBody.setText(currentPost.body);
            updateLocationTitle();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            Toast.makeText(EditPostActivity.this, "Failed to load post.",
                    Toast.LENGTH_SHORT).show();
        }

        private void loadPostImage(String imageUri) {
            Glide.with(getApplicationContext())
                    .load(imageUri)
                    .centerCrop()
                    .into(postImage);
        }
    };
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.menu_edit_post);

        mPostKey = getIntent().getStringExtra(EXTRA_POST_KEY);
        if (mPostKey == null) {
            throw new IllegalArgumentException("Must pass EXTRA_POST_KEY");
        }

        userImageSize = getResources().getDimensionPixelSize(R.dimen.space_image_width);
        fieldRequired = getString(R.string.field_required);
        storage = FirebaseStorage.getInstance();
        mPostReference = FirebaseDatabase.getInstance().getReference().child("posts").child(mPostKey);

        authorName = (TextView) findViewById(R.id.post_author);
        authorImage = (ImageView) findViewById(R.id.post_author_photo);
        postTitle = (EditText) findViewById(R.id.field_title);
        postBody = (EditText) findViewById(R.id.field_body);

        postLocationName = (TextView) findViewById(R.id.field_location);
        postLocationTitle = (TextView) findViewById(R.id.field_location_hint);
        postImage = (ImageView) findViewById(R.id.post_image);
        mSubmitButton = (FloatingActionButton) findViewById(R.id.fab_edit_post);

        postLocationName.setOnClickListener(this);
        postImage.setOnClickListener(this);
        mSubmitButton.setOnClickListener(this);

        registerForContextMenu(postImage);
    }

    private void updatePost() {
        final String title = postTitle.getText().toString();
        final String body = postBody.getText().toString();

        if (TextUtils.isEmpty(title)) {
            postTitle.setError(fieldRequired);
            return;
        }

        if (TextUtils.isEmpty(body)) {
            postBody.setError(fieldRequired);
            return;
        }

        // Disable button so there are no multi-posts
        setEditingEnabled(false);
        //Snackbar.make(findViewById(R.id.coordinator), "Posting...", Snackbar.LENGTH_SHORT).show();
        showProgressDialog(R.string.updating);

        trackUpdatePost();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("title", title);
        childUpdates.put("body", body);
        childUpdates.put("latitude", currentPost.latitude);
        childUpdates.put("longitude", currentPost.longitude);

        mPostReference.updateChildren(childUpdates).
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

    private void trackUpdatePost() {
        Bundle bundle = new Bundle();
        bundle.putString("title", currentPost.title);
        bundle.putString("body", currentPost.body);
        bundle.putString("latitude", Double.toString(currentPost.latitude));
        bundle.putString("longitude", Double.toString(currentPost.longitude));

        mFirebaseAnalytics.logEvent("update_post", bundle);
    }

    private void setEditingEnabled(boolean enabled) {
        postTitle.setEnabled(enabled);
        postBody.setEnabled(enabled);
        if (enabled) {
            mSubmitButton.setVisibility(View.VISIBLE);
        } else {
            mSubmitButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        mPostReference.addValueEventListener(postListener);
    }

    @Override
    public void onStop() {
        super.onStop();

        // Remove post value event listener
        mPostReference.removeEventListener(postListener);
    }

    @Override
    public void updateImageView(Uri imageUri) {
        postImage.setImageURI(imageUri);
        this.imageUri = imageUri;
    }

    @Override
    public StorageReference getPhotoRef() {
        StorageReference storageRef = storage.getReference("post_photos");
        return storageRef.child(currentPost.uid);
    }

    @Override
    public void updateDataRef(String downloadUrl) {
        mPostReference.child("imageUri").
                setValue(downloadUrl).
                addOnCompleteListener(onCompleteListener).
                addOnFailureListener(onFailureListener);
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
                updatePost();
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

            currentPost.latitude = place.getLatLng().latitude;
            currentPost.longitude = place.getLatLng().longitude;

            postBody.setText(place.getName() + "," + place.getAddress());
            updateLocationTitle();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateLocationTitle() {
        String locationMsg = String.format(Locale.getDefault(), "[%s , %s]", currentPost.latitude, currentPost.longitude);
        postLocationTitle.setText(getString(R.string.post_location_title, locationMsg));
    }
}
