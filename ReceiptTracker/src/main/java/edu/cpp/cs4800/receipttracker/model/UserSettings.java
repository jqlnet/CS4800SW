package edu.cpp.cs4800.receipttracker.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @Column(name = "uid")
    private String uid;

    private String displayName;
    private boolean emailNotifications;

    public UserSettings() {}

    public UserSettings(String uid, String displayName) {
        this.uid = uid;
        this.displayName = displayName;
        this.emailNotifications = false;
    }

    // ── Getters ──
    public String getUid()              { return uid; }
    public String getDisplayName()      { return displayName; }
    public boolean isEmailNotifications() { return emailNotifications; }

    // ── Setters ──
    public void setUid(String uid)                        { this.uid = uid; }
    public void setDisplayName(String displayName)        { this.displayName = displayName; }
    public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }
}