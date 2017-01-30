package com.denisr.garageshare.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.denisr.garageshare.R;
import com.denisr.garageshare.models.Post;
import com.denisr.garageshare.models.UserStatus;
import com.denisr.garageshare.view.CircleTransform;


public class PostViewHolder extends RecyclerView.ViewHolder {

    public TextView titleView;
    public TextView authorView;
    //public ImageView starView;
    public TextView itemsCountView;
    public TextView usedItemsCountView;
    public TextView bodyView;
    public ImageView actionView;
    public ImageView authorImageView;
    public ImageView userStatus;
    public ImageView postImageView;
    public ProgressBar postProgress;

    private final int userImageSize;

    public PostViewHolder(View itemView) {
        super(itemView);
        userImageSize = itemView.getResources().getDimensionPixelSize(R.dimen.space_image_width);

        titleView = (TextView) itemView.findViewById(R.id.post_title);
        authorView = (TextView) itemView.findViewById(R.id.post_author);
        //starView = (ImageView) itemView.findViewById(R.id.star);
        itemsCountView = (TextView) itemView.findViewById(R.id.post_items_count);
        usedItemsCountView = (TextView) itemView.findViewById(R.id.post_used_items_count);
        bodyView = (TextView) itemView.findViewById(R.id.post_body);
        actionView = (ImageView) itemView.findViewById(R.id.expanded_menu);
        authorImageView = (ImageView) itemView.findViewById(R.id.post_author_photo);
        postImageView = (ImageView) itemView.findViewById(R.id.post_image_layout);

        userStatus = (ImageView) itemView.findViewById(R.id.user_status_icon);

        postProgress = (ProgressBar) itemView.findViewById(R.id.post_items_progress);
    }

    public void bindToPost(Post post, String uid, View.OnClickListener itemListener, View.OnClickListener actionListiner) {
        final boolean isCurrentUserPost = post.uid.equals(uid);
        final UserStatus currentUserStatus = post.users.get(uid);
        final boolean isUserAllowedToPost = currentUserStatus != null && currentUserStatus == UserStatus.ALLOWED;

        if (isCurrentUserPost || isUserAllowedToPost) {
            userStatus.setImageResource(R.drawable.ic_access_allowed);
        } else if (currentUserStatus == null) {
            userStatus.setImageResource(R.drawable.ic_access_request);
        } else if (currentUserStatus == UserStatus.REQUESTED) {
            userStatus.setImageResource(R.drawable.ic_access_requested);
        } else {
            userStatus.setImageResource(R.drawable.ic_access_denied);
        }

        titleView.setText(post.title);
        authorView.setText(post.author);
        itemsCountView.setText("(" + post.itemsCount + ")");
        usedItemsCountView.setText(post.usedItemsCount + "");
        postProgress.setMax(post.itemsCount);
        postProgress.setProgress(post.usedItemsCount);
        bodyView.setText(post.body);

        itemView.setOnClickListener(itemListener);

        if (isCurrentUserPost) {
            actionView.setVisibility(View.VISIBLE);
            actionView.setOnClickListener(actionListiner);
        } else {
            actionView.setVisibility(View.GONE);
        }

        if (post.authorImageUri != null) {
            Glide.with(itemView.getContext())
                    .load(post.authorImageUri)
                    .override(userImageSize, userImageSize)
                    .fitCenter()
                    .transform(new CircleTransform(itemView.getContext()))
                    .into(authorImageView);
        }
        if (post.imageUri != null) {
            Glide.with(itemView.getContext())
                    .load(post.imageUri)
                    //.override(userImageSize, userImageSize)
                    .centerCrop()
                    //.transform(new CircleTransform(itemView.getContext()))
                    .into(postImageView);
        }
    }
}
