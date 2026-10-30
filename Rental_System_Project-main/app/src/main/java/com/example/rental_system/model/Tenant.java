package com.example.rental_system.model;

public class Tenant extends User {

    private String tenantId;

    public Tenant(
            String userId,
            String name,
            String email,
            String password,
            String tenantId) {

        super(userId, name, email, password);

        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }
}