package com.example.ecom.config;

import com.example.ecom.Model.User;
import com.example.ecom.Reposetory.UserRepo;
import com.example.ecom.enums.Provider;
import com.example.ecom.enums.Role;
import com.example.ecom.security.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.util.Optional;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepo userRepo;
    private final JwtUtil jwtUtil;

    public CustomOAuth2SuccessHandler(UserRepo userRepo, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
    }
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauthUser =
                (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        Optional<User> optionalUser =
                userRepo.findByEmail(email);

        User user;

        if (optionalUser.isEmpty()) {

            user = new User();

            user.setName(name);
            user.setEmail(email);
            user.setRole(Role.ROLE_USER);
            user.setProvider(Provider.GOOGLE);

            user = userRepo.save(user);

        } else {
            user = optionalUser.get();
        }

        // Generate JWT
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        // Redirect React with JWT and role
        response.sendRedirect(
                frontendUrl + "/login-success?token="
                        + token
                        + "&role="
                        + user.getRole().name()
        );
    }
}