package com.zgamelogic.services;


import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public abstract class NotificationService {

    public static void sendNotification(String device, String apnEndpoint){
        String url = apnEndpoint + "/3/device/" + device;
        HttpHeaders headers = new HttpHeaders();
        // TODO authorization token bearer JWT

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        // TODO construct body

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(requestBody, headers), String.class);
    }
}
