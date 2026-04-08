package edu.cpp.cs4800.receipttracker.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "receipts")
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vendor;
    private double amount;
    private LocalDate date;
    private String paymentType;
    private LocalDate refundDeadline;

    @Column(length = 150)
    private String description;

    private boolean refunded;
    private boolean nonReturnable;

    // ── Required no-arg constructor for JPA ──
    public Receipt() {}

    public Receipt(String vendor,
                   double amount,
                   LocalDate date,
                   String paymentType,
                   LocalDate refundDeadline,
                   String description) {
        this.vendor = vendor;
        this.amount = amount;
        this.date = date;
        this.paymentType = paymentType;
        this.refundDeadline = refundDeadline;
        this.description = description;
        this.refunded = false;
        this.nonReturnable = false;
    }

    // ── Getters ──
    public Long getId()                  { return id; }
    public String getVendor()            { return vendor; }
    public double getAmount()            { return amount; }
    public LocalDate getDate()           { return date; }
    public String getPaymentType()       { return paymentType; }
    public LocalDate getRefundDeadline() { return refundDeadline; }
    public String getDescription()       { return description; }
    public boolean isRefunded()          { return refunded; }
    public boolean isNonReturnable()     { return nonReturnable; }

    // ── Setters ──
    public void setVendor(String vendor)                    { this.vendor = vendor; }
    public void setAmount(double amount)                    { this.amount = amount; }
    public void setDate(LocalDate date)                     { this.date = date; }
    public void setPaymentType(String paymentType)          { this.paymentType = paymentType; }
    public void setRefundDeadline(LocalDate refundDeadline) { this.refundDeadline = refundDeadline; }
    public void setDescription(String description)          { this.description = description; }
    public void setRefunded(boolean refunded)               { this.refunded = refunded; }
    public void setNonReturnable(boolean nonReturnable)     { this.nonReturnable = nonReturnable; }

    // ── Status logic ──
    public String getStatus() {
        if (nonReturnable) return "na";
        if (refunded) return "refunded";
        if (LocalDate.now().isBefore(refundDeadline) || LocalDate.now().isEqual(refundDeadline))
            return "refundable";
        return "expired";
    }

    public boolean isRefundable() {
        if (nonReturnable || refunded) return false;
        return LocalDate.now().isBefore(refundDeadline) || LocalDate.now().isEqual(refundDeadline);
    }
}