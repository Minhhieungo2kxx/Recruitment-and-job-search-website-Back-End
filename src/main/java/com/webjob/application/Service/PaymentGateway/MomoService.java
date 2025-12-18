package com.webjob.application.Service.PaymentGateway;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MomoService {
    private static final String PARTNER_CODE = "MOMO";
    private static final String REDIRECT_URL = "http://localhost:8081/api/v1/payments/momo-return";
    private static final String IPN_URL = "http://localhost:8081/api/v1/payments/momo-ipn";
    private static final String REQUEST_TYPE = "payWithMethod";

    @Value("${access.key}")
    private String accessKey;

    @Value("${secret.key}")
    private String secretKey;


    public String createPaymentRequest(String amount,String ordercode) {
        try {
            String requestId = PARTNER_CODE + new Date().getTime();
            String orderId =ordercode;
            String orderInfo = "Thanh toán MoMo";
            String extraData = "";

            String rawSignature = String.format(
                    "accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                    accessKey, amount, extraData, IPN_URL, orderId, orderInfo, PARTNER_CODE, REDIRECT_URL,
                    requestId, REQUEST_TYPE);

            String signature = signHmacSHA256(rawSignature, secretKey);

            JSONObject requestBody = new JSONObject();
            requestBody.put("partnerCode", PARTNER_CODE);
            requestBody.put("accessKey", accessKey);
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amount);
            requestBody.put("orderId", orderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", REDIRECT_URL);
            requestBody.put("ipnUrl", IPN_URL);
            requestBody.put("extraData", extraData);
            requestBody.put("requestType", REQUEST_TYPE);
            requestBody.put("signature", signature);

            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost("https://test-payment.momo.vn/v2/gateway/api/create");
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));

            CloseableHttpResponse response = client.execute(post);

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null)
                result.append(line);

            JSONObject json = new JSONObject(result.toString());

            return json.getString("payUrl");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String signHmacSHA256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);

        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();

        for (byte b : hash) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1)
                hex.append('0');
            hex.append(h);
        }

        return hex.toString();
    }
}
