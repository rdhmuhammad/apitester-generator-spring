package com.apitester.generator.example;

import com.apitester.generator.TestDtoClasses;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/member")
@com.apitester.generator.annotation.ApiTester(folders = {"Management"})
public class TestMemberController {

    @PostMapping("/online")
    public String createOnline(@RequestBody TestDtoClasses.SimpleQueryDto body) {
        return "ok";
    }

    @GetMapping("/verify/{phone}")
    public String verifyPhone(@PathVariable String phone) {
        return "ok";
    }

    @GetMapping("/search")
    public String search(@RequestParam String keyword,
                         @RequestParam(defaultValue = "1") int page) {
        return "ok";
    }

    @GetMapping("/filter")
    public String filter(TestDtoClasses.SnakeCaseDto filter) {
        return "ok";
    }
}
