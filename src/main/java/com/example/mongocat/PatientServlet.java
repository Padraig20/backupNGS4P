package com.example.mongocat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;

@WebServlet(name = "PatientServlet", value = "/patient/*")
public class PatientServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        MongoClient client = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = client.getDatabase("database");
        MongoCollection<Document> patients = database.getCollection("patients");
        MongoCollection<Document> versionInfo = database.getCollection("vios");
        MongoCollection<Document> samples = database.getCollection("fs.files");

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");

        String url = request.getQueryString();
        String[] arr = url.split("=");

        Document doc = patients.find(Filters.eq("_id", new ObjectId(arr[1]))).first();
        out.println("<h3>" + doc.get("identifier") + "</h3> <hr>");

        try {
            for (int i = 1; i <= Objects.requireNonNull(doc).getInteger("samples_amount"); i++) {
                out.println("<p><b>" + i + ". sample: </b></p>");
                ObjectId id = (ObjectId) doc.get("sample_" + i);
                Document sample = samples.find(Filters.eq("_id", id)).first();
                assert sample != null; //safety measure
                Document docco = (Document) sample.get("metadata");
                System.out.println(docco.toString());
                ObjectId temp = docco.getObjectId("vioId");
                System.out.println(temp.toHexString());
                Document vio = versionInfo.find(Filters.eq("_id", temp)).first();
                System.out.println(vio);
                assert vio != null; //safety measure
                out.println("<p>" +
                        "&emsp; <b>date:</b>&emsp;" + sample.get("uploadDate") + "<br>" +
                        "&emsp; <b>size:</b> &emsp;" + sample.get("length") + " Byte<br>" +
                        "&emsp; <b>ID:</b> &emsp;&ensp;" + sample.get("_id") + "<br>" +
                        "&emsp; <b>HIS:</b>&ensp;&ensp;" + vio.get("HIS_version") + "<br>" +
                        "&emsp; <b>DB: </b> &ensp;&ensp;" + vio.get("db_version")
                        + "</p>");
                String href = "/MongoCat_war_exploded/download/?load=" + sample.get("filename");
                out.println("<button onclick=\"location = '" + href + "'\">Download</button>\n");
                out.println("<hr>");
            }
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        }
        out.println("<form><input type=\"button\" value=\"Go back!\" onclick=\"location = '/MongoCat_war_exploded/patients'\"></form>");
        out.println("</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
