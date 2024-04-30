package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataService {
//    static {
//        try {
//            Class.forName("com.mysql.cj.jdbc.Driver");
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException("MySQL JDBC Driver를 찾을 수 없습니다.", e);
//        }
//    }
    private static final String DB_URL = "jdbc:mysql://localhost:3306/loan";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "my-secret-pw";

    public boolean saveUserInfo(String userName, int loanAmount, int loanDuration) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String insertQuery = "INSERT INTO user (name, loan_amount, loan_duration) VALUES (?, ?, ?)";
            try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                insertStatement.setString(1, userName);
                insertStatement.setInt(2, loanAmount);
                insertStatement.setInt(3, loanDuration);
                insertStatement.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<LoanCompany> getLoanCompanies() {
        List<LoanCompany> loanCompanies = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String selectQuery = "SELECT * FROM company";
            try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
                 ResultSet resultSet = selectStatement.executeQuery()) {
                while (resultSet.next()) {
                    String companyName = resultSet.getString("company_name");
                    double interestRate = resultSet.getDouble("interest_rate");
                    loanCompanies.add(new LoanCompany(companyName, interestRate));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return loanCompanies;
    }

    public String calculateLoanData(String userName, int loanAmount, int loanDuration) {
        StringBuilder result = new StringBuilder();
        result.append("User Information:\n");
        result.append("Name: ").append(userName).append("\n");
        result.append("Loan Amount: ").append(loanAmount).append("\n");
        result.append("Loan Duration (months): ").append(loanDuration).append("\n\n");

        result.append("Loan Companies:\n");
        result.append("|Company Name|Interest Rate|Total Interest|\n");

        List<LoanCompany> loanCompanies = getLoanCompanies(); // 데이터베이스에서 회사 정보 가져오기

        String lowestInterestCompany = "";
        double lowestInterestRate = Double.MAX_VALUE;

        for (LoanCompany company : loanCompanies) {
            double interestRate = company.getInterestRate();
            double totalInterest = calculateTotalInterest(loanAmount, loanDuration, interestRate);
            result.append("|").append(company.getCompanyName()).append("|")
                    .append(interestRate).append("|")
                    .append(totalInterest).append("|\n");

            if (interestRate < lowestInterestRate) {
                lowestInterestRate = interestRate;
                lowestInterestCompany = company.getCompanyName();
            }
        }

        result.append("\nLowest Interest Company: <span style='color: orange;'>").append(lowestInterestCompany).append("</span>");

        return result.toString();
    }

    private double calculateTotalInterest(int loanAmount, int loanDuration, double interestRate) {
        return loanAmount * loanDuration * interestRate;
    }

    public static class LoanCompany {
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
}
