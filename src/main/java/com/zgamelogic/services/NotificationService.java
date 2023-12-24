package com.zgamelogic.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zgamelogic.data.services.applePushNotification.ApplePushNotification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;

public abstract class NotificationService {

    public static void sendNotification(String device, String jwt, String apnEndpoint){
        String url = apnEndpoint + "/3/device/" + device;
        System.out.println(url);
        HttpHeaders headers = new HttpHeaders();
        headers.add("authorization", "bearer " + jwt);
        headers.add("apns-push-type", "alert");
        headers.add("apns-priority", "5");
        headers.add("apns-topic", "zgamelogic.Minecraft-Server-Utility");
        ApplePushNotification notification = new ApplePushNotification("Test notification");
        try {
            System.out.println(new ObjectMapper().writeValueAsString(notification));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> e = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(notification, headers), String.class);
        System.out.println(e.getStatusCode());
        System.out.println(e.getBody());
    }

    private static String authJWT() {
//        Path keyFilePath = Paths.get("AuthKey_DR4XM76JWJ.p8");
//        try {
//            String privateKey = Files.readString(keyFilePath, StandardCharsets.UTF_8);
//            Algorithm algorithm = Algorithm.RSA256(null, );
//
//            return JWT.create()
//                    .withIssuer("your-issuer")
//                    .withAudience("your-audience")
//                    .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 100))
//                    .sign(algorithm);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        return "";
    }
}
