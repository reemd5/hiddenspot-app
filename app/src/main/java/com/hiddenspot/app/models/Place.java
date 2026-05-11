package com.hiddenspot.app.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class Place {

    @DocumentId
    private String id;
    private String name;
    private String city;
    private String address;
    private String phone;
    private String description;
    private String category;
    private List<String> images;
    private String userId;
    private String userName;
    private String userAvatar;
    private double rating;
    private int ratingCount;
    private int likesCount;
    private int upvotes;
    private int downvotes;
    private boolean isFavorited;
    
    @ServerTimestamp
    private Timestamp createdAt;
    
    private boolean isVerified;

    public Place() {}

    public Place(String name, String city, String address, String phone,
                 String description, String category, List<String> images,
                 String userId, String userName) {
        this.name = name; this.city = city; this.address = address;
        this.phone = phone; this.description = description;
        this.category = category; this.images = images;
        this.userId = userId; this.userName = userName;
        this.rating = 0.0; this.ratingCount = 0;
        this.likesCount = 0; this.upvotes = 0; this.downvotes = 0;
        this.isFavorited = false;
        this.isVerified = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public List<String> getImages() { return images; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserAvatar() { return userAvatar; }
    public double getRating() { return rating; }
    public int getRatingCount() { return ratingCount; }
    public int getLikesCount() { return likesCount; }
    public int getUpvotes() { return upvotes; }
    public int getDownvotes() { return downvotes; }
    public boolean isFavorited() { return isFavorited; }
    public Timestamp getCreatedAt() { return createdAt; }

    @PropertyName("isVerified")
    public boolean isVerified() { return isVerified; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCity(String city) { this.city = city; }
    public void setAddress(String address) { this.address = address; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setImages(List<String> images) { this.images = images; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public void setRating(double rating) { this.rating = rating; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
    public void setUpvotes(int upvotes) { this.upvotes = upvotes; }
    public void setDownvotes(int downvotes) { this.downvotes = downvotes; }
    public void setFavorited(boolean favorited) { isFavorited = favorited; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @PropertyName("isVerified")
    public void setVerified(boolean verified) { isVerified = verified; }

    public String getFirstImage() {
        if (images != null && !images.isEmpty()) return images.get(0);
        return "";
    }

    public String getFormattedDate() {
        if (createdAt == null) return "Recent";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(createdAt.toDate());
    }

    public String getCategoryDisplay() {
        if (category == null) return "";
        switch (category) {
            case "Restaurant": return "🍽️ Restaurant";
            case "Garden":     return "🌿 Garden";
            case "Café":       return "☕ Café";
            case "Viewpoint":  return "🏔️ Viewpoint";
            case "Park":       return "🌳 Park";
            case "Beach":      return "🏖️ Beach";
            case "Library":    return "📚 Library";
            case "Shop":       return "🛍️ Shop";
            case "Historical": return "🏛️ Historical";
            default:           return category;
        }
    }
}
