package com.example.demo.controller.index;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.demo.service.DataService;

import java.util.Map;

@RestController
class HomeController {

    @Autowired
    private DataService dataService;
    @GetMapping("/getData")
    @ResponseBody
    public String getData(@RequestParam String userName, @RequestParam int loanAmount, @RequestParam int loanDuration) {
        return dataService.calculateLoanData(userName, loanAmount, loanDuration);
    }
    @PostMapping("/saveUserInfo")
    public String saveUserInfo(@RequestBody Map<String, Object> userInfo) {
        String userName = (String) userInfo.get("userName");

        // 대출 금액과 대출 기간을 정수로 변환
        int loanAmount;
        int loanDuration;
        try {
            loanAmount = Integer.parseInt(String.valueOf(userInfo.get("loanAmount")));
            loanDuration = Integer.parseInt(String.valueOf(userInfo.get("loanDuration")));
        } catch (NumberFormatException e) {
            return "Invalid loan amount or loan duration."; // 형식이 올바르지 않을 경우 처리
        }

        // 사용자 정보 저장
        boolean success = dataService.saveUserInfo(userName, loanAmount, loanDuration);

        if (success) {
            return dataService.calculateLoanData(userName, loanAmount, loanDuration);
        } else {
            return "Failed to save user information.";
        }
    }

}
