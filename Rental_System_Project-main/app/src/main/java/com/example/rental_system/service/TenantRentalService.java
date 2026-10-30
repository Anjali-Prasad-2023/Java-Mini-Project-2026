package com.example.rental_system.service;

public class TenantRentalService extends RentalService {

    public TenantRentalService() {
        super("Tenant Rental Service");
    }

    @Override
    public void processRental() {
        System.out.println("Processing tenant rental request");
    }
}