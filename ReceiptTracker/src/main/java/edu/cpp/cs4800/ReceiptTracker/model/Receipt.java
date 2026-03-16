package edu.cpp.cs4800.receipttracker.model;

import java.time.LocalDate;

public class Receipt {
    private Long id;
    private String vendor;
    private double amount;
    private LocalDate date;

    public Receipt(Long id, String vendor, double amount, LocalDate date) {
        this.id = id;
        this.vendor = vendor;
        this.amount = amount;
        this.date = date;
    }

    public Long getId() { return id; }
    public String getVendor() { return vendor; }
    public double getAmount() { return amount; }
    public LocalDate getDate() { return date; }
}
