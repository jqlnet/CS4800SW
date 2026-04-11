package edu.cpp.cs4800.receipttracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ReceiptScanController {

    @Value("${anthropic.api.key}")
    private String ANTHROPIC_API_KEY;
    private static final String CLAUDE_MODEL = "claude-sonnet-4-20250514";

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
            // Convert image to base64
            byte[] imageBytes = image.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mediaType = image.getContentType();

            // Build the prompt
            String prompt = """
                Look at this receipt image carefully. Extract the following fields and return ONLY a JSON object, no other text.
                
                Rules:
                - If you are confident about a value, return it as-is
                - If you think you can read it but are not 100% sure, add a ? at the end (e.g. "Walmart?" or "24.99?")
                - If you cannot read a value at all, return null for that field
                - For date, use YYYY-MM-DD format
                - For amount, return only the final total as a number (no $ sign)
                - For paymentType, return one of: Card, Cash, EBT — or null if unknown
                - For confidence, return "high", "medium", or "low" based on overall receipt readability
                
                Return this exact JSON structure:
                {
                  "vendor": "store name or null",
                  "amount": 0.00 or null,
                  "date": "YYYY-MM-DD or null",
                  "paymentType": "Card/Cash/EBT or null",
                  "confidence": "high/medium/low"
                }
                """;

            // Build Claude API request body
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
            requestBody.put("max_tokens", 500);
            requestBody.put("messages", List.of(message));

            String requestJson = mapper.writeValueAsString(requestBody);

            // Call Claude API
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

            // Parse Claude's response
            JsonNode responseJson = mapper.readTree(response.body());
            String claudeText = responseJson
                    .path("content")
                    .get(0)
                    .path("text")
                    .asText();

            // Parse the JSON Claude returned
            JsonNode extracted = mapper.readTree(claudeText.trim());

            String confidence = extracted.path("confidence").asText("low");

            // Build result based on confidence
            if ("low".equals(confidence)) {
                result.put("error", "Claude couldn't read this receipt clearly. Please try a clearer photo or fill in manually.");
                result.put("canFillManually", true);
                result.put("canRetry", true);
            } else {
                result.put("success", true);
                result.put("confidence", confidence);

                // Vendor
                if (!extracted.path("vendor").isNull()) {
                    result.put("vendor", extracted.path("vendor").asText());
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