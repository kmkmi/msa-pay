package com.msapay.common;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class CommonHttpClient {
    private final HttpClient httpClient;
    
    public CommonHttpClient() {
        httpClient = HttpClient.newBuilder().build();
    }
    
    public HttpResponse<String> sendGetRequest(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // 응답 상태 코드 확인하여 에러 응답이면 예외 발생
        if (response.statusCode() >= 400) {
            throw new RuntimeException("External service error: " + response.statusCode() + " - " + response.body());
        }
        
        return response;
    }
    
    public HttpResponse<String> sendPostRequest(String url, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // 응답 상태 코드 확인하여 에러 응답이면 예외 발생
        if (response.statusCode() >= 400) {
            throw new RuntimeException("External service error: " + response.statusCode() + " - " + response.body());
        }
        
        return response;
    }
}

