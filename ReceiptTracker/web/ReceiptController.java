package edu.cpp.cs4800.ReceiptTracker.web;

import edu.cpp.cs4800.ReceiptTracker.service.ReceiptService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Receipt Tracker – CS4800");
        return "home";
    }

    @GetMapping("/receipts")
    public String listReceipts(Model model) {
        model.addAttribute("title", "My Receipts");
        model.addAttribute("receipts", receiptService.getAllReceipts());
        return "receipts";
    }
}
