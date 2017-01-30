package com.denisr.garageshare.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.denisr.garageshare.EditPostActivity;
import com.denisr.garageshare.PostDetailActivity;
import com.denisr.garageshare.PostUsersActivity;
import com.denisr.garageshare.R;
import com.denisr.garageshare.models.Post;
import com.denisr.garageshare.presentation.PostListPresenter;
import com.denisr.garageshare.presentation.PostListView;
import com.denisr.garageshare.viewholder.PostViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

public abstract class PostListFragment extends Fragment implements PostListView {

    private static final String TAG = "PostListFragment";

    private DatabaseReference mDatabase;
    private FirebaseRecyclerAdapter<Post, PostViewHolder> mAdapter;
    private RecyclerView mRecycler;
    private GridLayoutManager mManager;
    private String mUid;

    private PostListPresenter presenter;

    public PostListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_all_posts, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        mRecycler = (RecyclerView) rootView.findViewById(R.id.messages_list);
        mRecycler.setHasFixedSize(true);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set up Layout Manager, reverse layout
        mManager = new GridLayoutManager(getActivity(), getResources().getInteger(R.integer.post_row_count));
        //.mManager.setReverseLayout(true);
        //mManager.setStackFromEnd(true);
        mRecycler.setLayoutManager(mManager);
        mUid = getUid();

        presenter = new PostListPresenter(mUid);
        presenter.attachView(this);

        // Set up FirebaseRecyclerAdapter with the Query
        Query postsQuery = getQuery(mDatabase);
        mAdapter = new FirebaseRecyclerAdapter<Post, PostViewHolder>(Post.class, R.layout.item_post,
                PostViewHolder.class, postsQuery) {

            @Override
            protected void populateViewHolder(final PostViewHolder viewHolder, final Post model, final int position) {
                final DatabaseReference postRef = getRef(position);
                // Set click listener for the whole post view
                model.setKey(postRef.getKey());

                // Bind Post to ViewHolder, setting OnClickListener for the item
                viewHolder.bindToPost(model, mUid, new View.OnClickListener() {
                    @Override
                    public void onClick(View itemView) {
                        presenter.onItemClick(model, postRef);
                    }
                }, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showPopupMenu(viewHolder.actionView, position);
                    }
                });
            }

            private void showPopupMenu(View view, int position) {
                // inflate menu
                PopupMenu popup = new PopupMenu(view.getContext(), view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.popup_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(new MyMenuItemClickListener(position));
                popup.show();
            }

            class MyMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

                private int position;

                private MyMenuItemClickListener(int positon) {
                    this.position = positon;
                }

                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    final DatabaseReference postRef = getRef(position);

                    switch (menuItem.getItemId()) {
                        case R.id.edit_post:
                            Intent intent = new Intent(getActivity(), EditPostActivity.class);
                            intent.putExtra(PostDetailActivity.EXTRA_POST_KEY, postRef.getKey());
                            startActivity(intent);
                            return true;
                        case R.id.delete_post:
                            Toast.makeText(getActivity().getApplicationContext(), "Delete post", Toast.LENGTH_SHORT).show();
                            return true;
                        case R.id.post_users:
                            intent = new Intent(getActivity(), PostUsersActivity.class);
                            intent.putExtra(PostUsersActivity.EXTRA_POST_KEY, postRef.getKey());
                            startActivity(intent);
                            return true;

                        default:
                    }
                    return false;
                }
            }
        };
        mRecycler.setAdapter(mAdapter);
    }

    public String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public abstract Query getQuery(DatabaseReference databaseReference);

    public void openPostDetail(String key) {
        Intent intent = new Intent(getActivity(), PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_KEY, key);
        startActivity(intent);
    }

    public void showRequestAccessDialog(final DatabaseReference postRef) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.post_request_access_title).
                setMessage(getString(R.string.post_request_access_message)).
                setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        presenter.onRequestAccessClicked(postRef);
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.cleanup();
        }
        presenter.onDetach();
    }

    @Override
    public void showAccessInfo(@StringRes int messageId, String title) {
        Snackbar.make(getActivity().findViewById(R.id.coordinator), getString(messageId, title), Snackbar.LENGTH_LONG).show();
    }
}
