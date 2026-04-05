package edu.cpp.cs4800.receipttracker;

import edu.cpp.cs4800.receipttracker.model.Receipt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Controller
public class ReceiptController {

    private final List<Receipt> receipts = new ArrayList<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    public ReceiptController() {
        // Seed data with varied refund windows
        receipts.add(new Receipt(
                idSequence.getAndIncrement(),
                "Coffee Shop",
                4.55,
                LocalDate.now().minusDays(2),
                "Card",
                LocalDate.now().minusDays(2).plusDays(30)));

        receipts.add(new Receipt(
                idSequence.getAndIncrement(),
                "Grocery Store",
                32.10,
                LocalDate.now().minusDays(35),
                "EBT",
                LocalDate.now().minusDays(35).plusDays(30)));

        receipts.add(new Receipt(
                idSequence.getAndIncrement(),
                "Walmart",
                58.75,
                LocalDate.now().minusDays(5),
                "EBT",
                LocalDate.now().minusDays(5).plusDays(90)));
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Receipt Tracker – CS4800");
        return "home";
    }

    @GetMapping("/receipts")
    public String getReceipts(Model model) {
        double totalAmount = receipts.stream()
                .mapToDouble(Receipt::getAmount)
                .sum();

        double ebtTotal = receipts.stream()
                .filter(r -> "EBT".equalsIgnoreCase(r.getPaymentType()))
                .mapToDouble(Receipt::getAmount)
                .sum();

        long refundableCount = receipts.stream()
                .filter(Receipt::isRefundable)
                .count();

        model.addAttribute("title", "My Receipts");
        model.addAttribute("receipts", receipts);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("ebtTotal", ebtTotal);
        model.addAttribute("refundableCount", refundableCount);
        return "receipts";
    }

    @GetMapping("/receipts/add")
    public String showAddForm(Model model) {
        model.addAttribute("title", "Add Receipt");
        return "add-receipt";
    }

    @PostMapping("/receipts")
    public String addReceipt(@RequestParam String vendor,
                             @RequestParam double amount,
                             @RequestParam String date,
                             @RequestParam String paymentType,
                             @RequestParam String refundWindowPreset,
                             @RequestParam(required = false) Integer customDays) {

        LocalDate purchaseDate = LocalDate.parse(date);

        // Determine refund window in days
        int refundDays;
        if ("custom".equals(refundWindowPreset) && customDays != null && customDays > 0) {
            refundDays = customDays;
        } else {
            refundDays = switch (refundWindowPreset) {
                case "60" -> 60;
                case "90" -> 90;
                default   -> 30;
            };
        }

        LocalDate refundDeadline = purchaseDate.plusDays(refundDays);

        Receipt receipt = new Receipt(
                idSequence.getAndIncrement(),
                vendor,
                amount,
                purchaseDate,
                paymentType,
                refundDeadline);

        receipts.add(receipt);
        return "redirect:/receipts";
    }

    @PostMapping("/receipts/delete/{id}")
    public String deleteReceipt(@PathVariable Long id) {
        receipts.removeIf(r -> r.getId().equals(id));
        return "redirect:/receipts";
    }
}