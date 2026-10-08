package com.campusos.fees;

import com.campusos.common.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Razorpay integration. When real API keys are configured (app.razorpay.enabled=true)
 * orders are created through the Razorpay REST API (sandbox/test keys). Otherwise a
 * deterministic local sandbox order id is generated so the payment flow can be
 * exercised end to end without external calls.
 */
@Service
@Slf4j
public class RazorpayService {

    private final boolean enabled;
    private final String keyId;
    private final String keySecret;

    public RazorpayService(@Value("${app.razorpay.enabled:false}") boolean enabled,
                           @Value("${app.razorpay.key-id:}") String keyId,
                           @Value("${app.razorpay.key-secret:}") String keySecret) {
        this.enabled = enabled;
        this.keyId = keyId;
        this.keySecret = keySecret;
        if (enabled) {
            log.info("Razorpay enabled with key {}", keyId);
        } else {
            log.info("Razorpay running in local sandbox mode (no real API calls)");
        }
    }

    public record Order(String orderId, BigDecimal amount, String currency, boolean live) {}

    public Order createOrder(String receiptNo, BigDecimal amountInRupees) {
        if (!enabled) {
            // Deterministic sandbox order id for local testing.
            String id = "order_local_" + receiptNo.toLowerCase();
            return new Order(id, amountInRupees, "INR", false);
        }
        try {
            var client = new com.razorpay.RazorpayClient(keyId, keySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInRupees.multiply(new BigDecimal("100")).intValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", receiptNo);
            var order = client.orders.create(orderRequest);
            return new Order(order.get("id").toString(), amountInRupees, "INR", true);
        } catch (Exception ex) {
            log.error("Razorpay order creation failed", ex);
            throw ApiException.badRequest("Payment gateway error: " + ex.getMessage());
        }
    }

    /** Verifies razorpay signature: HMAC-SHA256(order_id + '|' + payment_id, keySecret). */
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        if (!enabled) {
            // Local sandbox: accept well-formed signatures produced by the sandbox helper.
            return signature != null && signature.equals(sandboxSignature(orderId, paymentId));
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return MessageDigest.isEqual(hex.toString().getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return false;
        }
    }

    /** Only available in local sandbox mode to let the E2E flow simulate a payment. */
    public String sandboxSignature(String orderId, String paymentId) {
        return java.util.Base64.getEncoder()
                .encodeToString((orderId + "|" + paymentId + "|campusos-sandbox").getBytes(StandardCharsets.UTF_8));
    }

    public boolean isLive() {
        return enabled;
    }
}
