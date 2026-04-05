package edu.cpp.cs4800.receipttracker.model;

import java.time.LocalDate;

public class Receipt {
    private Long id;
    private String vendor;
    private double amount;
    private LocalDate date;
    private String paymentType;
    private LocalDate refundDeadline;
    private String description;

    public Receipt(Long id,
                   String vendor,
                   double amount,
                   LocalDate date,
                   String paymentType,
                   LocalDate refundDeadline,
                   String description) {
        this.id = id;
        this.vendor = vendor;
        this.amount = amount;
        this.date = date;
        this.paymentType = paymentType;
        this.refundDeadline = refundDeadline;
        this.description = description;
    }

    // ── Getters ──
    public Long getId()                  { return id; }
    public String getVendor()            { return vendor; }
    public double getAmount()            { return amount; }
    public LocalDate getDate()           { return date; }
    public String getPaymentType()       { return paymentType; }
    public LocalDate getRefundDeadline() { return refundDeadline; }
    public String getDescription()       { return description; }

    // ── Setters (needed for edit) ──
    public void setVendor(String vendor)               { this.vendor = vendor; }
    public void setAmount(double amount)               { this.amount = amount; }
    public void setDate(LocalDate date)                { this.date = date; }
    public void setPaymentType(String paymentType)     { this.paymentType = paymentType; }
    public void setRefundDeadline(LocalDate refundDeadline) { this.refundDeadline = refundDeadline; }
    public void setDescription(String description)     { this.description = description; }

    public boolean isRefundable() {
        return LocalDate.now().isBefore(refundDeadline) || LocalDate.now().isEqual(refundDeadline);
    }
}