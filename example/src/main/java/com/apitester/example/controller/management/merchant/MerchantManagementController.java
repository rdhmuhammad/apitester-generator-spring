package com.apitester.example.controller.management.merchant;

import com.apitester.example.dto.ApiResponse;
import com.apitester.example.dto.MerchantRequest;
import com.apitester.example.entity.Merchant;
import com.apitester.generator.annotation.ApiTester;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/management/merchant")
@ApiTester(folders = {"Management"})
public class MerchantManagementController {

    private final Map<Long, Merchant> merchants = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @GetMapping
    public ApiResponse<List<Merchant>> getAll() {
        return ApiResponse.ok(new ArrayList<>(merchants.values()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Merchant> getById(@PathVariable Long id) {
        Merchant merchant = merchants.get(id);
        if (merchant == null) {
            return ApiResponse.error("Merchant not found");
        }
        return ApiResponse.ok(merchant);
    }

    @PostMapping
    public ApiResponse<Merchant> create(@RequestBody MerchantRequest request) {
        Merchant merchant = Merchant.builder()
                .id(idGenerator.getAndIncrement())
                .name(request.getName())
                .description(request.getDescription())
                .email(request.getEmail())
                .build();
        merchants.put(merchant.getId(), merchant);
        return ApiResponse.ok("Merchant created", merchant);
    }

    @PutMapping("/{id}")
    public ApiResponse<Merchant> update(@PathVariable Long id, @RequestBody MerchantRequest request) {
        Merchant merchant = merchants.get(id);
        if (merchant == null) {
            return ApiResponse.error("Merchant not found");
        }
        merchant.setName(request.getName());
        merchant.setDescription(request.getDescription());
        merchant.setEmail(request.getEmail());
        return ApiResponse.ok("Merchant updated", merchant);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        if (merchants.remove(id) == null) {
            return ApiResponse.error("Merchant not found");
        }
        return ApiResponse.ok("Merchant deleted", null);
    }
}
