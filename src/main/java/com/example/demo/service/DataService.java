package com.example.demo.service;

import com.example.demo.model.LoanCompany;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import javax.annotation.PostConstruct;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DataService {

    @Value("${db.secret.name}")
    private String dbSecretName;
    @Value("${db.url.parameter.name}")
    private String dbUrlParameterName;

    private final SecretsManagerClient secretsManagerClient;
    private final SsmClient ssmClient;

    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    public DataService(SecretsManagerClient secretsManagerClient, SsmClient ssmClient) {
        this.secretsManagerClient = secretsManagerClient;
        this.ssmClient = ssmClient;
    }

    @PostConstruct
    public void initialize() {
        dbUrl = "jdbc:mysql://" + getParameterStoreValue(dbUrlParameterName) +"/demo";
        GetSecretValueRequest valueRequest = GetSecretValueRequest.builder().secretId(dbSecretName).build();
        GetSecretValueResponse valueResponse = secretsManagerClient.getSecretValue(valueRequest);
        String secretString = valueResponse.secretString();
        JsonObject jsonObject = JsonParser.parseString(secretString).getAsJsonObject();

        dbUser = jsonObject.get("username").getAsString();
        dbPassword = jsonObject.get("password").getAsString();

        log.info("DB URL: {}", dbUrl);
        log.info("DB User: {}", dbUser);
        log.info("DB Password: {}", dbPassword);
    }

    public String getParameterStoreValue(String parameterName) {
        GetParameterRequest parameterRequest = GetParameterRequest.builder().name(parameterName).withDecryption(true).build();
        GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);
        return parameterResponse.parameter().value();
    }

    public boolean saveUserInfo(String userName, int loanAmount, int loanDuration) {
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
            log.error("Error saving user info", e);
            return false;
        }
    }

    public List<LoanCompany> getLoanCompanies() {
        List<LoanCompany> loanCompanies = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement selectStatement = connection.createStatement();
             ResultSet resultSet = selectStatement.executeQuery("SELECT company_name, interest_rate FROM loan_companies")) {
            while (resultSet.next()) {
                String companyName = resultSet.getString("company_name");
                double interestRate = resultSet.getDouble("interest_rate");
                loanCompanies.add(new LoanCompany(companyName, interestRate));
            }
        } catch (SQLException e) {
            log.error("Error getting loan companies", e);
        }
        return loanCompanies;
    }

    public String calculateLoanData(String userName) {
        StringBuilder result = new StringBuilder();
        User user = getUserByName(userName);
        if (user == null) {
            return "<p>User not found</p>";
        }

        result.append("<h2>User Information</h2>");
        result.append("<p><strong>Name:</strong> ").append(user.getName()).append("</p>");
        result.append("<p><strong>Loan Amount:</strong> ").append(user.getLoanAmount()).append("</p>");
        result.append("<p><strong>Loan Duration (months):</strong> ").append(user.getLoanDuration()).append("</p>");

        result.append("<h2>Loan Companies</h2>");
        result.append("<table class='table table-bordered'>");
        result.append("<thead><tr><th>Company Name</th><th>Interest Rate</th><th>Total Interest</th></tr></thead>");
        result.append("<tbody>");

        List<LoanCompany> loanCompanies = getLoanCompanies();

        String lowestInterestCompany = "";
        double lowestInterestRate = Double.MAX_VALUE;

        for (LoanCompany company : loanCompanies) {
            double interestRate = company.getInterestRate();
            double totalInterest = calculateTotalInterest(user.getLoanAmount(), user.getLoanDuration(), interestRate);
            result.append("<tr>");
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

        result.append("</tbody></table>");
        result.append("<p>The company with the lowest interest rate is: <strong>")
                .append(lowestInterestCompany)
                .append("</strong></p>");

        return result.toString();
    }

    private User getUserByName(String userName) {
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement selectStatement = connection.prepareStatement("SELECT name, loan_amount, loan_duration FROM user WHERE name = ?")) {
            selectStatement.setString(1, userName);
            try (ResultSet resultSet = selectStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new User(
                            resultSet.getString("name"),
                            resultSet.getInt("loan_amount"),
                            resultSet.getInt("loan_duration")
                    );
                }
            }
        } catch (SQLException e) {
            log.error("Error getting user by name", e);
        }
        return null;
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

    public static class User {
        private String name;
        private int loanAmount;
        private int loanDuration;

        public User(String name, int loanAmount, int loanDuration) {
            this.name = name;
            this.loanAmount = loanAmount;
            this.loanDuration = loanDuration;
        }

        public String getName() {
            return name;
        }

        public int getLoanAmount() {
            return loanAmount;
        }

        public int getLoanDuration() {
            return loanDuration;
        }
    }
}
