package com.example.jar;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class  tank {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "Hello Titan 🚀");
        model.addAttribute("description", "Spring Boot is running successfully!");
        return "hello";
    }
}
