package com.hiddenspot.app.adapters;

import android.content.Context;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hiddenspot.app.R;
import com.hiddenspot.app.models.Review;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(Review review, int position);
    }

    public interface OnReviewClickListener {
        void onReviewClick(Review review, int position);
    }

    private final Context context;
    private final String currentUserId;
    private List<Review> reviews;
    private OnDeleteClickListener onDeleteClickListener;
    private OnReviewClickListener onReviewClickListener;
    private boolean deleteEnabled = true;

    public ReviewAdapter(Context context, List<Review> reviews, String currentUserId) {
        this.context = context;
        this.reviews = reviews;
        this.currentUserId = currentUserId;
    }

    public void updateReviews(List<Review> reviews) {
        this.reviews = reviews;
        notifyDataSetChanged();
    }

    public void setOnDeleteClickListener(OnDeleteClickListener onDeleteClickListener) {
        this.onDeleteClickListener = onDeleteClickListener;
    }

    public void setOnReviewClickListener(OnReviewClickListener onReviewClickListener) {
        this.onReviewClickListener = onReviewClickListener;
    }

    public void setDeleteEnabled(boolean deleteEnabled) {
        this.deleteEnabled = deleteEnabled;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        String name = review.getUserName() != null && !review.getUserName().trim().isEmpty()
                ? review.getUserName() : "Anonymous";
        holder.tvInitial.setText(name.substring(0, 1).toUpperCase());
        bindAvatar(holder, review.getUserAvatar());
        holder.tvName.setText(name);
        holder.tvDate.setText(review.getFormattedDate());
        String placeName = review.getGemName() != null ? review.getGemName().trim() : "";
        holder.tvPlace.setText(placeName.isEmpty() ? "" : "Post: " + placeName);
        holder.tvPlace.setVisibility(placeName.isEmpty() ? View.GONE : View.VISIBLE);
        holder.ratingBar.setRating(review.getRating());
        String comment = review.getComment() != null ? review.getComment().trim() : "";
        holder.tvComment.setText(comment);
        holder.tvComment.setVisibility(comment.isEmpty() ? View.GONE : View.VISIBLE);
        boolean canDelete = deleteEnabled
                && currentUserId != null
                && currentUserId.equals(review.getUserId());
        holder.btnDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        holder.btnDelete.setOnClickListener(v -> {
            if (canDelete && onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(review, holder.getBindingAdapterPosition());
            }
        });
        holder.itemView.setOnClickListener(v -> {
            if (onReviewClickListener != null) {
                onReviewClickListener.onReviewClick(review, holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return reviews != null ? reviews.size() : 0;
    }

    private void bindAvatar(@NonNull ReviewViewHolder holder, String avatar) {
        if (avatar != null && !avatar.trim().isEmpty()) {
            try {
                holder.ivAvatar.setVisibility(View.VISIBLE);
                holder.tvInitial.setVisibility(View.GONE);
                if (avatar.startsWith("http")) {
                    Glide.with(context).load(avatar).centerCrop().into(holder.ivAvatar);
                } else {
                    byte[] imageBytes = Base64.decode(avatar, Base64.DEFAULT);
                    Glide.with(context).load(imageBytes).centerCrop().into(holder.ivAvatar);
                }
                return;
            } catch (Exception ignored) {
                holder.ivAvatar.setImageDrawable(null);
            }
        }

        holder.ivAvatar.setVisibility(View.GONE);
        holder.ivAvatar.setImageDrawable(null);
        holder.tvInitial.setVisibility(View.VISIBLE);
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        final CircleImageView ivAvatar;
        final TextView tvInitial;
        final TextView tvName;
        final TextView tvDate;
        final TextView tvPlace;
        final TextView tvComment;
        final RatingBar ratingBar;
        final ImageButton btnDelete;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_reviewer_avatar);
            tvInitial = itemView.findViewById(R.id.tv_reviewer_initial);
            tvName = itemView.findViewById(R.id.tv_reviewer_name);
            tvDate = itemView.findViewById(R.id.tv_review_date);
            tvPlace = itemView.findViewById(R.id.tv_review_place);
            tvComment = itemView.findViewById(R.id.tv_review_comment);
            ratingBar = itemView.findViewById(R.id.rating_bar_review);
            btnDelete = itemView.findViewById(R.id.btn_delete_review);
        }
    }
}
