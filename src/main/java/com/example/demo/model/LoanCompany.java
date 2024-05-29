package com.example.demo.model;

public class LoanCompany {
    private String companyName;
    private double interestRate;

    public LoanCompany(String companyName, double interestRate) {
        this.companyName = companyName;
        this.interestRate = interestRate;
    }

    public String getCompanyName() {
        return companyName;
    }

    public double getInterestRate() {
        return interestRate;
    }
}
