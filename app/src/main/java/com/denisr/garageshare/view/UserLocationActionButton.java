package com.denisr.garageshare.view;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import com.denisr.garageshare.R;
import com.denisr.garageshare.models.Post;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class UserLocationActionButton extends FloatingActionButton implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public static final int DISTANCE_HERE = 100;
    public static final int DISTANCE_NEAR_HERE = 200;
    private static final String TAG = UserLocationActionButton.class.getSimpleName();
    // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    // The fastest rate for active location updates. Exact. Updates will never be more frequent than this value.
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private static final String KEY_LOCATION = "location";
    private Animation animation;
    private Location postLocation;
    private GoogleApiClient mGoogleApiClient;
    // A request object to store parameters for requests to the FusedLocationProviderApi.
    private LocationRequest mLocationRequest;
    private boolean mLocationPermissionGranted;
    // The geographical location where the device is currently located.
    private Location mCurrentLocation;

    public UserLocationActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImageResource(R.drawable.ic_gps_off_white_24);
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        initAnimation();
        setAlpha(0.9f);
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    private void initAnimation() {
        animation = new AlphaAnimation(0.7f, 0.9f);
        animation.setDuration(200);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.setRepeatCount(1);
        animation.setRepeatMode(Animation.RESTART);
    }

    /**
     * Sets up the location request.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void initialise(Post post) {
        //this.post = post;

        postLocation = new Location("");
        postLocation.setLatitude(post.latitude);
        postLocation.setLongitude(post.longitude);

        /*super.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });*/
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        /*Parcelable parcelable = super.onSaveInstanceState();
        Bundle bundle = new Bundle();
        parcelable.putParcelable(KEY_LOCATION, mCurrentLocation);*/
        return super.onSaveInstanceState();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        // Retrieve location rom saved instance state.
        /*if (state != null) {
            Bundle bundle = (Bundle) state;
            mCurrentLocation = bundle.getParcelable(KEY_LOCATION);
        }*/
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE) {
            onResume();
        } else {
            onPause();
        }
    }

    private void onResume() {
        if (mGoogleApiClient.isConnected()) {
            getDeviceLocation();
        }
    }

    private void onPause() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
    }

    /**
     * Gets the current location of the device and starts the location update notifications.
     */
    @SuppressWarnings("MissingPermission")
    private void getDeviceLocation() {
        if (mLocationPermissionGranted) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                    mLocationRequest, this);
        } else {
            setImageResource(R.drawable.ic_gps_off_white_24);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            onResume();
        } else {
            onPause();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        setImageResource(R.drawable.ic_gps_not_fixed_white_24);
        getDeviceLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Play services connection suspended");
        setImageResource(R.drawable.ic_gps_off_white_24);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.d(TAG, "Play services connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
        setImageResource(R.drawable.ic_gps_off_white_24);
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        setImageResource(R.drawable.ic_gps_fixed_white_24);
        float distance = mCurrentLocation.distanceTo(postLocation);
        Log.d(TAG, "onLocationChanged distance:" + mCurrentLocation.distanceTo(postLocation));

        int color;
        if (distance <= DISTANCE_HERE) {
            color = R.color.palette_day_white;
        } else if (distance > DISTANCE_HERE && distance <= DISTANCE_NEAR_HERE) {
            color = R.color.grey_700;
        } else {
            color = R.color.grey_400;
        }

        startAnimation(animation);

        Drawable dealSettingsDrawable = getDrawable();
        if (dealSettingsDrawable != null) {
            dealSettingsDrawable.mutate();
            dealSettingsDrawable.setColorFilter(ContextCompat.getColor(getContext(), color), PorterDuff.Mode.SRC_IN);
        }
    }

    public void onRequestPermissionsResult(boolean permissionGranted) {
        if (permissionGranted && !mLocationPermissionGranted) {
            getDeviceLocation();
        }

        mLocationPermissionGranted = permissionGranted;
    }

    public boolean isUserInPostLocation() {
        return mCurrentLocation != null && mCurrentLocation.distanceTo(postLocation) <= DISTANCE_NEAR_HERE;
    }
}
