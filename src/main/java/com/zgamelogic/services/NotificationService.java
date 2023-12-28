package com.zgamelogic.services;

import com.zgamelogic.data.services.applePushNotification.ApplePushNotification;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

@Service
@PropertySource("File:msu.properties")
public class NotificationService {

    @Value("${kid}")    private String kid;
    @Value("${org_id}") private String orgId;
    @Value("${APN}")    private String apnEndpoint;

    public void sendNotification(String device, ApplePushNotification notification){
        String url = apnEndpoint + "/3/device/" + device;
        HttpHeaders headers = new HttpHeaders();
        headers.add("authorization", "bearer " + authJWT());
        headers.add("apns-push-type", "alert");
        headers.add("apns-priority", "5");
        headers.add("apns-expiration", "0");
        headers.add("apns-topic", "zgamelogic.Minecraft-Server-Utility");
        RestTemplate restTemplate = new RestTemplate(new OkHttp3ClientHttpRequestFactory());
        restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(notification, headers), String.class);
    }

    private String authJWT() {
        try {
            String privateKeyPEM = new String(Files.readAllBytes(new File("AuthKey_" + kid + ".p8").toPath()));
            privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", ""); // Remove any whitespaces or newlines

            byte[] decodedKey = org.bouncycastle.util.encoders.Base64.decode(privateKeyPEM);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            return Jwts.builder()
                    .setIssuer(orgId)
                    .setIssuedAt(new Date())
                    .signWith(privateKey, SignatureAlgorithm.ES256)
                    .setHeaderParam("kid", kid)
                    .compact();
        } catch (Exception e){

            return "";
        }
    }
}
