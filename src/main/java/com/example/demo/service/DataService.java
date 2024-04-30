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
        result.append("<h2>User Information</h2>");
        result.append("<p><strong>Name:</strong> ").append(userName).append("</p>");
        result.append("<p><strong>Loan Amount:</strong> ").append(loanAmount).append("</p>");
        result.append("<p><strong>Loan Duration (months):</strong> ").append(loanDuration).append("</p>");

        result.append("<h2>Loan Companies</h2>");
        result.append("<table border=\"1\">");
        result.append("<tr><th>Company Name</th><th>Interest Rate</th><th>Total Interest</th></tr>");

        List<LoanCompany> loanCompanies = getLoanCompanies(); // 데이터베이스에서 회사 정보 가져오기

        String lowestInterestCompany = "";
        double lowestInterestRate = Double.MAX_VALUE;

        for (LoanCompany company : loanCompanies) {
            double interestRate = company.getInterestRate();
            double totalInterest = calculateTotalInterest(loanAmount, loanDuration, interestRate);
            result.append("<tr>");
            // 가장 낮은 금리를 가진 회사인 경우에는 색을 변경합니다.
            if (interestRate == lowestInterestRate) {
                result.append("<td style='background-color: orange; color: white;'>");
            } else {
                result.append("<td>");
            }
            result.append(company.getCompanyName()).append("</td>")
                    .append("<td>").append(interestRate).append("</td>")
                    .append("<td>").append(totalInterest).append("</td>")
                    .append("</tr>");

            if (interestRate < lowestInterestRate) {
                lowestInterestRate = interestRate;
                lowestInterestCompany = company.getCompanyName();
            }
        }

        result.append("</table>");
        // 이미 표에서 행으로 출력되었기 때문에 추가로 출력하지 않고, 색을 변경해줍니다.
        result.append("<p><span style='background-color: orange; color: white;'>").append(lowestInterestCompany).append("</span></p>");

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
