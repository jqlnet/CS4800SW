package edu.cpp.cs4800.receipttracker.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "uid")
    private String uid; // Firebase UID — unique per Google account

    private String email;
    private String name;

    public User() {}

    public User(String uid, String email, String name) {
        this.uid = uid;
        this.email = email;
        this.name = name;
    }

    // ── Getters ──
    public String getUid()   { return uid; }
    public String getEmail() { return email; }
    public String getName()  { return name; }

    // ── Setters ──
    public void setUid(String uid)     { this.uid = uid; }
    public void setEmail(String email) { this.email = email; }
    public void setName(String name)   { this.name = name; }
}