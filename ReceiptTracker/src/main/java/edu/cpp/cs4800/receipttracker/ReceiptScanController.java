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

    private static final String CLAUDE_MODEL = "claude-sonnet-4-6";

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
                if (firstLine) { firstLine = false; continue; }
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

    private Integer lookupRefundDays(String vendor) {
        if (vendor == null) return null;
        String v = vendor.toLowerCase().replace("?", "").trim();
        if (storePolicies.containsKey(v)) return storePolicies.get(v);
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
                Look at this receipt image carefully and extract information. Return ONLY a valid JSON object with no extra text, no markdown, no explanation.
                
                RULES:
                - confidence: "high" if you can read most of the receipt clearly, "medium" if partially readable, "low" if mostly unreadable
                - vendor: The store or restaurant name. Expand abbreviations (e.g. "WLMRT" = "Walmart", "TGT" = "Target", "WFM" = "Whole Foods Market")
                - amount: The FINAL amount the customer actually paid after ALL discounts, coupons, and tax. This is usually the LAST and LARGEST total on the receipt. Look for "TOTAL", "AMOUNT DUE", "BALANCE DUE", "AMOUNT PAID", "GRAND TOTAL". NEVER pick subtotals, tax amounts, or partial totals. If you see multiple totals, always choose the final bottom-most one.
                - date: Purchase date in YYYY-MM-DD format. If not visible, return null.
                - paymentType: One of Card, Cash, or EBT. VISA/MC/AMEX/DISCOVER/CHIP/DEBIT = Card, CASH = Cash, EBT/SNAP/LINK/LONE STAR = EBT. If not visible, return null.
                - items: List each purchased item with its price. Expand abbreviations into readable names. Format: "Item Name: $X.XX" separated by ", ". Do NOT include tax, subtotal, discounts, or bag fees as items. If you cannot read items clearly, return null.
                
                CONFIDENCE for individual fields:
                - Confident = return value normally
                - Unsure but reasonable guess = add ? at end (e.g. "Walmart?" or "24.99?")
                - Cannot read at all = return null
                
                Return ONLY this JSON:
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
                    .asText()
                    .trim();

            // Strip markdown code fences if present
            if (claudeText.startsWith("```")) {
                claudeText = claudeText.replaceAll("```json", "").replaceAll("```", "").trim();
            }

            JsonNode extracted = mapper.readTree(claudeText);
            String confidence = extracted.path("confidence").asText("low");

            if ("low".equals(confidence)) {
                result.put("error", "Claude couldn't read this receipt clearly. Please try a clearer photo or fill in manually.");
                result.put("canFillManually", true);
                result.put("canRetry", true);
            } else {
                result.put("success", true);
                result.put("confidence", confidence);

                // Track which fields were filled
                List<String> missingFields = new ArrayList<>();

                // Vendor
                String vendor = null;
                if (!extracted.path("vendor").isNull() && !extracted.path("vendor").isMissingNode()) {
                    vendor = extracted.path("vendor").asText();
                    result.put("vendor", vendor);
                } else {
                    missingFields.add("Vendor");
                }

                // Amount
                if (!extracted.path("amount").isNull() && !extracted.path("amount").isMissingNode()) {
                    result.put("amount", extracted.path("amount").asText());
                } else {
                    missingFields.add("Amount");
                }

                // Date
                if (!extracted.path("date").isNull() && !extracted.path("date").isMissingNode()) {
                    result.put("date", extracted.path("date").asText());
                } else {
                    missingFields.add("Purchase Date");
                }

                // Payment type
                if (!extracted.path("paymentType").isNull() && !extracted.path("paymentType").isMissingNode()) {
                    result.put("paymentType", extracted.path("paymentType").asText());
                } else {
                    missingFields.add("Payment Type");
                }

                // Items as description
                if (!extracted.path("items").isNull() && !extracted.path("items").isMissingNode()) {
                    String items = extracted.path("items").asText();
                    if (items.length() > 500) items = items.substring(0, 497) + "...";
                    result.put("items", items);
                }

                // Pass missing fields to frontend
                if (!missingFields.isEmpty()) {
                    result.put("missingFields", missingFields);
                }

                // ── Store policy lookup ──
                if (vendor != null) {
                    Integer refundDays = lookupRefundDays(vendor);
                    if (refundDays != null) {
                        result.put("refundDays", refundDays);
                        result.put("refundDaysFound", true);
                        // If 0 days, flag as non-returnable
                        if (refundDays == 0) {
                            result.put("nonReturnable", true);
                        }
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