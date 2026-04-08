package edu.cpp.cs4800.receipttracker.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    // Spring Data JPA automatically provides:
    // save(), findById(), findAll(), deleteById(), count(), etc.
}