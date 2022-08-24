package com.example.mongocat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "PatientOverviewServlet", value = "/patients")
public class PatientOverviewServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        MongoClient client = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = client.getDatabase("database");
        MongoCollection<Document> collection = database.getCollection("patients");

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");

        Iterable<Document> iterable = collection.find();
        for (Document doc : iterable) {
            out.println("<h4>Patient: " + doc.get("identifier") + "</h4>");
            out.println("<p>Samples: " + doc.get("samples_amount") + "</p>");
            String href = "/MongoCat_war_exploded/patient/?load=" + doc.get("_id");
            out.println("<button onclick=\"location = '" + href + "'\">View Patient</button>\n");
            out.println("<hr>");
        }
        out.println("<form><input type=\"button\" value=\"Go back!\" onclick=\"location = '/MongoCat_war_exploded/hello-servlet'\"></form>");
        out.println("</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
