package com.example.Titan;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class tang {

    @GetMapping("/time")
    public String redirectToResume() {
        return "forward:/index.html";
    }
}