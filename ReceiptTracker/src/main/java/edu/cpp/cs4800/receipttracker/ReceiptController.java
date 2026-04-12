package edu.cpp.cs4800.receipttracker;

import edu.cpp.cs4800.receipttracker.model.Receipt;
import edu.cpp.cs4800.receipttracker.model.ReceiptRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
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

    // ── Helper: get logged in user ID from session ──
    private String getUid(HttpSession session) {
        return (String) session.getAttribute("uid");
    }

    // ── Helper: redirect to login if not logged in ──
    private boolean notLoggedIn(HttpSession session) {
        return getUid(session) == null;
    }

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        if (notLoggedIn(session))
            return "redirect:/login";
        model.addAttribute("title", "Receipt Tracker – CS4800");
        model.addAttribute("userName", session.getAttribute("userName"));
        return "home";
    }

    @GetMapping("/receipts")
    public String getReceipts(HttpSession session, Model model) {
        if (notLoggedIn(session))
            return "redirect:/login";

        String uid = getUid(session);
        List<Receipt> receipts = receiptRepository.findByUserId(uid);

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
        model.addAttribute("userName", session.getAttribute("userName"));
        return "receipts";
    }

    @GetMapping("/receipts/add")
    public String showAddForm(HttpSession session, Model model) {
        if (notLoggedIn(session))
            return "redirect:/login";
        model.addAttribute("title", "Add Receipt");
        return "add-receipt";
    }

    @PostMapping("/receipts")
    public String addReceipt(HttpSession session,
            @RequestParam String vendor,
            @RequestParam double amount,
            @RequestParam String date,
            @RequestParam String paymentType,
            @RequestParam String refundWindowPreset,
            @RequestParam(required = false) Integer customDays,
            @RequestParam(required = false, defaultValue = "") String description,
            @RequestParam(required = false) String nonReturnable) {

        if (notLoggedIn(session))
            return "redirect:/login";

        String uid = getUid(session);
        LocalDate purchaseDate = LocalDate.parse(date);

        int refundDays;
        if ("custom".equals(refundWindowPreset) && customDays != null && customDays > 0) {
            refundDays = customDays;
        } else {
            refundDays = switch (refundWindowPreset) {
                case "60" -> 60;
                case "90" -> 90;
                default -> 30;
            };
        }

        LocalDate refundDeadline = purchaseDate.plusDays(refundDays);

        Receipt receipt = new Receipt(
                uid,
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

    @GetMapping("/receipts/edit/{id}")
    public String showEditForm(@PathVariable Long id, HttpSession session, Model model) {
        if (notLoggedIn(session))
            return "redirect:/login";

        Receipt receipt = receiptRepository.findById(id).orElse(null);
        if (receipt == null || !receipt.getUserId().equals(getUid(session)))
            return "redirect:/receipts";

        model.addAttribute("title", "Edit Receipt");
        model.addAttribute("receipt", receipt);
        return "edit-receipt";
    }

    @PostMapping("/receipts/update/{id}")
    public String updateReceipt(@PathVariable Long id,
            HttpSession session,
            @RequestParam String vendor,
            @RequestParam double amount,
            @RequestParam String date,
            @RequestParam String paymentType,
            @RequestParam String refundDeadline,
            @RequestParam(required = false, defaultValue = "") String description,
            @RequestParam(required = false) String refunded,
            @RequestParam(required = false) String nonReturnable) {

        if (notLoggedIn(session))
            return "redirect:/login";

        Receipt receipt = receiptRepository.findById(id).orElse(null);
        if (receipt == null || !receipt.getUserId().equals(getUid(session)))
            return "redirect:/receipts";

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

    @PostMapping("/receipts/refund/{id}")
    public String markRefunded(@PathVariable Long id, HttpSession session) {
        if (notLoggedIn(session))
            return "redirect:/login";
        receiptRepository.findById(id).ifPresent(r -> {
            if (r.getUserId().equals(getUid(session))) {
                r.setRefunded(true);
                receiptRepository.save(r);
            }
        });
        return "redirect:/receipts";
    }

    @PostMapping("/receipts/unrefund/{id}")
    public String unmarkRefunded(@PathVariable Long id, HttpSession session) {
        if (notLoggedIn(session))
            return "redirect:/login";
        receiptRepository.findById(id).ifPresent(r -> {
            if (r.getUserId().equals(getUid(session))) {
                r.setRefunded(false);
                receiptRepository.save(r);
            }
        });
        return "redirect:/receipts";
    }

    @GetMapping("/receipts/export")
    public void exportCsv(HttpSession session, HttpServletResponse response) throws IOException {
        if (session.getAttribute("uid") == null) {
            response.sendRedirect("/login");
            return;
        }

        String uid = getUid(session);
        List<Receipt> receipts = receiptRepository.findByUserId(uid);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"receipts.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("Vendor,Date,Amount,Payment Type,Status,Refund Deadline,Description");

        for (Receipt r : receipts) {
            writer.printf("\"%s\",\"%s\",\"%.2f\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                    r.getVendor(),
                    r.getDate(),
                    r.getAmount(),
                    r.getPaymentType(),
                    r.getStatus(),
                    r.getRefundDeadline(),
                    r.getDescription() != null ? r.getDescription().replace("\"", "\"\"") : "");
        }
        writer.flush();
    }

    @PostMapping("/receipts/delete/{id}")
    public String deleteReceipt(@PathVariable Long id, HttpSession session) {
        if (notLoggedIn(session))
            return "redirect:/login";
        receiptRepository.findById(id).ifPresent(r -> {
            if (r.getUserId().equals(getUid(session))) {
                receiptRepository.deleteById(id);
            }
        });
        return "redirect:/receipts";
    }
}