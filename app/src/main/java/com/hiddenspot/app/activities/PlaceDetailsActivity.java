package com.hiddenspot.app.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.hiddenspot.app.R;
import com.hiddenspot.app.adapters.ReviewAdapter;
import com.hiddenspot.app.models.AppNotification;
import com.hiddenspot.app.models.Place;
import com.hiddenspot.app.models.Review;
import com.hiddenspot.app.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlaceDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_PLACE_ID = "place_id";
    public static final String EXTRA_PLACE_NAME = "place_name";
    public static final String EXTRA_PLACE_IMAGE = "place_image";
    public static final String EXTRA_PLACE_CITY = "place_city";
    public static final String EXTRA_PLACE_ADDRESS = "place_address";
    public static final String EXTRA_PLACE_PHONE = "place_phone";
    public static final String EXTRA_PLACE_DESC = "place_desc";
    public static final String EXTRA_PLACE_CATEGORY = "place_category";
    public static final String EXTRA_PLACE_RATING = "place_rating";
    public static final String EXTRA_PLACE_UPVOTES = "place_upvotes";
    public static final String EXTRA_PLACE_DOWNVOTES = "place_downvotes";
    public static final String EXTRA_PLACE_FAVORITED = "place_favorited";
    public static final String EXTRA_POSTER_NAME = "poster_name";
    public static final String EXTRA_POSTER_USER_ID = "poster_user_id";
    public static final String EXTRA_POSTER_AVATAR = "poster_avatar";
    public static final String EXTRA_POSTED_DATE = "posted_date";

    private String placeId;
    private String posterUserId;
    private int upvotes;
    private int downvotes;
    private String voteState = "none";
    private boolean isFavorited;
    private String placeName;

    private android.widget.ImageView ivHero;
    private TextView tvCategoryBadge;
    private TextView tvPlaceName;
    private TextView tvAddress;
    private TextView tvPhone;
    private TextView tvRating;
    private TextView tvDescription;
    private TextView tvPosterName;
    private TextView tvPostedDate;
    private TextView tvPosterInitial;
    private de.hdodenhof.circleimageview.CircleImageView ivPosterAvatar;
    private LinearLayout layoutPhone;
    private MaterialButton btnUpvote;
    private MaterialButton btnDownvote;
    private MaterialButton btnMaps;
    private MaterialButton btnCall;
    private ImageButton btnBack;
    private ImageButton btnEdit;
    private ImageButton btnShare;
    private ImageButton btnFavorite;
    private RecyclerView rvReviews;
    private ReviewAdapter reviewAdapter;
    private final List<Review> reviewList = new ArrayList<>();
    private RatingBar ratingBarInput;
    private TextInputEditText etReviewComment;
    private MaterialButton btnSubmitReview;
    private TextView tvReviewCount;
    private TextView tvNoReviews;
    private float myRating = 0f;

    public static Intent createIntent(android.content.Context context, Place place) {
        Intent i = new Intent(context, PlaceDetailsActivity.class);
        i.putExtra(EXTRA_PLACE_ID, place.getId());
        i.putExtra(EXTRA_PLACE_NAME, place.getName());
        i.putExtra(EXTRA_PLACE_IMAGE, place.getFirstImage());
        i.putExtra(EXTRA_PLACE_CITY, place.getCity());
        i.putExtra(EXTRA_PLACE_ADDRESS, place.getAddress());
        i.putExtra(EXTRA_PLACE_PHONE, place.getPhone());
        i.putExtra(EXTRA_PLACE_DESC, place.getDescription());
        i.putExtra(EXTRA_PLACE_CATEGORY, place.getCategory());
        i.putExtra(EXTRA_PLACE_RATING, place.getRating());
        i.putExtra(EXTRA_PLACE_UPVOTES, place.getUpvotes());
        i.putExtra(EXTRA_PLACE_DOWNVOTES, place.getDownvotes());
        i.putExtra(EXTRA_PLACE_FAVORITED, place.isFavorited());
        i.putExtra(EXTRA_POSTER_NAME, place.getUserName());
        i.putExtra(EXTRA_POSTER_USER_ID, place.getUserId());
        i.putExtra(EXTRA_POSTER_AVATAR, place.getUserAvatar());
        i.putExtra(EXTRA_POSTED_DATE, place.getFormattedDate());
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_details);
        bindViews();
        populateFromIntent();
        setupListeners();
        loadCurrentVoteState();
        loadReviews();
        checkMyExistingReview();
    }

    private void bindViews() {
        ivHero = findViewById(R.id.iv_hero);
        tvCategoryBadge = findViewById(R.id.tv_category_badge);
        tvPlaceName = findViewById(R.id.tv_place_name);
        tvAddress = findViewById(R.id.tv_address);
        tvPhone = findViewById(R.id.tv_phone);
        tvRating = findViewById(R.id.tv_rating);
        tvDescription = findViewById(R.id.tv_description);
        tvPosterName = findViewById(R.id.tv_poster_name);
        tvPostedDate = findViewById(R.id.tv_posted_date);
        tvPosterInitial = findViewById(R.id.tv_poster_initial);
        ivPosterAvatar = findViewById(R.id.iv_poster_avatar);
        layoutPhone = findViewById(R.id.layout_phone);
        btnUpvote = findViewById(R.id.btn_upvote);
        btnDownvote = findViewById(R.id.btn_downvote);
        btnMaps = findViewById(R.id.btn_maps);
        btnCall = findViewById(R.id.btn_call);
        btnBack = findViewById(R.id.btn_back);
        btnEdit = findViewById(R.id.btn_edit);
        btnShare = findViewById(R.id.btn_share);
        btnFavorite = findViewById(R.id.btn_favorite);
        rvReviews = findViewById(R.id.rv_reviews);
        ratingBarInput = findViewById(R.id.rating_bar_input);
        etReviewComment = findViewById(R.id.et_review_comment);
        btnSubmitReview = findViewById(R.id.btn_submit_review);
        tvReviewCount = findViewById(R.id.tv_review_count);
        tvNoReviews = findViewById(R.id.tv_no_reviews);

        if (rvReviews != null) {
            reviewAdapter = new ReviewAdapter(this, reviewList, uid());
            rvReviews.setLayoutManager(new LinearLayoutManager(this));
            rvReviews.setAdapter(reviewAdapter);
            rvReviews.setNestedScrollingEnabled(false);
            reviewAdapter.setOnDeleteClickListener(this::confirmDeleteReview);
        }
    }

    private void populateFromIntent() {
        Intent intent = getIntent();
        placeId = intent.getStringExtra(EXTRA_PLACE_ID);
        upvotes = intent.getIntExtra(EXTRA_PLACE_UPVOTES, 0);
        downvotes = intent.getIntExtra(EXTRA_PLACE_DOWNVOTES, 0);
        isFavorited = intent.getBooleanExtra(EXTRA_PLACE_FAVORITED, false);

        String name = intent.getStringExtra(EXTRA_PLACE_NAME);
        placeName = name;
        String image = intent.getStringExtra(EXTRA_PLACE_IMAGE);
        String city = intent.getStringExtra(EXTRA_PLACE_CITY);
        String address = intent.getStringExtra(EXTRA_PLACE_ADDRESS);
        String phone = intent.getStringExtra(EXTRA_PLACE_PHONE);
        String desc = intent.getStringExtra(EXTRA_PLACE_DESC);
        String category = intent.getStringExtra(EXTRA_PLACE_CATEGORY);
        double rating = intent.getDoubleExtra(EXTRA_PLACE_RATING, 0.0);
        String poster = intent.getStringExtra(EXTRA_POSTER_NAME);
        posterUserId = intent.getStringExtra(EXTRA_POSTER_USER_ID);
        String posterAvatar = intent.getStringExtra(EXTRA_POSTER_AVATAR);
        String date = intent.getStringExtra(EXTRA_POSTED_DATE);

        if (image != null && !image.isEmpty()) {
            if (!image.startsWith("http")) {
                try {
                    byte[] imageBytes = Base64.decode(image, Base64.DEFAULT);
                    Glide.with(this).load(imageBytes)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .centerCrop()
                            .into(ivHero);
                } catch (Exception e) {
                    ivHero.setImageResource(R.color.muted);
                }
            } else {
                Glide.with(this).load(image)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(ivHero);
            }
        }

        tvCategoryBadge.setText(getCategoryDisplay(category));
        tvCategoryBadge.setBackgroundColor(getCategoryColor(category));
        tvPlaceName.setText(name);
        tvAddress.setText((address != null ? address : "") + (city != null ? ", " + city : ""));
        tvRating.setText(String.format(Locale.getDefault(), "%.1f", rating));
        tvDescription.setText(desc);

        if (phone != null && !phone.isEmpty()) {
            layoutPhone.setVisibility(View.VISIBLE);
            btnCall.setVisibility(View.VISIBLE);
            tvPhone.setText(phone);
            final String p = phone;
            tvPhone.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + p))));
            btnCall.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + p))));
        }

        if (poster != null && !poster.isEmpty()) {
            tvPosterName.setText(poster);
            tvPosterInitial.setText(poster.substring(0, 1).toUpperCase(Locale.getDefault()));
        }
        updatePosterAvatar(posterAvatar);
        if (date != null) {
            tvPostedDate.setText("Posted on " + date);
        }

        updateEditButtonVisibility();
        updateVoteButtons();
        updateFavoriteIcon();
        updateReviewComposerState();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> openEditPost());
        }

        btnShare.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, "Check out: " + tvPlaceName.getText() + " via HiddenSpot");
            startActivity(Intent.createChooser(i, "Share via"));
        });

        btnFavorite.setOnClickListener(v -> {
            isFavorited = !isFavorited;
            updateFavoriteIcon();
            String uid = uid();
            if (uid != null && placeId != null) {
                if (isFavorited) {
                    FirebaseHelper.getInstance().saveGem(uid, placeId, s -> {}, e -> {});
                    Toast.makeText(this, "Saved to favorites!", Toast.LENGTH_SHORT).show();
                } else {
                    FirebaseHelper.getInstance().unsaveGem(uid, placeId, s -> {}, e -> {});
                    Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnUpvote.setOnClickListener(v -> handleVote("up"));
        btnDownvote.setOnClickListener(v -> handleVote("down"));

        btnMaps.setOnClickListener(v -> {
            String addr = tvAddress.getText().toString();
            Uri gmmUri = Uri.parse("geo:0,0?q=" + Uri.encode(addr));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://maps.google.com/?q=" + Uri.encode(addr))));
            }
        });

        if (ratingBarInput != null) {
            ratingBarInput.setOnRatingBarChangeListener((bar, rating, fromUser) -> myRating = rating);
        }

        if (btnSubmitReview != null) {
            btnSubmitReview.setOnClickListener(v -> submitReview());
        }
    }

    private void handleVote(String type) {
        String uid = uid();
        if (placeId == null || uid == null) return;

        String previousVoteState = voteState;
        int previousUpvotes = upvotes;
        int previousDownvotes = downvotes;
        String newVoteState = voteState.equals(type) ? "none" : type;
        applyVoteStateChange(voteState, newVoteState);
        updateVoteButtons();
        btnUpvote.setEnabled(false);
        btnDownvote.setEnabled(false);

        FirebaseHelper.getInstance().setGemVote(placeId, uid,
                "none".equals(newVoteState) ? null : toVoteValue(newVoteState),
                v -> runOnUiThread(() -> {
                    maybeCreateVoteNotification(uid, previousVoteState, newVoteState);
                    btnUpvote.setEnabled(true);
                    btnDownvote.setEnabled(true);
                }),
                e -> runOnUiThread(() -> {
                    voteState = previousVoteState;
                    upvotes = previousUpvotes;
                    downvotes = previousDownvotes;
                    updateVoteButtons();
                    btnUpvote.setEnabled(true);
                    btnDownvote.setEnabled(true);
                    Toast.makeText(this, e.getMessage() != null ? e.getMessage() : "Failed to update vote",
                            Toast.LENGTH_SHORT).show();
                }));
    }

    private void updateVoteButtons() {
        btnUpvote.setText(String.valueOf(upvotes));
        btnDownvote.setText(String.valueOf(downvotes));
        if ("up".equals(voteState)) {
            btnUpvote.setBackgroundResource(R.drawable.bg_vote_up_active);
            btnUpvote.setTextColor(getResources().getColor(R.color.upvote_green, null));
            btnUpvote.setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.upvote_green, null)));
        } else {
            btnUpvote.setBackgroundResource(R.drawable.bg_chip_inactive);
            btnUpvote.setTextColor(getResources().getColor(R.color.muted_foreground, null));
            btnUpvote.setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.muted_foreground, null)));
        }
        if ("down".equals(voteState)) {
            btnDownvote.setBackgroundResource(R.drawable.bg_vote_down_active);
            btnDownvote.setTextColor(getResources().getColor(R.color.downvote_red, null));
            btnDownvote.setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.downvote_red, null)));
        } else {
            btnDownvote.setBackgroundResource(R.drawable.bg_chip_inactive);
            btnDownvote.setTextColor(getResources().getColor(R.color.muted_foreground, null));
            btnDownvote.setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.muted_foreground, null)));
        }
    }

    private void updateFavoriteIcon() {
        btnFavorite.setImageResource(isFavorited ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
    }

    private void loadCurrentVoteState() {
        String uid = uid();
        if (placeId == null || uid == null) return;

        FirebaseHelper.getInstance().fetchGemVote(placeId, uid, snap -> runOnUiThread(() -> {
            String storedVote = snap.getString("value");
            if ("upvote".equals(storedVote)) voteState = "up";
            else if ("downvote".equals(storedVote)) voteState = "down";
            else voteState = "none";
            updateVoteButtons();
        }), e -> runOnUiThread(this::updateVoteButtons));
    }

    private void applyVoteStateChange(String oldState, String newState) {
        if ("up".equals(oldState)) upvotes = Math.max(0, upvotes - 1);
        if ("down".equals(oldState)) downvotes = Math.max(0, downvotes - 1);
        if ("up".equals(newState)) upvotes += 1;
        if ("down".equals(newState)) downvotes += 1;
        voteState = newState;
    }

    private String toVoteValue(String state) {
        return "up".equals(state) ? "upvote" : "downvote";
    }

    private String uid() {
        return FirebaseHelper.getInstance().getCurrentUser() != null
                ? FirebaseHelper.getInstance().getCurrentUser().getUid() : null;
    }

    private void updateEditButtonVisibility() {
        if (btnEdit == null) return;
        String uid = uid();
        boolean isOwnPost = uid != null && uid.equals(posterUserId);
        btnEdit.setVisibility(isOwnPost ? View.VISIBLE : View.GONE);
    }

    private void openEditPost() {
        String uid = uid();
        if (uid == null || placeId == null || !uid.equals(posterUserId)) {
            return;
        }
        FirebaseHelper.getInstance().fetchGemById(placeId, snap -> {
            if (!snap.exists()) {
                runOnUiThread(() -> Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show());
                return;
            }
            Place place = snap.toObject(Place.class);
            if (place == null) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to open post", Toast.LENGTH_SHORT).show());
                return;
            }

            if (place.getCreatedAt() != null) {
                long ageMillis = System.currentTimeMillis() - place.getCreatedAt().toDate().getTime();
                if (ageMillis > TimeUnit.MINUTES.toMillis(30)) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "Posts can't be edited after 30 minutes",
                            Toast.LENGTH_SHORT).show());
                    return;
                }
            }

            place.setId(snap.getId());
            runOnUiThread(() -> startActivity(AddPlaceActivity.createEditIntent(this, place)));
        }, e -> runOnUiThread(() -> Toast.makeText(this,
                e.getMessage() != null ? e.getMessage() : "Failed to open editor",
                Toast.LENGTH_SHORT).show()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPlaceDetails();
    }

    private void refreshPlaceDetails() {
        if (placeId == null || placeId.trim().isEmpty()) return;

        FirebaseHelper.getInstance().fetchGemById(placeId, snap -> {
            if (!snap.exists()) {
                return;
            }

            Place place = snap.toObject(Place.class);
            if (place == null) {
                return;
            }
            place.setId(snap.getId());

            runOnUiThread(() -> {
                placeName = place.getName();
                posterUserId = place.getUserId();
                upvotes = place.getUpvotes();
                downvotes = place.getDownvotes();

                tvCategoryBadge.setText(getCategoryDisplay(place.getCategory()));
                tvCategoryBadge.setBackgroundColor(getCategoryColor(place.getCategory()));
                tvPlaceName.setText(place.getName());

                String address = place.getAddress() != null ? place.getAddress() : "";
                String city = place.getCity() != null ? place.getCity() : "";
                tvAddress.setText(address + (city.isEmpty() ? "" : ", " + city));
                tvRating.setText(String.format(Locale.getDefault(), "%.1f", place.getRating()));
                tvDescription.setText(place.getDescription());

                String phone = place.getPhone();
                if (phone != null && !phone.trim().isEmpty()) {
                    layoutPhone.setVisibility(View.VISIBLE);
                    btnCall.setVisibility(View.VISIBLE);
                    tvPhone.setText(phone);
                    tvPhone.setOnClickListener(v -> startActivity(
                            new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))));
                    btnCall.setOnClickListener(v -> startActivity(
                            new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))));
                } else {
                    layoutPhone.setVisibility(View.GONE);
                    btnCall.setVisibility(View.GONE);
                }

                String posterName = place.getUserName();
                if (posterName != null && !posterName.isEmpty()) {
                    tvPosterName.setText(posterName);
                    tvPosterInitial.setText(posterName.substring(0, 1).toUpperCase(Locale.getDefault()));
                }
                updatePosterAvatar(place.getUserAvatar());
                tvPostedDate.setText("Posted on " + place.getFormattedDate());

                String image = place.getFirstImage();
                if (image != null && !image.isEmpty()) {
                    if (!image.startsWith("http")) {
                        try {
                            byte[] imageBytes = Base64.decode(image, Base64.DEFAULT);
                            Glide.with(this).load(imageBytes)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .centerCrop()
                                    .into(ivHero);
                        } catch (Exception e) {
                            ivHero.setImageResource(R.color.muted);
                        }
                    } else {
                        Glide.with(this).load(image)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .centerCrop()
                                .into(ivHero);
                    }
                } else {
                    ivHero.setImageResource(R.color.muted);
                }

                updateEditButtonVisibility();
                updateVoteButtons();
            });
        }, e -> { });
    }

    private void updatePosterAvatar(String posterAvatar) {
        if (posterAvatar != null && !posterAvatar.trim().isEmpty()) {
            try {
                tvPosterInitial.setVisibility(View.GONE);
                if (posterAvatar.startsWith("http")) {
                    Glide.with(this).load(posterAvatar).centerCrop().into(ivPosterAvatar);
                } else {
                    byte[] imageBytes = Base64.decode(posterAvatar, Base64.DEFAULT);
                    Glide.with(this).load(imageBytes).centerCrop().into(ivPosterAvatar);
                }
            } catch (Exception e) {
                ivPosterAvatar.setImageDrawable(null);
                tvPosterInitial.setVisibility(View.VISIBLE);
            }
        } else {
            ivPosterAvatar.setImageDrawable(null);
            tvPosterInitial.setVisibility(View.VISIBLE);
        }
    }

    private void loadReviews() {
        if (placeId == null || reviewAdapter == null) return;

        FirebaseHelper.getInstance().fetchReviewsForGem(placeId, snap -> {
            reviewList.clear();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Review review = doc.toObject(Review.class);
                if (review != null) {
                    review.setId(doc.getId());
                    reviewList.add(review);
                }
            }

            runOnUiThread(() -> {
                reviewAdapter.updateReviews(reviewList);
                if (tvReviewCount != null) {
                    int count = reviewList.size();
                    tvReviewCount.setText(count + " review" + (count == 1 ? "" : "s"));
                }
                if (tvNoReviews != null) {
                    tvNoReviews.setText(getNoReviewsMessage());
                    tvNoReviews.setVisibility(reviewList.isEmpty() ? View.VISIBLE : View.GONE);
                }
                if (rvReviews != null) {
                    rvReviews.setVisibility(reviewList.isEmpty() ? View.GONE : View.VISIBLE);
                }
            });
        }, e -> runOnUiThread(() -> {
            if (tvNoReviews != null) {
                tvNoReviews.setText(getNoReviewsMessage());
                tvNoReviews.setVisibility(View.VISIBLE);
            }
            if (rvReviews != null) rvReviews.setVisibility(View.GONE);
        }));
    }

    private void confirmDeleteReview(Review review, int position) {
        if (review == null || position == RecyclerView.NO_POSITION) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete Review")
                .setMessage("Delete this review?")
                .setPositiveButton("Delete", (dialog, which) -> deleteReview(review, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteReview(Review review, int position) {
        if (position < 0 || position >= reviewList.size()) return;
        Review removed = reviewList.remove(position);
        reviewAdapter.updateReviews(reviewList);
        int count = reviewList.size();
        if (tvReviewCount != null) tvReviewCount.setText(count + " review" + (count == 1 ? "" : "s"));
        if (tvNoReviews != null) {
            tvNoReviews.setText(getNoReviewsMessage());
            tvNoReviews.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        }
        if (rvReviews != null) rvReviews.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        if (ratingBarInput != null) ratingBarInput.setRating(0f);
        if (etReviewComment != null) etReviewComment.setText("");
        myRating = 0f;
        if (btnSubmitReview != null) btnSubmitReview.setText("Submit Review");

        FirebaseHelper.getInstance().deleteReview(removed, v -> runOnUiThread(() ->
                        Toast.makeText(this, "Review deleted", Toast.LENGTH_SHORT).show()),
                e -> runOnUiThread(() -> {
                    reviewList.add(position, removed);
                    reviewAdapter.updateReviews(reviewList);
                    int restoredCount = reviewList.size();
                    if (tvReviewCount != null) {
                        tvReviewCount.setText(restoredCount + " review" + (restoredCount == 1 ? "" : "s"));
                    }
                    if (tvNoReviews != null) {
                        tvNoReviews.setText(getNoReviewsMessage());
                        tvNoReviews.setVisibility(restoredCount == 0 ? View.VISIBLE : View.GONE);
                    }
                    if (rvReviews != null) rvReviews.setVisibility(restoredCount == 0 ? View.GONE : View.VISIBLE);
                    checkMyExistingReview();
                    Toast.makeText(this,
                            e.getMessage() != null ? e.getMessage() : "Failed to delete review",
                            Toast.LENGTH_SHORT).show();
                }));
    }

    private void updateReviewComposerState() {
        String uid = uid();
        boolean isOwnPost = uid != null && uid.equals(posterUserId);

        if (ratingBarInput != null) ratingBarInput.setVisibility(isOwnPost ? View.GONE : View.VISIBLE);
        if (etReviewComment != null) {
            View parent = (View) etReviewComment.getParent().getParent();
            parent.setVisibility(isOwnPost ? View.GONE : View.VISIBLE);
        }
        if (btnSubmitReview != null) btnSubmitReview.setVisibility(isOwnPost ? View.GONE : View.VISIBLE);

        if (isOwnPost && tvReviewCount != null) {
            tvReviewCount.setText("You can't rate your own post");
        }
        if (tvNoReviews != null) {
            tvNoReviews.setText(getNoReviewsMessage());
        }
    }

    private String getNoReviewsMessage() {
        String uid = uid();
        boolean isOwnPost = uid != null && uid.equals(posterUserId);
        return isOwnPost
                ? "No one reviewed your post yet!"
                : "No reviews yet.";
    }

    private void checkMyExistingReview() {
        String uid = uid();
        if (uid == null || placeId == null || ratingBarInput == null) return;

        FirebaseHelper.getInstance().fetchMyReview(uid, placeId, existing -> {
            if (existing == null) return;
            runOnUiThread(() -> {
                ratingBarInput.setRating(existing.getRating());
                myRating = existing.getRating();
                if (etReviewComment != null && existing.getComment() != null) {
                    etReviewComment.setText(existing.getComment());
                }
                if (btnSubmitReview != null) {
                    btnSubmitReview.setText("Update Review");
                }
            });
        }, e -> { });
    }

    private void submitReview() {
        String uid = uid();
        if (uid == null) {
            Toast.makeText(this, "Please log in to leave a review", Toast.LENGTH_SHORT).show();
            return;
        }
        if (myRating == 0f) {
            Toast.makeText(this, "Please select a star rating", Toast.LENGTH_SHORT).show();
            return;
        }
        if (uid.equals(posterUserId)) {
            Toast.makeText(this, "You can't rate your own post", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = etReviewComment != null && etReviewComment.getText() != null
                ? etReviewComment.getText().toString().trim() : "";
        String userName = FirebaseHelper.getInstance().getCurrentUser() != null
                ? FirebaseHelper.getInstance().getCurrentUser().getDisplayName() : "Anonymous";
        if (userName == null || userName.trim().isEmpty()) userName = "Anonymous";

        if (btnSubmitReview != null) {
            btnSubmitReview.setEnabled(false);
            btnSubmitReview.setText("Saving...");
        }

        String fallbackAvatar = FirebaseHelper.getInstance().getCurrentUser() != null
                && FirebaseHelper.getInstance().getCurrentUser().getPhotoUrl() != null
                ? FirebaseHelper.getInstance().getCurrentUser().getPhotoUrl().toString() : "";

        String finalUserName = userName;
        FirebaseHelper.getInstance().fetchUserProfile(uid, snap -> {
            String avatar = snap.getString("avatarUrl");
            if (avatar == null || avatar.trim().isEmpty()) avatar = fallbackAvatar;

            Review review = new Review(placeId, uid, finalUserName, avatar, myRating, comment);
            FirebaseHelper.getInstance().addReview(review, v -> runOnUiThread(() -> {
                maybeCreateReviewNotification(uid, finalUserName);
                Toast.makeText(this, "Review saved!", Toast.LENGTH_SHORT).show();
                if (btnSubmitReview != null) {
                    btnSubmitReview.setEnabled(true);
                    btnSubmitReview.setText("Update Review");
                }
                loadReviews();
            }), e -> runOnUiThread(() -> {
                Toast.makeText(this, e.getMessage() != null ? e.getMessage() : "Failed to save review",
                        Toast.LENGTH_SHORT).show();
                if (btnSubmitReview != null) {
                    btnSubmitReview.setEnabled(true);
                    btnSubmitReview.setText("Submit Review");
                }
            }));
        }, e -> {
            Review review = new Review(placeId, uid, finalUserName, fallbackAvatar, myRating, comment);
            FirebaseHelper.getInstance().addReview(review, v -> runOnUiThread(() -> {
                maybeCreateReviewNotification(uid, finalUserName);
                Toast.makeText(this, "Review saved!", Toast.LENGTH_SHORT).show();
                if (btnSubmitReview != null) {
                    btnSubmitReview.setEnabled(true);
                    btnSubmitReview.setText("Update Review");
                }
                loadReviews();
            }), err -> runOnUiThread(() -> {
                Toast.makeText(this, err.getMessage() != null ? err.getMessage() : "Failed to save review",
                        Toast.LENGTH_SHORT).show();
                if (btnSubmitReview != null) {
                    btnSubmitReview.setEnabled(true);
                    btnSubmitReview.setText("Submit Review");
                }
            }));
        });
    }

    private void maybeCreateVoteNotification(String actorUserId, String oldState, String newState) {
        if (!"up".equals(newState) || "up".equals(oldState) || actorUserId == null || placeId == null) return;
        if (posterUserId == null || posterUserId.equals(actorUserId)) return;

        String actorName = FirebaseHelper.getInstance().getCurrentUser() != null
                ? FirebaseHelper.getInstance().getCurrentUser().getDisplayName() : "Someone";
        if (actorName == null || actorName.trim().isEmpty()) actorName = "Someone";

        AppNotification notification = new AppNotification(
                posterUserId,
                actorUserId,
                actorName,
                "upvote",
                placeId,
                placeName,
                actorName + " upvoted your post " + (placeName != null ? "\"" + placeName + "\"" : "")
        );
        FirebaseHelper.getInstance().createNotification(notification, s -> { }, e -> { });
    }

    private void maybeCreateReviewNotification(String actorUserId, String actorName) {
        if (actorUserId == null || placeId == null) return;
        if (posterUserId == null || posterUserId.equals(actorUserId)) return;
        if (actorName == null || actorName.trim().isEmpty()) actorName = "Someone";

        AppNotification notification = new AppNotification(
                posterUserId,
                actorUserId,
                actorName,
                "review",
                placeId,
                placeName,
                actorName + " reviewed your post " + (placeName != null ? "\"" + placeName + "\"" : "")
        );
        FirebaseHelper.getInstance().createNotification(notification, s -> { }, e -> { });
    }

    private String getCategoryDisplay(String cat) {
        if (cat == null) return "";
        switch (cat) {
            case "Restaurant": return "Restaurant";
            case "Garden": return "Garden";
            case "Café": return "Café";
            case "Viewpoint": return "Viewpoint";
            case "Park": return "Park";
            case "Beach": return "Beach";
            case "Library": return "Library";
            case "Shop": return "Shop";
            case "Historical": return "Historical";
            default: return cat;
        }
    }

    private int getCategoryColor(String cat) {
        if (cat == null) return getResources().getColor(R.color.primary, null);
        switch (cat) {
            case "Restaurant": return getResources().getColor(R.color.badge_restaurant, null);
            case "Garden": return getResources().getColor(R.color.badge_garden, null);
            case "Café": return getResources().getColor(R.color.badge_cafe, null);
            case "Viewpoint": return getResources().getColor(R.color.badge_viewpoint, null);
            case "Park": return getResources().getColor(R.color.badge_park, null);
            case "Beach": return getResources().getColor(R.color.badge_beach, null);
            case "Library": return getResources().getColor(R.color.badge_library, null);
            case "Shop": return getResources().getColor(R.color.badge_shop, null);
            case "Historical": return getResources().getColor(R.color.badge_historical, null);
            default: return getResources().getColor(R.color.primary, null);
        }
    }
}
