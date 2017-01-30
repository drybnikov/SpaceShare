package com.denisr.garageshare;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.denisr.garageshare.fragment.PostMapFragment;
import com.denisr.garageshare.fragment.TimePickerDialogFragment;
import com.denisr.garageshare.models.Comment;
import com.denisr.garageshare.models.CommentStatus;
import com.denisr.garageshare.models.Post;
import com.denisr.garageshare.presentation.PostDetailPresenter;
import com.denisr.garageshare.presentation.PostDetailView;
import com.denisr.garageshare.service.FirebaseAnalyticsManager;
import com.denisr.garageshare.service.NotificationBuilder;
import com.denisr.garageshare.view.CircleTransFormWithBorder;
import com.denisr.garageshare.view.DialogUtil;
import com.denisr.garageshare.view.UserLocationActionButton;
import com.denisr.garageshare.viewholder.CommentViewHolder;

public class PostDetailActivity extends BaseActivity implements View.OnClickListener, TimePickerDialogFragment.TimePickerDialogListener, PostDetailView {

    public static final String EXTRA_POST_KEY = "post_key";
    public static final String EXTRA_ACTION_KEY = "action_key";

    public static final String ACTION_FINISH_USE_BOX = "finish_use_box";
    private static final String TAG = "PostDetailActivity";
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private final NotificationBuilder notificationManager = new NotificationBuilder();
    private String mPostKey;
    private CommentAdapter mAdapter;
    private String mUid;
    private TextView mAuthorView;
    private ImageView mSpaceAuthorPhoto;
    private ImageView mSpaceToolbarImage;
    private UserLocationActionButton mUserLocationActionButton;
    private FloatingActionButton mAddNewSpace;
    private View mapContainer;
    private RecyclerView mCommentsRecycler;
    private int authorImageSize;
    private boolean isUserPost = false;
    private PostDetailPresenter presenter;
    private FirebaseAnalyticsManager analyticsManager;

    public static Intent getStartIntent(Context context, String postKey, String action) {
        Intent intent = getStartIntent(context, postKey);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES).putExtra(EXTRA_ACTION_KEY, action);

        return intent;
    }

    public static Intent getStartIntent(Context context, String postKey) {
        Intent intent = new Intent(context, PostDetailActivity.class);
        intent.putExtra(EXTRA_POST_KEY, postKey);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow(); // in Activity's onCreate() for instance
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        setContentView(R.layout.activity_post_detail);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        analyticsManager = new FirebaseAnalyticsManager(this);
        presenter = new PostDetailPresenter(analyticsManager, getUid());
        presenter.attachView(this);

        // Get currentPost key from intent
        mPostKey = getIntent().getStringExtra(EXTRA_POST_KEY);
        if (mPostKey == null) {
            throw new IllegalArgumentException("Must pass EXTRA_POST_KEY");
        }

        authorImageSize = getResources().getDimensionPixelSize(R.dimen.space_image_width);
        showProgressDialog(R.string.loading);

        presenter.setPostKey(mPostKey);

        // Initialize Views
        mAuthorView = (TextView) findViewById(R.id.post_author);
        mSpaceAuthorPhoto = (ImageView) findViewById(R.id.post_author_photo);
        mSpaceToolbarImage = (ImageView) findViewById(R.id.space_toolbar_image);
        mUserLocationActionButton = (UserLocationActionButton) findViewById(R.id.user_location_icon);

        mCommentsRecycler = (RecyclerView) findViewById(R.id.recycler_comments);
        mAddNewSpace = (FloatingActionButton) findViewById(R.id.fab_new_space);
        mapContainer = findViewById(R.id.map_container);

        mAddNewSpace.setOnClickListener(this);
        mUserLocationActionButton.setOnClickListener(this);

        mCommentsRecycler.setLayoutManager(new GridLayoutManager(this, getResources().getInteger(R.integer.space_row_count)));
        mUid = getUid();

        getDeviceLocation();
    }

    private void getDeviceLocation() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mUserLocationActionButton.onRequestPermissionsResult(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        boolean locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }
        mUserLocationActionButton.onRequestPermissionsResult(locationPermissionGranted);
    }

    public void updatePost(Post post) {
        mAuthorView.setText(post.author);
        if (post.imageUri != null) {
            Glide.with(getApplicationContext())
                    .load(post.imageUri)
                    .centerCrop()
                    .into(mSpaceToolbarImage);
        }
        if (post.authorImageUri != null) {
            Glide.with(getApplicationContext())
                    .load(post.authorImageUri)
                    .override(authorImageSize, authorImageSize)
                    .fitCenter()
                    .transform(new CircleTransFormWithBorder(getApplicationContext()))
                    .into(mSpaceAuthorPhoto);
        }
        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(post.title);

        mAdapter.updatePost(post);
    }

    @Override
    public void initAdapter() {
        mAdapter = new CommentAdapter(this);
        mCommentsRecycler.setAdapter(mAdapter);
    }

    @Override
    public void failLoadData(String message) {
        Toast.makeText(PostDetailActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showInfoMessage(String message) {
        Snackbar.make(findViewById(R.id.coordinator), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void setBookedNotificationAlarm(String mPostKey, Post currentPost, Comment userComment) {
        notificationManager.setBookedNotificationAlarm(this, mPostKey, currentPost, userComment);
    }

    @Override
    public void hideBoxUsedNotification() {
        notificationManager.hideBoxUsedNotification(this);
    }

    @Override
    public void notifyItemInserted(int position) {
        mAdapter.notifyItemInserted(position);
    }

    @Override
    public void updateStatistic(String message) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(message);
        }
    }

    @Override
    public void notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void notifyItemRemoved(int position) {
        mAdapter.notifyItemRemoved(position);
    }

    @Override
    public void showEditCommentDialog(String text, DialogUtil.DialogClickListiner listiner) {
        DialogUtil.showDialogWithEditText(this, R.string.item_dialog_title_edit, text, listiner);
    }

    @Override
    public void showDeleteCommentDialog(String text, View.OnClickListener onClickListener) {
        Snackbar.make(findViewById(R.id.coordinator), "Are you sure to delete item '" + text + "'?", Snackbar.LENGTH_LONG).
                setAction(R.string.dialog_button_delete, onClickListener).
                show();
    }

    public void initialiseUserLocationButton(Post post) {
        mUserLocationActionButton.initialise(post);
    }

    public void updateAddNewSpace(boolean visible) {
        mAddNewSpace.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onStart() {
        super.onStart();
        presenter.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        presenter.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDetach();
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.fab_new_space) {
            DialogUtil.showDialogWithEditText(this, R.string.item_dialog_title_add, "", new DialogUtil.DialogClickListiner() {
                @Override
                public void onClick(DialogInterface dialogInterface, String itemValue) {
                    presenter.postComment(itemValue);
                }
            });
        } else if (i == R.id.user_location_icon) {
            PostMapFragment postMapFragment = (PostMapFragment) getSupportFragmentManager()
                    .findFragmentByTag(PostMapFragment.class.getName());
            if (postMapFragment == null) {
                postMapFragment = PostMapFragment.newInstance(mPostKey);
                getSupportFragmentManager().beginTransaction().replace(mapContainer.getId(), postMapFragment,
                        PostMapFragment.class.getName()).commit();
            }

            mapContainer.setVisibility(mapContainer.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, long startTime, long endTime) {
        presenter.onBookedTimeSelected(startTime, endTime);
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {

    }

    private final class CommentAdapter extends RecyclerView.Adapter<CommentViewHolder> {
        private final Context mContext;
        private Post currentPost;

        public CommentAdapter(final Context context) {
            mContext = context;
        }

        @Override
        public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final CommentViewHolder holder, int position) {
            final Comment comment = presenter.getCommentByPosition(position);
            final boolean isUserHasUsedSpace = presenter.userHasUsedComment();

            holder.bindToComment(comment, isUserHasUsedSpace, isUserUseThisComment(comment),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            analyticsManager.trackOnCommentClicked(comment, isUserHasUsedSpace, isUserUseThisComment(comment));

                            if (isUserHasUsedSpace && comment.status == CommentStatus.FREE) {
                                Snackbar.make(findViewById(R.id.coordinator), R.string.post_item_user_use_one, Snackbar.LENGTH_LONG).show();
                            } else if (comment.status != CommentStatus.FREE && !isUserUseThisComment(comment)) {
                                Snackbar.make(findViewById(R.id.coordinator), getString(R.string.post_item_already_used, comment.text), Snackbar.LENGTH_LONG).show();
                            } else if (comment.status == CommentStatus.USED && isUserUseThisComment(comment)) {
                                showCancelUsageDialog(comment);
                            } else if (mUserLocationActionButton.isUserInPostLocation()) {
                                presenter.startUseAction(comment);
                            } else {
                                Snackbar.make(findViewById(R.id.coordinator), R.string.post_item_location_so_far, Snackbar.LENGTH_LONG).
                                        setAction(
                                                R.string.post_item_use_it, new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        presenter.startUseAction(comment);
                                                    }
                                                }).show();
                            }
                        }
                    });

            if (isUserPost) {
                holder.actionView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showPopupMenu(holder.actionView, holder.getAdapterPosition());
                    }
                });
            } else {
                holder.actionView.setVisibility(View.GONE);
            }

            if (!finishUseBoxAction(comment)) {
                showBoxUsedNotification(comment);
            }
        }

        private boolean isUserUseThisComment(Comment comment) {
            return (comment.status.equals(CommentStatus.USED) || comment.status.equals(CommentStatus.BOOKED)) && comment.uid.equals(mUid);
        }

        private void showCancelUsageDialog(final Comment comment) {
            presenter.onCancelUsage(comment);

            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(R.string.finish_use_title).
                    setMessage(getString(R.string.finish_use_message, comment.text)).
                    setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            presenter.cancerUseAction(comment);
                        }
                    }).
                    setNeutralButton(R.string.book_space, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            selectBookedTime();
                        }
                    }).
                    setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }

        private void showPopupMenu(View view, int position) {
            // inflate menu
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.popup_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(new MyMenuItemClickListener(position));
            popup.show();
        }

        private boolean finishUseBoxAction(Comment comment) {
            boolean isFinishUseBoxAction = ACTION_FINISH_USE_BOX.equals(getIntent().getStringExtra(EXTRA_ACTION_KEY));
            if (isFinishUseBoxAction && comment.status == CommentStatus.USED && isUserUseThisComment(comment)) {
                getIntent().removeExtra(EXTRA_ACTION_KEY);
                showCancelUsageDialog(comment);
                return true;
            }

            return false;
        }

        private void showBoxUsedNotification(Comment comment) {
            if (isUserUseThisComment(comment) && comment.status.equals(CommentStatus.USED) && currentPost != null) {
                notificationManager.showBoxNotification(mContext, mPostKey, currentPost, comment);
            }
        }

        private void selectBookedTime() {
            DialogFragment newFragment = new TimePickerDialogFragment();
            newFragment.show(getSupportFragmentManager(), TimePickerDialogFragment.class.getSimpleName());
        }

        @Override
        public int getItemCount() {
            return presenter.getItemCount();
        }

        private void updatePost(Post post) {
            this.currentPost = post;
        }

        class MyMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

            private int position;

            public MyMenuItemClickListener(int positon) {
                this.position = positon;
            }

            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.edit_post:
                        presenter.onEditCommentClick(position);
                        return true;
                    case R.id.delete_post:
                        presenter.onDeleteCommentClick(position);
                        return true;
                    default:
                }
                return false;
            }
        }
    }
}
