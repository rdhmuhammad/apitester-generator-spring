package com.apitester.generator.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class ApiTesterUiController {

    @Value("${server.port:8080}")
    private int defaultPort;

    @GetMapping(value = {"/.env", "/apitester/.env"}, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getEnv(@RequestHeader(value = "Host", required = false) String host) {
        int port = defaultPort;
        if (host != null && host.contains(":")) {
            try {
                port = Integer.parseInt(host.split(":")[1]);
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok("PORT=" + port + "\nVITE_API_URL=/apitester/api/v1\n");
    }
}
