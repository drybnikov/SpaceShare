package com.denisr.garageshare.viewholder;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.denisr.garageshare.R;
import com.denisr.garageshare.models.PostUser;
import com.denisr.garageshare.view.CircleTransform;

public class PostUserViewHolder extends RecyclerView.ViewHolder {

    public TextView authorView;
    public TextView statusView;
    public ImageView itemPhoto;
    public ImageView statusImageView;

    private Context context;

    public PostUserViewHolder(View itemView) {
        super(itemView);

        context = itemView.getContext();

        authorView = (TextView) itemView.findViewById(R.id.post_author);
        statusView = (TextView) itemView.findViewById(R.id.post_status);
        itemPhoto = (ImageView) itemView.findViewById(R.id.post_author_photo);
        statusImageView = (ImageView) itemView.findViewById(R.id.post_image_status);
    }

    public void bindToUser(PostUser user, int userImageSize) {
        authorView.setText(user.username);
        statusView.setText(user.status.name());

        switch (user.status) {
            case ALLOWED:
                statusView.setTextColor(context.getResources().getColor(R.color.colorFree));
                statusImageView.setImageResource(R.drawable.ic_access_allowed);
                break;
            case REQUESTED:
                statusView.setTextColor(context.getResources().getColor(R.color.colorPrimaryDark));
                statusImageView.setImageResource(R.drawable.ic_access_requested);
                break;
            case BLOCKED:
                statusView.setTextColor(context.getResources().getColor(R.color.colorUsed));
                statusImageView.setImageResource(R.drawable.ic_access_denied);
                break;
        }

        if (user.userImage != null) {
            Glide.with(context)
                    .load(user.userImage)
                    .override(userImageSize, userImageSize)
                    .fitCenter()
                    .transform(new CircleTransform(context))
                    .into(itemPhoto);
        }
    }
}
