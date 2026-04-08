package edu.cpp.cs4800.receipttracker.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // Spring Data JPA automatically provides save(), findById(), etc.
    // The ID type is String because Firebase UIDs are strings
}