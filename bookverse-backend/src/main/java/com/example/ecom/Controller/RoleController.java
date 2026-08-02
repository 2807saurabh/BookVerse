package com.example.ecom.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/role")
public class RoleController {

    @GetMapping("/user")
    public String user() {
        return "Welcome User!";
    }

    @GetMapping("/admin")
    public String admin() {
        return "Welcome Admin!";
    }
}