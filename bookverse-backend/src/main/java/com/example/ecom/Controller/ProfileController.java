package com.example.ecom.Controller;

import com.example.ecom.Reposetory.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final UserRepo users;
    public ProfileController(UserRepo users) { this.users = users; }
    @GetMapping("/me") public Map<String, Object> me(Authentication auth) {
        var user = users.findByEmail(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return Map.of("id", user.getId(), "name", user.getName() == null ? "" : user.getName(), "email", user.getEmail());
    }
}
