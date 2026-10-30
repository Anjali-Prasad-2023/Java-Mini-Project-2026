package com.example.rental_system.model;

public class Owner extends User {

    private String ownerId;

    public Owner(
            String userId,
            String name,
            String email,
            String password,
            String ownerId) {

        super(userId, name, email, password);

        this.ownerId = ownerId;
    }

    public String getOwnerId() {
        return ownerId;
    }
}