package com.denisr.garageshare;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
import com.denisr.garageshare.models.Post;
import com.denisr.garageshare.models.PostUser;
import com.denisr.garageshare.models.User;
import com.denisr.garageshare.models.UserStatus;
import com.denisr.garageshare.service.FirebaseAnalyticsManager;
import com.denisr.garageshare.view.CircleTransFormWithBorder;
import com.denisr.garageshare.viewholder.PostUserViewHolder;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostUsersActivity extends BaseActivity implements View.OnClickListener {

    public static final String EXTRA_POST_KEY = "post_key";
    public static final String EXTRA_ACTION_KEY = "action_key";
    public static final String EXTRA_ACTION_USER_ID = "action_user_id";

    public static final String ACTION_ADD_USER = "add_user";
    public static final String ACTION_BLOCK_USER = "block_user";

    private static final String TAG = "PostUsersActivity";

    private DatabaseReference mPostReference;
    private DatabaseReference mUsersReference;
    private String mPostKey;
    private UserAdapter mAdapter;
    private String mUid;
    private TextView mAuthorView;
    private ImageView mSpaceAthorPhoto;
    private ImageView mSpaceToolbarImage;
    private FloatingActionButton mAddNewSpace;
    private RecyclerView mCommentsRecycler;
    private final ValueEventListener userListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            Log.d(TAG, "userListener onDataChange count:" + dataSnapshot.getChildrenCount());
            Map<String, User> allUsersMap = new HashMap<>();

            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                User user = userSnapshot.getValue(User.class);
                allUsersMap.put(userSnapshot.getKey(), user);
            }

            // Listen for users
            mAdapter = new UserAdapter(PostUsersActivity.this, allUsersMap, mPostReference.child("users"));
            mCommentsRecycler.setAdapter(mAdapter);

            hideProgressDialog();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.w(TAG, "loadUser:onCancelled", databaseError.toException());
            hideProgressDialog();
        }
    };
    private FirebaseAnalyticsManager firebaseAnalyticsManager;
    private int userImageSize;
    private boolean isUserPost = false;
    private final ValueEventListener postListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            Post post = dataSnapshot.getValue(Post.class);
            Log.d(TAG, "postListener onDataChange post:" + post);

            if (post == null) {
                return;
            }
            mAuthorView.setText(post.author);

            if (post.imageUri != null) {
                Glide.with(getApplicationContext())
                        .load(post.imageUri)
                        .thumbnail(0.4f)
                        .centerCrop()
                        .into(mSpaceToolbarImage);
            }
            if (post.authorImageUri != null) {
                Glide.with(getApplicationContext())
                        .load(post.authorImageUri)
                        .override(userImageSize, userImageSize)
                        .fitCenter()
                        .transform(new CircleTransFormWithBorder(getApplicationContext()))
                        .into(mSpaceAthorPhoto);
            }
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(post.title);
            }
            isUserPost = post.uid.equals(mUid);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            Toast.makeText(PostUsersActivity.this, "Failed to load currentPost.",
                    Toast.LENGTH_SHORT).show();
        }
    };

    public static Intent getStartIntent(Context context, String postKey, String userUid, String action) {
        Intent intent = getStartIntent(context, postKey);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES).
                putExtra(EXTRA_ACTION_USER_ID, userUid).
                putExtra(EXTRA_ACTION_KEY, action);

        return intent;
    }

    public static Intent getStartIntent(Context context, String postKey) {
        Intent intent = new Intent(context, PostUsersActivity.class);
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

        // Get currentPost key from intent
        mPostKey = getIntent().getStringExtra(EXTRA_POST_KEY);
        if (mPostKey == null) {
            throw new IllegalArgumentException("Must pass EXTRA_POST_KEY");
        }

        userImageSize = getResources().getDimensionPixelSize(R.dimen.space_image_width);
        showProgressDialog(R.string.loading);

        // Initialize Database
        mPostReference = FirebaseDatabase.getInstance().getReference().child("posts").child(mPostKey);
        mUsersReference = FirebaseDatabase.getInstance().getReference().child("users");
        firebaseAnalyticsManager = new FirebaseAnalyticsManager(this);

        // Initialize Views
        mAuthorView = (TextView) findViewById(R.id.post_author);
        mSpaceAthorPhoto = (ImageView) findViewById(R.id.post_author_photo);
        mSpaceToolbarImage = (ImageView) findViewById(R.id.space_toolbar_image);

        mCommentsRecycler = (RecyclerView) findViewById(R.id.recycler_comments);
        mAddNewSpace = (FloatingActionButton) findViewById(R.id.fab_new_space);
        mAddNewSpace.setVisibility(View.GONE);
        findViewById(R.id.user_location_icon).setVisibility(View.GONE);

        mCommentsRecycler.setLayoutManager(new LinearLayoutManager(this));
        mUid = getUid();
    }

    @Override
    public void onStart() {
        super.onStart();

        mPostReference.addValueEventListener(postListener);
        mUsersReference.addValueEventListener(userListener);
    }

    @Override
    public void onStop() {
        super.onStop();

        mPostReference.removeEventListener(postListener);
        mUsersReference.removeEventListener(userListener);

        mAdapter.cleanupListener();
        mAdapter = null;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.fab_new_space) {

        }
    }

    private final class UserAdapter extends RecyclerView.Adapter<PostUserViewHolder> {

        private final Context mContext;
        private final DatabaseReference mDatabaseReference;
        private final Map<String, User> mAllUsersMap;
        private final int userImageSize;
        private ChildEventListener mChildEventListener;
        private List<String> mUserIds = new ArrayList<>();
        private List<PostUser> mPostUsers = new ArrayList<>();

        public UserAdapter(final Context context,
                           Map<String, User> allUsersMap,
                           DatabaseReference ref) {

            Log.d(TAG, "UserAdapter :" + ref.getKey());
            mContext = context;
            mDatabaseReference = ref;
            this.mAllUsersMap = allUsersMap;
            userImageSize = context.getResources().getDimensionPixelSize(R.dimen.space_image_width);

            // Create child event listener
            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    String userKey = dataSnapshot.getKey();
                    Log.d(TAG, "onChildAdded:" + userKey);
                    User user = mAllUsersMap.get(userKey);

                    if (user != null) {
                        UserStatus userStatus = dataSnapshot.getValue(UserStatus.class);
                        PostUser postUser = new PostUser(user, userStatus, userKey);
                        mUserIds.add(userKey);
                        mPostUsers.add(postUser);
                        notifyItemInserted(mPostUsers.size() - 1);
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());

                    UserStatus newUserStatus = dataSnapshot.getValue(UserStatus.class);
                    String userKey = dataSnapshot.getKey();

                    int userIndex = mUserIds.indexOf(userKey);
                    if (userIndex > -1) {
                        // Replace with the new data
                        mPostUsers.get(userIndex).status = newUserStatus;
                        //mPostUsers.set(userIndex, newComment);

                        // Update the RecyclerView
                        notifyItemChanged(userIndex);
                        //notifyDataSetChanged(); //We need update all, because it's can affect all items

                        PostUser postUser = mPostUsers.get(userIndex);
                        Snackbar.make(findViewById(R.id.coordinator),
                                "For user '" + postUser.username + "' status changed to " + postUser.status.name(),
                                Snackbar.LENGTH_LONG).
                                show();
                    } else {
                        Log.w(TAG, "onChildChanged:unknown_child:" + userKey);
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());

                    String userKey = dataSnapshot.getKey();

                    int userIndex = mUserIds.indexOf(userKey);
                    if (userIndex > -1) {
                        // Remove data from the list
                        mUserIds.remove(userIndex);
                        mPostUsers.remove(userIndex);

                        // Update the RecyclerView
                        notifyItemRemoved(userIndex);
                    } else {
                        Log.w(TAG, "onChildRemoved:unknown_child:" + userKey);
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "postComments:onCancelled", databaseError.toException());
                    Toast.makeText(mContext, "Failed to load comments.",
                            Toast.LENGTH_SHORT).show();
                }
            };
            ref.addChildEventListener(childEventListener);

            // Store reference to listener so it can be removed on app stop
            mChildEventListener = childEventListener;
        }

        @Override
        public PostUserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.item_post_user, parent, false);
            return new PostUserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final PostUserViewHolder holder, int position) {
            final PostUser postUser = mPostUsers.get(position);
            holder.bindToUser(postUser, userImageSize);

            holder.statusView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPopupMenu(holder.statusView, holder.getAdapterPosition());
                }
            });

            doIntentAction(postUser);
        }

        private void showPopupMenu(View view, int position) {
            // inflate menu
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu_user_managment, popup.getMenu());
            popup.setOnMenuItemClickListener(new MyMenuItemClickListener(position));
            popup.show();
        }

        private boolean doIntentAction(PostUser postUser) {
            boolean isAddUserAction = ACTION_ADD_USER.equals(getIntent().getStringExtra(EXTRA_ACTION_KEY));
            boolean isBlockUserAction = ACTION_BLOCK_USER.equals(getIntent().getStringExtra(EXTRA_ACTION_KEY));
            String userUid = getIntent().getStringExtra(EXTRA_ACTION_USER_ID);
            if (isAddUserAction && postUser.status == UserStatus.REQUESTED && postUser.getUid().equals(userUid)) {
                getIntent().removeExtra(EXTRA_ACTION_KEY);
                addUserAction(postUser);
                return true;
            } else if (isBlockUserAction && postUser.getUid().equals(userUid)) {
                getIntent().removeExtra(EXTRA_ACTION_KEY);
                blockUserAction(postUser);
                return true;
            }
            return false;
        }

        private void addUserAction(final PostUser postUser) {
            mDatabaseReference.child(postUser.getUid()).setValue(UserStatus.ALLOWED);
            firebaseAnalyticsManager.trackUserAdded(postUser);
        }

        private void blockUserAction(final PostUser postUser) {
            Snackbar.make(findViewById(R.id.coordinator), "Are you sure to block user '" + postUser.username + "'?", Snackbar.LENGTH_LONG).
                    setAction(R.string.menu_block_user, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mDatabaseReference.child(postUser.getUid()).setValue(UserStatus.BLOCKED);
                            firebaseAnalyticsManager.trackUserBlocked(postUser);
                        }
                    }).
                    show();
        }

        @Override
        public int getItemCount() {
            return mPostUsers.size();
        }

        public void cleanupListener() {
            if (mChildEventListener != null) {
                mDatabaseReference.removeEventListener(mChildEventListener);
            }
        }

        class MyMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

            private int position;

            public MyMenuItemClickListener(int positon) {
                this.position = positon;
            }

            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                final PostUser postUser = mPostUsers.get(position);
                switch (menuItem.getItemId()) {
                    case R.id.add_user:
                        addUserAction(postUser);
                        return true;
                    case R.id.block_user:
                        blockUserAction(postUser);
                        return true;
                    case R.id.delete_user:
                        Snackbar.make(findViewById(R.id.coordinator), "Are you sure to delete user '" + postUser.username + "'?", Snackbar.LENGTH_LONG).
                                setAction(R.string.dialog_button_delete, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        final DatabaseReference userRef = mDatabaseReference.child(postUser.getUid());
                                        userRef.removeValue();
                                        firebaseAnalyticsManager.trackUserDeleted(postUser);
                                    }
                                }).
                                show();
                        return true;
                    default:
                }
                return false;
            }
        }
    }
}
