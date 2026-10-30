
package com.example.rental_system.model;

import java.util.ArrayList;
import java.util.List;

// Room model class using encapsulation
public class Room {

    private String roomId;
    private String ownerId;
    private String title;
    private String address;
    private String description;
    private double rent;
    private boolean available;
    private String imageUrl;
    private List<String> imageBase64;

    // Required for Firebase Firestore
    public Room() {
    }

    // Constructor used by the owner's AddRoomActivity
    public Room(
            String roomId,
            String ownerId,
            String title,
            String address,
            String description,
            double rent,
            boolean available,
            String imageUrl,
            List<String> imageBase64) {

        this.roomId = roomId;
        this.ownerId = ownerId;
        this.title = title;
        this.address = address;
        this.description = description;
        this.rent = rent;
        this.available = available;
        this.imageUrl = imageUrl;
        this.imageBase64 = imageBase64 != null
                ? new ArrayList<>(imageBase64)
                : new ArrayList<>();
    }

    // Getters
    public String getRoomId() {
        return roomId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getTitle() {
        return title;
    }

    public String getAddress() {
        return address;
    }

    public String getDescription() {
        return description;
    }

    public double getRent() {
        return rent;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public List<String> getImageBase64() {
        return imageBase64;
    }

    // Setters
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRent(double rent) {
        this.rent = rent;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setImageBase64(List<String> imageBase64) {
        this.imageBase64 = imageBase64 != null
                ? new ArrayList<>(imageBase64)
                : new ArrayList<>();
    }

    // Compatibility methods for your existing tenant code
    public String getRoomType() {
        return title;
    }

    public String getLocation() {
        return address;
    }

    public double getMonthlyRent() {
        return rent;
    }
}
