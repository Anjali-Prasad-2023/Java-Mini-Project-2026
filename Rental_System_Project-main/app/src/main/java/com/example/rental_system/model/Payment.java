
package com.example.rental_system.model;

public class Payment {

    private String paymentId;
    private String tenantId;
    private String ownerId;
    private String roomId;

    // Links payment to the accepted rental request
    private String requestId;

    // Kept for compatibility with older code
    private String rentalId;

    private double amount;
    private String paymentDate;
    private String rentMonth;
    private String status;
    private String method;
    private long createdAt;

    // Optional reference for UPI or bank transfers
    private String transactionReference;

    // Reserved for future payment gateway integration
    private String gatewayPaymentId;

    // Required by Firebase Firestore
    public Payment() {
    }

    // Existing constructor - kept for compatibility
    public Payment(String paymentId, String tenantId,
                   double amount, String paymentDate,
                   String status) {
        this.paymentId = paymentId;
        this.tenantId = tenantId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.status = status;
    }

    // Constructor including payment method
    public Payment(String paymentId, String tenantId,
                   double amount, String paymentDate,
                   String status, String method) {
        this(paymentId, tenantId, amount, paymentDate, status);
        this.method = method;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRentalId() {
        return rentalId;
    }

    public void setRentalId(String rentalId) {
        this.rentalId = rentalId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getRentMonth() {
        return rentMonth;
    }

    public void setRentMonth(String rentMonth) {
        this.rentMonth = rentMonth;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getGatewayPaymentId() {
        return gatewayPaymentId;
    }

    public void setGatewayPaymentId(String gatewayPaymentId) {
        this.gatewayPaymentId = gatewayPaymentId;
    }
}
