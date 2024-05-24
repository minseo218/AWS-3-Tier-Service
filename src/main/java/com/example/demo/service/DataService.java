package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.annotation.PostConstruct;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Configuration
public class DataService {
    @Value("${db.secret.name}")
    private String dbSecretName;
    @Value("${db.url.parameter.name}")
    private String dbUrlParameterName;

    private String dbUrl;
    private String dbUser;
    private String dbPassword;


    @PostConstruct
    public void initialize() {
        dbUrl = "jdbc:mysql://" + getParameterStoreValue(dbUrlParameterName) +"/loan";
        // Secret store 값 가져와서 변수에 넣기
        Region region = Region.AP_SOUTHEAST_1;
        SecretsManagerClient secretsClient = SecretsManagerClient.builder().region(region).build();
        GetSecretValueRequest valueRequest = GetSecretValueRequest.builder().secretId(dbSecretName).build();
        // Secret Manager로부터 Secret Value를 가져오기
        GetSecretValueResponse valueResponse = secretsClient.getSecretValue(valueRequest);
        // Secret Value를 JSON 형태의 문자열로 파싱
        String secretString = valueResponse.secretString();
        JsonObject jsonObject = JsonParser.parseString(secretString).getAsJsonObject();

        dbUser = jsonObject.get("username").getAsString();
        dbPassword = jsonObject.get("password").getAsString();

        // 받아온 값들을 로그로 출력
        log.info("DB URL: {}", dbUrl);
        log.info("DB User: {}", dbUser);
        log.info("DB Password: {}", dbPassword);
    }

    public String getParameterStoreValue(String parameterName) {
        Region region = Region.AP_SOUTHEAST_1;
        SsmClient ssmClient = SsmClient.builder().region(region).build();
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
            log.error("Error getting loan companies", e);
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
