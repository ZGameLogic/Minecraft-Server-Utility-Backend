package com.zgamelogic.services;

import com.zgamelogic.data.services.discord.DiscordToken;
import com.zgamelogic.data.services.discord.DiscordUser;
import com.zgamelogic.data.services.discord.MessagePacket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@PropertySource("File:msu.properties")
public class DiscordService {

    @Value("${client.id}") private String discordClientId;
    @Value("${client.secret}") private String discordClientSecret;
    @Value("${redirect.url}") private String discordRedirectUrl;

    public DiscordToken postForToken(String code){
        String url = "https://discord.com/api/oauth2/token";
        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        headers.add("Accept-Encoding", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", discordClientId);
        requestBody.add("client_secret", discordClientSecret);
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("code", code);
        requestBody.add("redirect_uri", discordRedirectUrl);
        ResponseEntity<DiscordToken> response = restTemplate.exchange(url, HttpMethod.POST,  new HttpEntity<>(requestBody, headers), DiscordToken.class);
        return response.getBody();
    }

    public DiscordToken refreshToken(String refreshToken){
        String url = "https://discord.com/api/oauth2/token";
        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        headers.add("Accept-Encoding", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", discordClientId);
        requestBody.add("client_secret", discordClientSecret);
        requestBody.add("grant_type", "refresh_token");
        requestBody.add("refresh_token", refreshToken);
        ResponseEntity<DiscordToken> response = restTemplate.exchange(url, HttpMethod.POST,  new HttpEntity<>(requestBody, headers), DiscordToken.class);
        return response.getBody();
    }

    public DiscordUser getUserFromToken(String token){
        String url = "https://discord.com/api/users/@me";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        ResponseEntity<DiscordUser> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), DiscordUser.class);
        return response.getBody();
    }

    public static void updateMessage(String message){
        String url = "https://zgamelogic.com:2000/message";
        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();
        MessagePacket messagePacket = new MessagePacket(
                330751526735970305L,
                1185639873709879388L,
                message,
                new String[]{"1062120824770936895"}
        );
        restTemplate.exchange(url, HttpMethod.POST,  new HttpEntity<>(messagePacket, headers), String.class);
    }
}
