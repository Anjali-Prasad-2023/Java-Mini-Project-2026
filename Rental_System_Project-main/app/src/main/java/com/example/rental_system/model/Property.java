package com.example.rental_system.model;

public class Property {

    private String propertyId;
    private String propertyName;
    private String address;
    private String ownerId;

    public Property(
            String propertyId,
            String propertyName,
            String address,
            String ownerId) {

        this.propertyId = propertyId;
        this.propertyName = propertyName;
        this.address = address;
        this.ownerId = ownerId;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getAddress() {
        return address;
    }

    public String getOwnerId() {
        return ownerId;
    }
}