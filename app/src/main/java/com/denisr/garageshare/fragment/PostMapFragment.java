package com.denisr.garageshare.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.denisr.garageshare.models.Post;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PostMapFragment extends SupportMapFragment implements OnMapReadyCallback {
    private static final String TAG = "PostMapFragment";
    private static final int DEFAULT_ZOOM = 15;
    private static final String EXTRA_POST_KEY = "post_key";
    private PostNotificationManager postNotificationManager;
    private GoogleMap mMap;
    private String mPostKey;

    public static PostMapFragment newInstance(String postKey) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_POST_KEY, postKey);
        PostMapFragment targetFragment = new PostMapFragment();
        targetFragment.setArguments(bundle);

        return targetFragment;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (getArguments() != null) {
            mPostKey = getArguments().getString(EXTRA_POST_KEY);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String mUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        postNotificationManager = new PostNotificationManager(getActivity(), mUid);

        getMapAsync(this);
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.setIndoorEnabled(true);
        mMap.setTrafficEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child("posts").limitToFirst(100).getRef();

        mDatabase.addChildEventListener(new ChildEventListener() {
            public void onChildAdded(DataSnapshot snapshot, String s) {
                Post post = snapshot.getValue(Post.class);
                post.setKey(snapshot.getKey());

                Log.d(TAG, "updateUserPosts post:" + post);
                if (TextUtils.isEmpty(mPostKey) || post.getKey().equals(mPostKey)) {
                    LatLng postLatLng = new LatLng(post.latitude, post.longitude);
                    mMap.addMarker(new MarkerOptions().position(postLatLng).title(post.title).snippet(post.body));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(postLatLng, DEFAULT_ZOOM));

                    postNotificationManager.onChildAdded(post);
                }
            }

            public void onChildChanged(DataSnapshot dataSnapshot, String key) {
                Post post = dataSnapshot.getValue(Post.class);
                post.setKey(dataSnapshot.getKey());
                Log.d(TAG, "onChildChanged post:" + post + ",key:" + dataSnapshot.getKey());
                postNotificationManager.onPostChanged(post);
            }

            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        });
    }
}
