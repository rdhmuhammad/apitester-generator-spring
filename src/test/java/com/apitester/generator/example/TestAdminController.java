package com.apitester.generator.example;

import com.apitester.generator.TestDtoClasses;
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
