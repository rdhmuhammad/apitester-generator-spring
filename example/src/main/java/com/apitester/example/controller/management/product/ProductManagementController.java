package com.apitester.example.controller.management.product;

import com.apitester.example.dto.ApiResponse;
import com.apitester.example.dto.ProductRequest;
import com.apitester.example.entity.Product;
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
@RequestMapping("/management/product")
@ApiTester(folders = {"Management"})
public class ProductManagementController {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @GetMapping
    public ApiResponse<List<Product>> getAll() {
        return ApiResponse.ok(new ArrayList<>(products.values()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> getById(@PathVariable Long id) {
        Product product = products.get(id);
        if (product == null) {
            return ApiResponse.error("Product not found");
        }
        return ApiResponse.ok(product);
    }

    @PostMapping
    public ApiResponse<Product> create(@RequestBody ProductRequest request) {
        Product product = Product.builder()
                .id(idGenerator.getAndIncrement())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .build();
        products.put(product.getId(), product);
        return ApiResponse.ok("Product created", product);
    }

    @PutMapping("/{id}")
    public ApiResponse<Product> update(@PathVariable Long id, @RequestBody ProductRequest request) {
        Product product = products.get(id);
        if (product == null) {
            return ApiResponse.error("Product not found");
        }
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        return ApiResponse.ok("Product updated", product);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        if (products.remove(id) == null) {
            return ApiResponse.error("Product not found");
        }
        return ApiResponse.ok("Product deleted", null);
    }
}
