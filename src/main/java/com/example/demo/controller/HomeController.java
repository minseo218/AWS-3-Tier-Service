package com.example.demo.controller;

import com.example.demo.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/app")
public class HomeController {

    @Autowired
    private DataService dataService;

    @GetMapping("/getUserData")
    @ResponseBody
    public String getUserData(@RequestParam String userName) {

        return dataService.calculateLoanData(userName);
    }

    @PostMapping("/saveUserInfo")
    public String saveUserInfo(@RequestBody Map<String, Object> userInfo) {
        String userName = (String) userInfo.get("userName");
        int loanAmount;
        int loanDuration;
        try {
            loanAmount = Integer.parseInt(String.valueOf(userInfo.get("loanAmount")));
            loanDuration = Integer.parseInt(String.valueOf(userInfo.get("loanDuration")));
        } catch (NumberFormatException e) {
            return "Invalid loan amount or loan duration.";
        }

        boolean success = dataService.saveUserInfo(userName, loanAmount, loanDuration);

        if (success) {
            return "User information saved successfully.";
        } else {
            return "Failed to save user information.";
        }
    }
}
