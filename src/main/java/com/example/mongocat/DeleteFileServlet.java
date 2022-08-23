package com.example.mongocat;

import com.mongodb.client.*;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "DeleteFileServlet", value = "/delete/*")
public class DeleteFileServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String message = "<h4>Deleting the requested file was successful!</h4>";
        try{
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
        MongoClient client = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = client.getDatabase("my_database");
        GridFSBucket gridFSBucket = GridFSBuckets.create(database);

        String url = request.getQueryString();
        String[] arr = url.split("=");

        System.out.println("Currently deleting...");

        ObjectId fileId = new ObjectId(arr[1]);
        gridFSBucket.delete(fileId);
    }
}