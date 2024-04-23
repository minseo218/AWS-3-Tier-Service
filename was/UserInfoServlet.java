import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import com.google.gson.*;
//parameter store
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

public class UserInfoServlet extends HttpServlet {

    //parameter Store 값 가져오기 
    private static final SsmClient ssmClient = SsmClient.builder().build();

    private static final String JDBC_URL = getParameterValue("db_hostname");
    private static final String USER = getParameterValue("db_username");
    private static final String PASS = getParameterValue("db_password");

    private static String getParameterValue(String parameterName) {
        GetParameterRequest parameterRequest = GetParameterRequest.builder()
            .name(parameterName)
            .withDecryption(true)
            .build();

        return ssmClient.getParameter(parameterRequest).parameter().value();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        String data = getDataFromDB();
        out.println("{ \"message\": \"" + data + "\" }");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        String payload = buffer.toString();

        JsonObject jsonObject = new Gson().fromJson(payload, JsonObject.class);
        String name = jsonObject.get("name").getAsString();
        String email = jsonObject.get("email").getAsString();

        saveUserInfoToDB(name, email);

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("name", name);
        responseJson.addProperty("email", email);
        out.println(responseJson.toString());
    }

    public String getDataFromDB() {
        // ... (기존 getDataFromDB() 메서드 코드)
    }

    public void saveUserInfoToDB(String name, String email) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DriverManager.getConnection(JDBC_URL, USER, PASS);
            String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}