package com.hiddenspot.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.hiddenspot.app.R;
import com.hiddenspot.app.activities.PlaceDetailsActivity;
import com.hiddenspot.app.models.Place;

import java.util.List;
import java.util.Locale;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder> {

    private final Context context;
    private List<Place> places;
    private OnFavoriteClickListener favoriteListener;
    private OnDeleteClickListener deleteClickListener;
    private boolean showDeleteButton;

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Place place, int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Place place, int position);
    }

    public PlaceAdapter(Context context, List<Place> places) {
        this.context = context;
        this.places = places;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.favoriteListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    public void setShowDeleteButton(boolean showDeleteButton) {
        this.showDeleteButton = showDeleteButton;
    }

    public void updatePlaces(List<Place> newPlaces) {
        this.places = newPlaces;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_place_card, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        holder.bind(places.get(position), position);
    }

    @Override
    public int getItemCount() { return places != null ? places.size() : 0; }

    class PlaceViewHolder extends RecyclerView.ViewHolder {
        final CardView cardView;
        final ImageView ivPlace;
        final TextView tvCategoryBadge, tvPlaceName, tvCity, tvDescription;
        final TextView tvRating, tvRatingCount, tvUpvotes, tvDownvotes;
        final ImageButton btnFavorite, btnDeletePost;

        PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView        = (CardView) itemView;
            ivPlace         = itemView.findViewById(R.id.iv_place);
            tvCategoryBadge = itemView.findViewById(R.id.tv_category_badge);
            btnFavorite     = itemView.findViewById(R.id.btn_favorite);
            btnDeletePost   = itemView.findViewById(R.id.btn_delete_post);
            tvPlaceName     = itemView.findViewById(R.id.tv_place_name);
            tvCity          = itemView.findViewById(R.id.tv_city);
            tvDescription   = itemView.findViewById(R.id.tv_description);
            tvRating        = itemView.findViewById(R.id.tv_rating);
            tvRatingCount   = itemView.findViewById(R.id.tv_rating_count);
            tvUpvotes       = itemView.findViewById(R.id.tv_upvotes);
            tvDownvotes     = itemView.findViewById(R.id.tv_downvotes);
        }

        void bind(Place place, int position) {
            String imgData = place.getFirstImage();
            if (imgData != null && !imgData.startsWith("http") && !imgData.isEmpty()) {
                try {
                    byte[] imageBytes = Base64.decode(imgData, Base64.DEFAULT);
                    Glide.with(context)
                            .load(imageBytes)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .placeholder(R.color.muted)
                            .centerCrop()
                            .into(ivPlace);
                } catch (Exception e) {
                    ivPlace.setImageResource(R.color.muted);
                }
            } else {
                Glide.with(context).load(imgData)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.color.muted).centerCrop().into(ivPlace);
            }

            tvCategoryBadge.setText(place.getCategoryDisplay());
            tvCategoryBadge.setBackgroundColor(getCategoryColor(place.getCategory()));
            btnFavorite.setImageResource(place.isFavorited()
                    ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
            tvPlaceName.setText(place.getName());
            tvCity.setText(place.getCity());
            tvDescription.setText(place.getDescription());
            tvRating.setText(String.format(Locale.getDefault(), "%.1f", place.getRating()));
            tvRatingCount.setText("(" + place.getRatingCount() + ")");
            tvUpvotes.setText(String.valueOf(place.getUpvotes()));
            tvDownvotes.setText(String.valueOf(place.getDownvotes()));
            btnDeletePost.setVisibility(showDeleteButton ? View.VISIBLE : View.GONE);
            btnFavorite.setVisibility(showDeleteButton ? View.GONE : View.VISIBLE);

            cardView.setOnClickListener(v -> {
                context.startActivity(PlaceDetailsActivity.createIntent(context, place));
            });
            cardView.setOnLongClickListener(null);

            btnFavorite.setOnClickListener(v -> {
                if (favoriteListener != null) favoriteListener.onFavoriteClick(place, position);
            });
            btnDeletePost.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(place, position);
                }
            });
        }

        private int getCategoryColor(String cat) {
            if (cat == null) return context.getResources().getColor(R.color.primary, null);
            switch (cat) {
                case "Restaurant": return context.getResources().getColor(R.color.badge_restaurant, null);
                case "Garden":     return context.getResources().getColor(R.color.badge_garden, null);
                case "Café":       return context.getResources().getColor(R.color.badge_cafe, null);
                case "Viewpoint":  return context.getResources().getColor(R.color.badge_viewpoint, null);
                case "Park":       return context.getResources().getColor(R.color.badge_park, null);
                case "Beach":      return context.getResources().getColor(R.color.badge_beach, null);
                case "Library":    return context.getResources().getColor(R.color.badge_library, null);
                case "Shop":       return context.getResources().getColor(R.color.badge_shop, null);
                case "Historical": return context.getResources().getColor(R.color.badge_historical, null);
                default:           return context.getResources().getColor(R.color.primary, null);
            }
        }
    }
}
