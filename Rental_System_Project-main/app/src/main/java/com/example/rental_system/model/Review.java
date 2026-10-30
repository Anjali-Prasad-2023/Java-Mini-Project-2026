
package com.example.rental_system.model;

// Model class for storing room reviews
public class Review {

    private String reviewId;
    private String tenantId;
    private String roomId;
    private int rating;
    private String comment;
    private String date;

    // Required by Firebase Firestore
    public Review() {
    }

    // Constructor used by existing tenant-side code
    public Review(
            String reviewId,
            String tenantId,
            String roomId,
            int rating,
            String comment,
            String date) {

        this.reviewId = reviewId;
        this.tenantId = tenantId;
        this.roomId = roomId;
        this.rating = rating;
        this.comment = comment;
        this.date = date;
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
