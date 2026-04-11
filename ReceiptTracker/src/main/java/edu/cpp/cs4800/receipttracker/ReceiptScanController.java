package edu.cpp.cs4800.receipttracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Controller
public class ReceiptScanController {

    @Value("${anthropic.api.key}")
    private String ANTHROPIC_API_KEY;

    private static final String CLAUDE_MODEL = "claude-sonnet-4-20250514";

    // Store name -> refund days lookup table
    private final Map<String, Integer> storePolicies = new HashMap<>();

    @PostConstruct
    public void loadStorePolicies() {
        try {
            InputStream is = new ClassPathResource("store-policies.csv").getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; } // skip header
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    String store = parts[0].trim().toLowerCase();
                    int days = Integer.parseInt(parts[1].trim());
                    storePolicies.put(store, days);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load store policies: " + e.getMessage());
        }
    }

    // Look up refund days for a vendor name (case-insensitive, partial match)
    private Integer lookupRefundDays(String vendor) {
        if (vendor == null) return null;
        String v = vendor.toLowerCase().replace("?", "").trim();

        // Exact match first
        if (storePolicies.containsKey(v)) return storePolicies.get(v);

        // Partial match — check if vendor contains or is contained by a known store
        for (Map.Entry<String, Integer> entry : storePolicies.entrySet()) {
            if (v.contains(entry.getKey()) || entry.getKey().contains(v)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @PostMapping("/receipts/scan")
    public ResponseEntity<Map<String, Object>> scanReceipt(
            @RequestParam("image") MultipartFile image,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();

        if (session.getAttribute("uid") == null) {
            result.put("error", "Not logged in");
            return ResponseEntity.status(401).body(result);
        }

        try {
            byte[] imageBytes = image.getBytes();
            String mediaType = image.getContentType();

            // ── Convert HEIC to JPEG if needed ──
            if (mediaType != null && (mediaType.equalsIgnoreCase("image/heic")
                    || mediaType.equalsIgnoreCase("image/heif")
                    || (image.getOriginalFilename() != null &&
                        image.getOriginalFilename().toLowerCase().endsWith(".heic")))) {
                try {
                    BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
                    if (bufferedImage != null) {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "JPEG", outputStream);
                        imageBytes = outputStream.toByteArray();
                        mediaType = "image/jpeg";
                    }
                } catch (Exception e) {
                    mediaType = "image/jpeg";
                }
            }

            if (mediaType == null || !mediaType.startsWith("image/")) {
                mediaType = "image/jpeg";
            }

            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // ── Build the prompt ──
            String prompt = """
                Look at this receipt image carefully and extract information. Return ONLY a valid JSON object with no extra text.
                
                RULES:
                - confidence: "high" if you can read most of the receipt clearly, "medium" if partially readable, "low" if mostly unreadable
                - vendor: The store or restaurant name. Expand any abbreviations (e.g. "WLMRT" = "Walmart", "TGT" = "Target")
                - amount: The GRAND TOTAL only — the final amount the customer paid. NOT the subtotal, NOT the tax, NOT any partial total. Look for words like "TOTAL", "AMOUNT DUE", "AMOUNT", "GRAND TOTAL", "TOTAL DUE". If multiple totals exist, pick the largest final one.
                - date: Purchase date in YYYY-MM-DD format
                - paymentType: One of Card, Cash, or EBT. Look for VISA/MC/AMEX/CHIP = Card, CASH = Cash, EBT/SNAP/LINK = EBT
                - items: A list of items purchased. Expand abbreviations into readable names (e.g. "BNN" = "Bananas", "BT CARE" = "Bluetooth Earbuds"). Include the price of each item. Format: "Item Name: $X.XX". Separate items with ", ". If you cannot read items, return null.
                
                CONFIDENCE RULES for individual fields:
                - If confident about a value: return it normally
                - If unsure but have a reasonable guess: add ? at the end (e.g. "Walmart?" or "24.99?")
                - If you cannot read a value at all: return null
                
                Return this exact JSON:
                {
                  "vendor": "store name or null",
                  "amount": 0.00 or null,
                  "date": "YYYY-MM-DD or null",
                  "paymentType": "Card/Cash/EBT or null",
                  "items": "Item1: $X.XX, Item2: $X.XX or null",
                  "confidence": "high/medium/low"
                }
                """;

            // ── Build Claude API request ──
            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> imageSource = new HashMap<>();
            imageSource.put("type", "base64");
            imageSource.put("media_type", mediaType);
            imageSource.put("data", base64Image);

            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image");
            imageContent.put("source", imageSource);

            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", prompt);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", List.of(imageContent, textContent));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", CLAUDE_MODEL);
            requestBody.put("max_tokens", 800);
            requestBody.put("messages", List.of(message));

            String requestJson = mapper.writeValueAsString(requestBody);

            // ── Call Claude API ──
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.anthropic.com/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", ANTHROPIC_API_KEY)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                result.put("error", "Claude API error: " + response.statusCode());
                result.put("canFillManually", true);
                return ResponseEntity.ok(result);
            }

            // ── Parse Claude's response ──
            JsonNode responseJson = mapper.readTree(response.body());
            String claudeText = responseJson
                    .path("content")
                    .get(0)
                    .path("text")
                    .asText();

            JsonNode extracted = mapper.readTree(claudeText.trim());
            String confidence = extracted.path("confidence").asText("low");

            if ("low".equals(confidence)) {
                result.put("error", "Claude couldn't read this receipt clearly. Please try a clearer photo or fill in manually.");
                result.put("canFillManually", true);
                result.put("canRetry", true);
            } else {
                result.put("success", true);
                result.put("confidence", confidence);

                // Vendor
                String vendor = null;
                if (!extracted.path("vendor").isNull()) {
                    vendor = extracted.path("vendor").asText();
                    result.put("vendor", vendor);
                }

                // Amount
                if (!extracted.path("amount").isNull()) {
                    result.put("amount", extracted.path("amount").asText());
                }

                // Date
                if (!extracted.path("date").isNull()) {
                    result.put("date", extracted.path("date").asText());
                }

                // Payment type
                if (!extracted.path("paymentType").isNull()) {
                    result.put("paymentType", extracted.path("paymentType").asText());
                }

                // Items as description (truncate to 500 chars)
                if (!extracted.path("items").isNull()) {
                    String items = extracted.path("items").asText();
                    if (items.length() > 500) {
                        items = items.substring(0, 497) + "...";
                    }
                    result.put("items", items);
                }

                // ── Store policy lookup ──
                if (vendor != null) {
                    Integer refundDays = lookupRefundDays(vendor);
                    if (refundDays != null) {
                        result.put("refundDays", refundDays);
                        result.put("refundDaysFound", true);
                    }
                }
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("error", "Something went wrong. Please try again or fill in manually.");
            result.put("canFillManually", true);
            result.put("canRetry", true);
            return ResponseEntity.ok(result);
        }
    }
}