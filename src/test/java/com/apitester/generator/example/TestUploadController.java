package com.apitester.generator.example;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/upload")
@com.apitester.generator.annotation.ApiTester(folders = {"Files"})
public class TestUploadController {

    @PostMapping("/file")
    public String upload(@RequestPart("file") MultipartFile file,
                         @RequestParam("name") String name) {
        return "ok";
    }

    @PostMapping("/multi")
    public String uploadMultiple(@RequestPart("files") MultipartFile[] files,
                                 @RequestParam("folder") String folder,
                                 @RequestParam(defaultValue = "false") boolean overwrite) {
        return "ok";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute FormDto form) {
        return "ok";
    }

    @PostMapping("/document")
    public String submitDocument(@ModelAttribute FormDto form,
                                 @RequestPart("attachment") MultipartFile attachment) {
        return "ok";
    }

    public static class FormDto {
        private String username;
        private String email;
        private int age;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }
}
