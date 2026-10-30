
package com.example.rental_system.model;

// Encapsulation
public class Complaint {

    private String complaintId;
    private String tenantId;
    private String roomId;
    private String title;
    private String subject;
    private String description;
    private String date;
    private String status;

    // Required by Firebase Firestore
    public Complaint() {
    }

    public Complaint(
            String complaintId,
            String tenantId,
            String roomId,
            String title,
            String description,
            String date,
            String status) {

        this.complaintId = complaintId;
        this.tenantId = tenantId;
        this.roomId = roomId;
        this.title = title;
        this.subject = title;
        this.description = description;
        this.date = date;
        this.status = status;
    }

    public String getComplaintId() {
        return complaintId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getTitle() {
        return title;
    }

    public String getSubject() {
        if (subject != null && !subject.isEmpty()) {
            return subject;
        }
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSubject(String subject) {
        this.subject = subject;
        this.title = subject;
    }

    public void setTitle(String title) {
        this.title = title;
        this.subject = title;
    }
}
