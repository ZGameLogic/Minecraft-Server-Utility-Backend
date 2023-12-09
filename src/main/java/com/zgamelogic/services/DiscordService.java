package com.zgamelogic.services;

import com.zgamelogic.data.services.discord.DiscordToken;
import com.zgamelogic.data.services.discord.DiscordUser;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public abstract class DiscordService {
    public static DiscordToken postForToken(String code, String clientId, String clientSecret, String redirectUri){
        String url = "https://discord.com/api/oauth2/token";
        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        headers.add("Accept-Encoding", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("code", code);
        requestBody.add("redirect_uri", redirectUri);
        ResponseEntity<DiscordToken> response = restTemplate.exchange(url, HttpMethod.POST,  new HttpEntity<>(requestBody, headers), DiscordToken.class);
        return response.getBody();
    }

    public static DiscordToken refreshToken(String refreshToken, String clientId, String clientSecret){
        String url = "https://discord.com/api/oauth2/token";
        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        headers.add("Accept-Encoding", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("grant_type", "refresh_token");
        requestBody.add("refresh_token", refreshToken);
        ResponseEntity<DiscordToken> response = restTemplate.exchange(url, HttpMethod.POST,  new HttpEntity<>(requestBody, headers), DiscordToken.class);
        return response.getBody();
    }

    public static DiscordUser getUserFromToken(String token){
        String url = "https://discord.com/api/users/@me";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        ResponseEntity<DiscordUser> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), DiscordUser.class);
        return response.getBody();
    }
}
