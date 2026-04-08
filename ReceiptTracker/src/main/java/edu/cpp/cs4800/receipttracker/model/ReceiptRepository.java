package edu.cpp.cs4800.receipttracker.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    // Find all receipts belonging to a specific user
    List<Receipt> findByUserId(String userId);
}