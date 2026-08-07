package com.apitester.generator;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@com.apitester.generator.annotation.ApiTester(folders = {"Admin", "Dashboard"})
public class TestAdminController {

    @PostMapping("/create")
    public String create(@RequestBody TestDtoClasses.CustomSetterDto body) {
        return "ok";
    }

    @GetMapping("/list")
    public String list(TestDtoClasses.JsonPropertyDto filter) {
        return "ok";
    }
}
