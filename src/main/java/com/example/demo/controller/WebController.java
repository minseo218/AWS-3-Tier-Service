package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app")
public class WebController {

    @GetMapping("/home")
    public String getHomePage() {
        return "home";
    }
    @GetMapping("/userInfo")
    public String getUserInfoPage() {
        return "userInfo";
    }
    @GetMapping("/compareData")
    public String getCompareDataPage() {
        return "compareData";
    }
}
