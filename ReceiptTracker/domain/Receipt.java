package edu.cpp.cs4800.ReceiptTracker.domain;

import java.time.LocalDate;

public class Receipt {

    private Long id;
    private String storeName;
    private LocalDate purchaseDate;
    private double totalAmount;
    private String paymentType; // cash, card, EBT, etc.
    private LocalDate refundDeadline;
    private boolean refundable;

    public Receipt(Long id,
                   String storeName,
                   LocalDate purchaseDate,
                   double totalAmount,
                   String paymentType,
                   LocalDate refundDeadline,
                   boolean refundable) {
        this.id = id;
        this.storeName = storeName;
        this.purchaseDate = purchaseDate;
        this.totalAmount = totalAmount;
        this.paymentType = paymentType;
        this.refundDeadline = refundDeadline;
        this.refundable = refundable;
    }

    public Long getId() {
        return id;
    }

    public String getStoreName() {
        return storeName;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public LocalDate getRefundDeadline() {
        return refundDeadline;
    }

    public boolean isRefundable() {
        return refundable;
    }
}
