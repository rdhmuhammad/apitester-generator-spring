package com.apitester.generator.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@ResponseBody
@RequestMapping
public class ApiTesterUiController {

    @GetMapping(value = {"/.env", "/apitester/.env"}, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getEnv(HttpServletRequest request) {
        int port = request.getServerPort();
        return ResponseEntity.ok("PORT=" + port + "\nVITE_API_URL=/apitester/api/v1\n");
    }
}
