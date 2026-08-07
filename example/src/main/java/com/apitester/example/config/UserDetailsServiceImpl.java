package com.apitester.example.config;

import com.apitester.example.entity.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = users.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), new ArrayList<>());
    }

    public User register(String username, String password, String email, PasswordEncoder passwordEncoder) {
        if (users.containsKey(username)) {
            throw new RuntimeException("Username already exists");
        }
        User user = User.builder()
                .id(idGenerator.getAndIncrement())
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .build();
        users.put(username, user);
        return user;
    }
}
