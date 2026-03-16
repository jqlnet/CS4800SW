package edu.cpp.cs4800.ReceiptTracker.service;

import edu.cpp.cs4800.ReceiptTracker.domain.Receipt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class ReceiptService {

    public List<Receipt> getAllReceipts() {
        // Fake data for demo
        return Arrays.asList(
                new Receipt(
                        1L,
                        "Target",
                        LocalDate.now().minusDays(2),
                        54.23,
                        "Card",
                        LocalDate.now().plusDays(28),
                        true
                ),
                new Receipt(
                        2L,
                        "Walmart",
                        LocalDate.now().minusDays(20),
                        32.10,
                        "EBT",
                        LocalDate.now().minusDays(5),
                        false
                )
        );
    }
}
