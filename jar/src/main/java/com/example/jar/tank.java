package com.example.jar;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class tank {

    @GetMapping("/")
    public String home() {
        return "hello.html";   // Calls hello.html
    }
}
