package com.example.rental_system.service;

public abstract class RentalService {

    protected String serviceName;

    public RentalService(String serviceName) {
        this.serviceName = serviceName;
    }

    public abstract void processRental();

    public void showServiceName() {
        System.out.println("Service: " + serviceName);
    }
}