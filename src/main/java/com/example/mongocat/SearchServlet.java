package com.example.mongocat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.TextSearchOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;

@WebServlet(name = "SearchServlet", value = "/search/*")
public class SearchServlet extends HttpServlet {
    private int counter;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        MongoClient client = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = client.getDatabase("samples");
        GridFSBucket gridFSBucket = GridFSBuckets.create(database);
        database.createCollection("test");
        //MongoCollection<Document> test= database.getCollection("test");

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();

        String url = request.getQueryString();
        String[] arr = url.split("=");

        out.println("<html><body>");
        out.println("<h4>Following files have been found regarding '" + arr[1] + "': </h4>");

        counter = 0;

        TextSearchOptions opt = new TextSearchOptions();
        opt.caseSensitive(false);

        Bson query = Filters.eq("filename", arr[1]);
        Bson sort = Sorts.ascending("filename");
        gridFSBucket.find(query)
                .sort(sort)
                .forEach(new Consumer<GridFSFile>() {
                    @Override
                    public void accept(final GridFSFile gridFSFile) {
                        System.out.println(gridFSFile);
                        out.println("<p><b>" + ++counter + ".&ensp;name:&ensp;</b>" + gridFSFile.getFilename() + "<br>" +
                                "&emsp; <b>date:</b>&emsp;" + gridFSFile.getUploadDate() + "<br>" +
                                "&emsp; <b>size:</b> &emsp;" + gridFSFile.getLength() + " Byte<br>" +
                                "&emsp; <b>ID:</b> &emsp;&ensp;" + gridFSFile.getObjectId() + "</p>");
                        String href = "/MongoCat_war_exploded/download/?load=" + gridFSFile.getFilename();
                        out.println("<button onclick=\"location = '" + href + "'\">Download</button>\n");
                        href = "/MongoCat_war_exploded/delete/?load=" + gridFSFile.getObjectId();
                        out.println("<button onclick=\"location = '" + href + "'\">Delete</button>\n");

                        out.println("<br>");

                        //rename form
                        String[] temp = gridFSFile.getFilename().split("\\.");
                        out.println("<form action=\"/MongoCat_war_exploded/rename/=" + gridFSFile.getObjectId() + "=" + temp[1] + "=\">\n" +
                                "  <label for=\"name\"></label>\n" +
                                "  <input type=\"name\" id=\"name\" name=\"name\">\n" +
                                "  <button id=\"connect\">Rename</button>" +
                                "</form>");

                        out.println("<hr>");
                    }
                });

        out.println("Documents found in total: " + counter + "<br><br>");
        out.println("<form><input type=\"button\" value=\"Go back!\" onclick=\"location = '/MongoCat_war_exploded/hello-servlet'\"></form>");
        out.println("</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
