package edu.cpp.cs4800.receipttracker;

import edu.cpp.cs4800.receipttracker.model.Receipt;
import edu.cpp.cs4800.receipttracker.model.ReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class ReceiptController {

    @Autowired
    private ReceiptRepository receiptRepository;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Receipt Tracker – CS4800");
        return "home";
    }

    @GetMapping("/receipts")
    public String getReceipts(Model model) {
        List<Receipt> receipts = receiptRepository.findAll();

        double totalAmount = receipts.stream()
                .mapToDouble(Receipt::getAmount)
                .sum();

        double ebtTotal = receipts.stream()
                .filter(r -> "EBT".equalsIgnoreCase(r.getPaymentType()))
                .mapToDouble(Receipt::getAmount)
                .sum();

        long refundableCount = receipts.stream()
                .filter(r -> "refundable".equals(r.getStatus()))
                .count();

        long refundedCount = receipts.stream()
                .filter(Receipt::isRefunded)
                .count();

        model.addAttribute("title", "My Receipts");
        model.addAttribute("receipts", receipts);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("ebtTotal", ebtTotal);
        model.addAttribute("refundableCount", refundableCount);
        model.addAttribute("refundedCount", refundedCount);
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
                             @RequestParam(required = false) Integer customDays,
                             @RequestParam(required = false, defaultValue = "") String description,
                             @RequestParam(required = false) String nonReturnable) {

        LocalDate purchaseDate = LocalDate.parse(date);

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
                vendor,
                amount,
                purchaseDate,
                paymentType,
                refundDeadline,
                description);

        receipt.setNonReturnable("on".equals(nonReturnable));
        receiptRepository.save(receipt);
        return "redirect:/receipts";
    }

    // ── EDIT: show pre-filled form ──
    @GetMapping("/receipts/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Receipt receipt = receiptRepository.findById(id).orElse(null);
        if (receipt == null) return "redirect:/receipts";
        model.addAttribute("title", "Edit Receipt");
        model.addAttribute("receipt", receipt);
        return "edit-receipt";
    }

    // ── EDIT: save changes ──
    @PostMapping("/receipts/update/{id}")
    public String updateReceipt(@PathVariable Long id,
                                @RequestParam String vendor,
                                @RequestParam double amount,
                                @RequestParam String date,
                                @RequestParam String paymentType,
                                @RequestParam String refundDeadline,
                                @RequestParam(required = false, defaultValue = "") String description,
                                @RequestParam(required = false) String refunded,
                                @RequestParam(required = false) String nonReturnable) {

        Receipt receipt = receiptRepository.findById(id).orElse(null);
        if (receipt == null) return "redirect:/receipts";

        receipt.setVendor(vendor);
        receipt.setAmount(amount);
        receipt.setDate(LocalDate.parse(date));
        receipt.setPaymentType(paymentType);
        receipt.setRefundDeadline(LocalDate.parse(refundDeadline));
        receipt.setDescription(description);
        receipt.setRefunded("on".equals(refunded));
        receipt.setNonReturnable("on".equals(nonReturnable));

        receiptRepository.save(receipt);
        return "redirect:/receipts";
    }

    // ── MARK REFUNDED ──
    @PostMapping("/receipts/refund/{id}")
    public String markRefunded(@PathVariable Long id) {
        receiptRepository.findById(id).ifPresent(r -> {
            r.setRefunded(true);
            receiptRepository.save(r);
        });
        return "redirect:/receipts";
    }

    // ── UNMARK REFUNDED ──
    @PostMapping("/receipts/unrefund/{id}")
    public String unmarkRefunded(@PathVariable Long id) {
        receiptRepository.findById(id).ifPresent(r -> {
            r.setRefunded(false);
            receiptRepository.save(r);
        });
        return "redirect:/receipts";
    }

    @PostMapping("/receipts/delete/{id}")
    public String deleteReceipt(@PathVariable Long id) {
        receiptRepository.deleteById(id);
        return "redirect:/receipts";
    }
}