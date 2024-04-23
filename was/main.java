import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Main {
    public static void main(String[] args) {
        Server server = new Server(8080);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new SimpleWebServlet()), "/getData");
        context.addServlet(new ServletHolder(new UserInfoServlet()), "/getUserInfo");

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SimpleDatabase db = new SimpleDatabase();
    }
}