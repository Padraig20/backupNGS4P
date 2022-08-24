package com.example.mongocat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSDownloadOptions;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "DownloadServlet", value = "/download/*")
public class DownloadServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String message = "<h4>Download was successful!</h4>";
        System.out.println("Currently downloading...");
        try {
            doPost(request, response);
        } catch (Exception e) {
            message = "<h4><b>ERROR:</b> " + e.getMessage() + "</h4>";
        }
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println("<html><body>");
        out.println(message);
        out.println("<form><input type=\"button\" value=\"Go back!\" onclick=\"window.location=document.referrer;\"></form>");
        out.println("</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String url = request.getQueryString();
        String[] arr = url.split("=");

        MongoClient client = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = client.getDatabase("database");

        GridFSBucket gridFSBucket = GridFSBuckets.create(database);

        GridFSDownloadOptions downloadOptions = new GridFSDownloadOptions().revision(0);
        try (FileOutputStream streamToDownloadTo = new FileOutputStream("C:\\Users\\Admin\\Downloads\\" + arr[1])) {
            gridFSBucket.downloadToStream(arr[1], streamToDownloadTo, downloadOptions);
            streamToDownloadTo.flush();
        }
    }
}
