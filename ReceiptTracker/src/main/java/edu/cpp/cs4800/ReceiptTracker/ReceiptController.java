package edu.cpp.cs4800.receipttracker;

import edu.cpp.cs4800.receipttracker.model.Receipt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
        receipts.add(new Receipt(idSequence.getAndIncrement(), "Coffee Shop", 4.55, LocalDate.now().minusDays(2)));
        receipts.add(new Receipt(idSequence.getAndIncrement(), "Grocery Store", 32.10, LocalDate.now().minusDays(1)));
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Receipt Tracker – CS4800");
        return "home"; // home.html
    }

    @GetMapping("/receipts")
    public String getReceipts(Model model) {
        model.addAttribute("title", "My Receipts");
        model.addAttribute("receipts", receipts);
        return "receipts"; // receipts.html
    }

    @PostMapping("/receipts")
    public String addReceipt(@RequestParam String vendor,
                             @RequestParam double amount,
                             @RequestParam String date) {
        Receipt receipt = new Receipt(idSequence.getAndIncrement(), vendor, amount, LocalDate.parse(date));
        receipts.add(receipt);
        return "redirect:/receipts";
    }
}
