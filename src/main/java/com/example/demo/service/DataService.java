package com.example.demo.service;

import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;

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
    public boolean saveUserInfo(String userName, int loanAmount, int loanDuration) {
        String dbUrlParameterName = "/3Tier/db/endpoint";
        String dbUserSecretName = "rds!cluster-8dd9b92d-9975-4258-b2c6-47bfad20baf4";

        String dbUrl = getParameterStoreValue(dbUrlParameterName);
        String dbUser = getSecretValue(dbUserSecretName, "username");
        String dbPassword = getSecretValue(dbUserSecretName, "password");

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
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
        String dbUrl = getParameterStoreValue("YOUR_DB_URL_PARAMETER_NAME");
        String dbUser = getSecretValue("YOUR_DB_USER_SECRET_NAME");
        String dbPassword = getSecretValue("YOUR_DB_PASSWORD_SECRET_NAME");

        List<LoanCompany> loanCompanies = new ArrayList<>();
        // Connection 객체 생성
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             // selectStatement 객체 생성 및 쿼리 실행
             Statement selectStatement = connection.createStatement();
             ResultSet resultSet = selectStatement.executeQuery("SELECT company_name, interest_rate FROM loan_companies")) {
            while (resultSet.next()) {
                String companyName = resultSet.getString("company_name");
                double interestRate = resultSet.getDouble("interest_rate");
                loanCompanies.add(new LoanCompany(companyName, interestRate));
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

    private String getSecretValue(String secretName) {
        String secretValue = null;
        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secretName);
        GetSecretValueResult getSecretValueResult = null;

        try {
            getSecretValueResult = AWSSecretsManagerClientBuilder.defaultClient().getSecretValue(getSecretValueRequest);
        } catch (Exception e) {
            throw e;
        }

        if (getSecretValueResult.getSecretString() != null) {
            secretValue = getSecretValueResult.getSecretString();
        }

        return secretValue;
    }

    private String getParameterStoreValue(String parameterName) {
        String parameterValue = null;
        GetParameterRequest getParameterRequest = new GetParameterRequest().withName(parameterName).withWithDecryption(true);
        GetParameterResult getParameterResult = null;

        try {
            getParameterResult = AWSSimpleSystemsManagementClientBuilder.defaultClient().getParameter(getParameterRequest);
        } catch (Exception e) {
            throw e;
        }

        if (getParameterResult.getParameter() != null) {
            parameterValue = getParameterResult.getParameter().getValue();
        }

        return parameterValue;
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
