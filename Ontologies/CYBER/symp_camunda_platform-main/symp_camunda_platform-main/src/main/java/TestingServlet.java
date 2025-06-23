import com.google.gson.Gson;
import de.fraunhofer.camunda.javaserver.external.ExternalWorkerTopics;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestingServlet extends HttpServlet {

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    PrintWriter out = response.getWriter();
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    String topics = new Gson().toJson(ExternalWorkerTopics.values());
    out.println(topics);
    out.flush();
  }

  public String getTopicsAsString() {
    return new Gson().toJson(ExternalWorkerTopics.values());
  }
}
