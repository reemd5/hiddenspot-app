package com.hiddenspot.app.utils;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hiddenspot.app.models.AppNotification;
import com.hiddenspot.app.models.Place;
import com.hiddenspot.app.models.Review;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FirebaseHelper {

    private static FirebaseHelper instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final FirebaseStorage storage;

    public static final String COLLECTION_GEMS = "gems"; //posts
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_SAVES = "saves"; //favorites
    public static final String COLLECTION_REVIEWS = "reviews";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";
    public static final String SUBCOLLECTION_VOTES = "votes"; //upvote, downvote

    private FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) instance = new FirebaseHelper();
        return instance;
    }

    public FirebaseAuth getAuth() { return auth; }
    public FirebaseFirestore getDb() { return db; }
    public FirebaseUser getCurrentUser() { return auth.getCurrentUser(); }

    public void signIn(String email, String password,
                       OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> onSuccess.onSuccess(null))
                .addOnFailureListener(onFailure);
    }

    public void signUp(String email, String password, String username,
                       OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> {
                    FirebaseUser firebaseUser = r.getUser();
                    if (firebaseUser == null) {
                        onFailure.onFailure(new IllegalStateException("User creation failed"));
                        return;
                    }
                    String uid = firebaseUser.getUid();
                    // to store user data as key-value pairs
                    Map<String, Object> user = new HashMap<>();
                    user.put("uid", uid);
                    user.put("displayName", username);
                    user.put("email", email);
                    user.put("bio", "");
                    user.put("avatarUrl", "");
                    user.put("gemsCount", 0);
                    firebaseUser.updateProfile(new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build())
                            .addOnSuccessListener(v ->
                                    db.collection(COLLECTION_USERS).document(uid).set(user)
                                            .addOnSuccessListener(v2 -> onSuccess.onSuccess(null))
                                            .addOnFailureListener(onFailure))
                            .addOnFailureListener(onFailure);
                }).addOnFailureListener(onFailure);
    }

    public void signOut() {
        auth.signOut();
    }

    public void createUserDocument(String uid, String email, String username,
                                   OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("displayName", username);
        user.put("email", email);
        user.put("bio", "");
        user.put("avatarUrl", "");
        user.put("gemsCount", 0);
        db.collection(COLLECTION_USERS).document(uid)
                .set(user)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void fetchAllGems(OnSuccessListener<QuerySnapshot> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_GEMS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void fetchGemsByUser(String userId,
                                OnSuccessListener<QuerySnapshot> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_GEMS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void fetchGemById(String gemId,
                             OnSuccessListener<DocumentSnapshot> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_GEMS).document(gemId)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void deleteGem(String gemId,
                          OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        String currentUid = getCurrentUser() != null ? getCurrentUser().getUid() : null;
        if (currentUid == null) {
            onFailure.onFailure(new IllegalStateException("Not authenticated"));
            return;
        }

        DocumentReference gemRef = db.collection(COLLECTION_GEMS).document(gemId);
        gemRef.get()
                .addOnSuccessListener(gemDoc -> {
                    if (!gemDoc.exists()) {
                        onFailure.onFailure(new IllegalStateException("Post not found"));
                        return;
                    }

                    String ownerId = gemDoc.getString("userId");
                    if (ownerId == null || !ownerId.equals(currentUid)) {
                        onFailure.onFailure(new IllegalStateException("You can only delete your own posts"));
                        return;
                    }
                    gemRef.delete()
                            .addOnSuccessListener(onSuccess)
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    public void addGem(Place place,
                       OnSuccessListener<DocumentReference> onSuccess, OnFailureListener onFailure) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", place.getName());
        data.put("city", place.getCity());
        data.put("address", place.getAddress());
        data.put("phone", place.getPhone());
        data.put("description", place.getDescription());
        data.put("category", place.getCategory());
        data.put("images", place.getImages());
        data.put("userId", place.getUserId());
        data.put("userName", place.getUserName());
        data.put("userAvatar", place.getUserAvatar());
        data.put("rating", place.getRating());
        data.put("ratingCount", place.getRatingCount());
        data.put("likesCount", place.getLikesCount());
        data.put("upvotes", place.getUpvotes());
        data.put("downvotes", place.getDownvotes());
        data.put("status", "pending");
        data.put("isVerified", false);
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_GEMS)
                .add(data)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void updateGem(String gemId, Place place,
                          OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        String currentUid = getCurrentUser() != null ? getCurrentUser().getUid() : null;
        if (currentUid == null) {
            onFailure.onFailure(new IllegalStateException("Not authenticated"));
            return;
        }

        DocumentReference gemRef = db.collection(COLLECTION_GEMS).document(gemId);
        gemRef.get()
                .addOnSuccessListener(gemDoc -> {
                    if (!gemDoc.exists()) {
                        onFailure.onFailure(new IllegalStateException("Post not found"));
                        return;
                    }

                    String ownerId = gemDoc.getString("userId");
                    if (ownerId == null || !ownerId.equals(currentUid)) {
                        onFailure.onFailure(new IllegalStateException("You can only edit your own posts"));
                        return;
                    }

                    com.google.firebase.Timestamp createdAt = gemDoc.getTimestamp("createdAt");
                    if (createdAt != null) {
                        long ageMillis = System.currentTimeMillis() - createdAt.toDate().getTime();
                        if (ageMillis > TimeUnit.MINUTES.toMillis(30)) {
                            onFailure.onFailure(new IllegalStateException("Posts can only be edited within 30 minutes"));
                            return;
                        }
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", place.getName());
                    updates.put("city", place.getCity());
                    updates.put("address", place.getAddress());
                    updates.put("phone", place.getPhone());
                    updates.put("description", place.getDescription());
                    updates.put("category", place.getCategory());
                    updates.put("images", place.getImages());
                    updates.put("userName", place.getUserName());
                    updates.put("userAvatar", place.getUserAvatar());
                    gemRef.update(updates)
                            .addOnSuccessListener(onSuccess)
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    public void fetchGemVote(String gemId, String userId,
                             OnSuccessListener<DocumentSnapshot> onSuccess,
                             OnFailureListener onFailure) {
        db.collection(COLLECTION_GEMS).document(gemId)
                .collection(SUBCOLLECTION_VOTES).document(userId)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // Updates a user's vote using a transaction: removes old vote, applies new one,
    // updates upvote/downvote counts, and saves or deletes the vote.
    // Ensures counts stay correct even with concurrent users.
    public void setGemVote(String gemId, String userId, String newVote,
                           OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        DocumentReference gemRef = db.collection(COLLECTION_GEMS).document(gemId);
        DocumentReference voteRef = gemRef.collection(SUBCOLLECTION_VOTES).document(userId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot gemSnap = transaction.get(gemRef);
            DocumentSnapshot voteSnap = transaction.get(voteRef);

            String currentVote = voteSnap.exists() ? voteSnap.getString("value") : null;
            long upvotes = gemSnap.contains("upvotes") && gemSnap.getLong("upvotes") != null
                    ? gemSnap.getLong("upvotes") : 0L;
            long downvotes = gemSnap.contains("downvotes") && gemSnap.getLong("downvotes") != null
                    ? gemSnap.getLong("downvotes") : 0L;

            if ("upvote".equals(currentVote)) upvotes = Math.max(0L, upvotes - 1L);
            if ("downvote".equals(currentVote)) downvotes = Math.max(0L, downvotes - 1L);

            if ("upvote".equals(newVote)) upvotes += 1L;
            if ("downvote".equals(newVote)) downvotes += 1L;

            Map<String, Object> gemUpdates = new HashMap<>();
            gemUpdates.put("upvotes", upvotes);
            gemUpdates.put("downvotes", downvotes);
            transaction.update(gemRef, gemUpdates);

            if (newVote == null || newVote.isEmpty()) {
                transaction.delete(voteRef);
            } else {
                Map<String, Object> voteData = new HashMap<>();
                voteData.put("userId", userId);
                voteData.put("value", newVote);
                voteData.put("updatedAt", FieldValue.serverTimestamp());
                transaction.set(voteRef, voteData);
            }
            return null;
        }).addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
    }

    public void saveGem(String userId, String gemId,
                        OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        Map<String, Object> save = new HashMap<>();
        save.put("userId", userId);
        save.put("gemId", gemId);
        save.put("collectionName", "Default");
        save.put("savedAt", FieldValue.serverTimestamp());
        db.collection(COLLECTION_SAVES).document(userId + "_" + gemId)
                .set(save)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void unsaveGem(String userId, String gemId,
                          OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_SAVES).document(userId + "_" + gemId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void fetchSavedGemIds(String userId,
                                 OnSuccessListener<QuerySnapshot> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_SAVES)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void addReview(Review review,
                          OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        Map<String, Object> data = new HashMap<>();
        data.put("gemId", review.getGemId());
        data.put("userId", review.getUserId());
        data.put("userName", review.getUserName());
        data.put("userAvatar", review.getUserAvatar());
        data.put("rating", review.getRating());
        data.put("comment", review.getComment());
        data.put("createdAt", FieldValue.serverTimestamp());

        String docId = review.getUserId() + "_" + review.getGemId();
        db.collection(COLLECTION_GEMS).document(review.getGemId()).get()
                .addOnSuccessListener(gemDoc -> {
                    String ownerId = gemDoc.getString("userId");
                    // only allow others to rate one's post
                    if (ownerId != null && ownerId.equals(review.getUserId())) {
                        onFailure.onFailure(new IllegalStateException("You can't rate your own post"));
                        return;
                    }

                    db.collection(COLLECTION_REVIEWS).document(docId)
                            .set(data)
                            .addOnSuccessListener(v -> recalculateRating(review.getGemId(), onSuccess, onFailure))
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    public void fetchMyReview(String userId, String gemId,
                              OnSuccessListener<Review> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_REVIEWS).document(userId + "_" + gemId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        onSuccess.onSuccess(null);
                        return;
                    }
                    Review review = doc.toObject(Review.class);
                    if (review != null) review.setId(doc.getId());
                    onSuccess.onSuccess(review);
                })
                .addOnFailureListener(onFailure);
    }

    public void fetchReviewsForGem(String gemId,
                                   OnSuccessListener<QuerySnapshot> onSuccess,
                                   OnFailureListener onFailure) {
        db.collection(COLLECTION_REVIEWS)
                .whereEqualTo("gemId", gemId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void deleteReview(Review review,
                             OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (review == null || review.getUserId() == null || review.getGemId() == null) {
            onFailure.onFailure(new IllegalArgumentException("Invalid review"));
            return;
        }

        String currentUid = getCurrentUser() != null ? getCurrentUser().getUid() : null;
        if (currentUid == null || !currentUid.equals(review.getUserId())) {
            onFailure.onFailure(new IllegalStateException("You can only delete your own review"));
            return;
        }

        String docId = review.getUserId() + "_" + review.getGemId();
        db.collection(COLLECTION_REVIEWS).document(docId)
                .delete()
                .addOnSuccessListener(v -> recalculateRating(review.getGemId(), onSuccess, onFailure))
                .addOnFailureListener(onFailure);
    }

    public void createNotification(AppNotification notification,
                                   OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (notification == null) {
            onFailure.onFailure(new IllegalArgumentException("Invalid notification"));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("recipientUserId", notification.getRecipientUserId());
        data.put("actorUserId", notification.getActorUserId());
        data.put("actorName", notification.getActorName());
        data.put("actionType", notification.getActionType());
        data.put("gemId", notification.getGemId());
        data.put("gemName", notification.getGemName());
        data.put("message", notification.getMessage());
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_NOTIFICATIONS)
                .add(data)
                .addOnSuccessListener(r -> onSuccess.onSuccess(null))
                .addOnFailureListener(onFailure);
    }

    public void fetchNotifications(String userId,
                                   OnSuccessListener<QuerySnapshot> onSuccess,
                                   OnFailureListener onFailure) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("recipientUserId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public ListenerRegistration listenForNotifications(
            String userId,
            com.google.firebase.firestore.EventListener<QuerySnapshot> listener
    ) {
        return db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("recipientUserId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener(listener);
    }

    private void recalculateRating(String gemId,
                                   OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_REVIEWS)
                .whereEqualTo("gemId", gemId)
                .get()
                .addOnSuccessListener(snap -> {
                    double total = 0;
                    int count = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Double rating = doc.getDouble("rating");
                        if (rating != null) {
                            total += rating;
                            count++;
                        }
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("rating", count > 0 ? total / count : 0.0);
                    updates.put("ratingCount", count);
                    db.collection(COLLECTION_GEMS).document(gemId)
                            .update(updates)
                            .addOnSuccessListener(onSuccess)
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    public void fetchUserProfile(String userId,
                                 OnSuccessListener<DocumentSnapshot> onSuccess,
                                 OnFailureListener onFailure) {
        db.collection(COLLECTION_USERS).document(userId)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void fetchUserDoc(String userId,
                             OnSuccessListener<DocumentSnapshot> onSuccess,
                             OnFailureListener onFailure) {
        fetchUserProfile(userId, onSuccess, onFailure);
    }

    public void updateUserProfile(String userId, Map<String, Object> updates,
                                  OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_USERS).document(userId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void updateAuthProfile(String displayName, String avatarUrl,
                                  OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            onFailure.onFailure(new IllegalStateException("No authenticated user"));
            return;
        }

        UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName);
        if (avatarUrl != null && !avatarUrl.isEmpty() && avatarUrl.startsWith("http")) {
            builder.setPhotoUri(android.net.Uri.parse(avatarUrl));
        }
        user.updateProfile(builder.build())
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void syncUserProfileToGems(String userId, String displayName, String avatarUrl,
                                      OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_GEMS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        batch.update(doc.getReference(), "userName", displayName, "userAvatar", avatarUrl);
                    }
                    batch.commit().addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

}
