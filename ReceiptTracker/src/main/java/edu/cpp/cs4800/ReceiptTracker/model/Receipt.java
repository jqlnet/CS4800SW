package edu.cpp.cs4800.receipttracker.model;

import java.time.LocalDate;

public class Receipt {
    private Long id;
    private String vendor;
    private double amount;
    private LocalDate date;
    private String paymentType;      // e.g. Cash, Card, EBT
    private LocalDate refundDeadline;

    public Receipt(Long id,
                   String vendor,
                   double amount,
                   LocalDate date,
                   String paymentType,
                   LocalDate refundDeadline) {
        this.id = id;
        this.vendor = vendor;
        this.amount = amount;
        this.date = date;
        this.paymentType = paymentType;
        this.refundDeadline = refundDeadline;
    }

    public Long getId() { return id; }
    public String getVendor() { return vendor; }
    public double getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getPaymentType() { return paymentType; }
    public LocalDate getRefundDeadline() { return refundDeadline; }

    public boolean isRefundable() {
        return LocalDate.now().isBefore(refundDeadline) || LocalDate.now().isEqual(refundDeadline);
    }
}
