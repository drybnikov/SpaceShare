package com.denisr.garageshare.viewholder;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.denisr.garageshare.R;
import com.denisr.garageshare.models.Comment;
import com.denisr.garageshare.models.CommentStatus;

public class CommentViewHolder extends RecyclerView.ViewHolder {

    public TextView authorView;
    public TextView bodyView;
    public TextView statusView;
    public TextView itemTime;
    public ImageView itemPhoto;
    public ImageView actionView;
    public View itemBody;

    private int userImageSize;
    private Context context;

    public CommentViewHolder(View itemView) {
        super(itemView);

        context = itemView.getContext();
        userImageSize = context.getResources().getDimensionPixelSize(R.dimen.user_space_image_width);

        authorView = (TextView) itemView.findViewById(R.id.comment_author);
        bodyView = (TextView) itemView.findViewById(R.id.comment_body);
        statusView = (TextView) itemView.findViewById(R.id.comment_status);
        itemBody = itemView.findViewById(R.id.item_body);
        itemPhoto = (ImageView) itemView.findViewById(R.id.comment_photo);
        itemTime = (TextView) itemView.findViewById(R.id.comment_time);
        actionView = (ImageView) itemView.findViewById(R.id.expanded_menu);
    }

    public void bindToComment(Comment comment, boolean userHasUsedSpace, boolean isUserUseThisComment,
                              View.OnClickListener itemClickListener) {
        authorView.setText(comment.author);
        bodyView.setText(comment.text);
        statusView.setText(comment.status.name());
        if (comment.time > 0 && comment.status == CommentStatus.USED) {
            itemTime.setText(DateUtils.formatDateTime(context, comment.time, DateUtils.FORMAT_SHOW_TIME) +
                    " (" + DateUtils.formatElapsedTime((System.currentTimeMillis() - comment.time) / 1000) + ")");
        } else if (comment.time > 0 && comment.endTime > 0 && comment.status == CommentStatus.BOOKED) {
            itemTime.setText(DateUtils.formatDateTime(context, comment.time, DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME) +
                    " - " + DateUtils.formatDateTime(context, comment.endTime, DateUtils.FORMAT_SHOW_TIME));
        } else {
            itemTime.setText("");
        }
        switch (comment.status) {
            case FREE:
                itemBody.setBackgroundResource(userHasUsedSpace ? R.color.colorDisabled : R.color.colorFree);
                itemPhoto.setImageDrawable(null);
                break;
            case BOOKED:
                itemBody.setBackgroundResource(isUserUseThisComment ? R.color.colorPrimary50 : R.color.colorBooked);
                itemPhoto.setImageResource(isUserUseThisComment ? R.drawable.ic_user_default_1 : R.drawable.ic_user_default_2);
                break;
            case USED:
                itemBody.setBackgroundResource(isUserUseThisComment ? R.color.colorMySpace : R.color.colorUsed);
                itemPhoto.setImageResource(isUserUseThisComment ? R.drawable.ic_user_default_1 : R.drawable.ic_user_default_2);
                break;
        }

        itemView.setOnClickListener(itemClickListener);

        if (comment.userImage != null && !comment.status.equals(CommentStatus.FREE)) {
            Glide.with(context)
                    .load(comment.userImage)
                    .override(userImageSize, userImageSize)
                    //.thumbnail(0.4f)
                    //.fitCenter()
                    //.transform(new RoundRectTransform(context))
                    .centerCrop()
                    .into(itemPhoto);
        }
    }
}
