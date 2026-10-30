
package com.example.rental_system.model;

public class RentalRequest {

    private String requestId;
    private String roomId;
    private String ownerId;
    private String tenantId;
    private String tenantName;
    private String roomTitle;
    private String roomType;
    private String location;
    private double rent;
    private String status;
    private long createdAt;

    // Required by Firebase Firestore
    public RentalRequest() {
    }

    // Constructor for the existing tenant-side code
    public RentalRequest(
            String requestId,
            String roomId,
            String tenantId,
            String roomType,
            String location,
            double rent,
            String status) {

        this.requestId = requestId;
        this.roomId = roomId;
        this.tenantId = tenantId;
        this.roomType = roomType;
        this.location = location;
        this.rent = rent;
        this.status = status;
    }

    // Constructor for the shared owner and tenant request data
    public RentalRequest(
            String requestId,
            String roomId,
            String ownerId,
            String tenantId,
            String tenantName,
            String roomTitle,
            String status,
            long createdAt) {

        this.requestId = requestId;
        this.roomId = roomId;
        this.ownerId = ownerId;
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.roomTitle = roomTitle;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getRoomTitle() {
        return roomTitle;
    }

    public void setRoomTitle(String roomTitle) {
        this.roomTitle = roomTitle;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getRent() {
        return rent;
    }

    public void setRent(double rent) {
        this.rent = rent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
