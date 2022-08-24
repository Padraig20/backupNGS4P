package com.example.mongocat;

import com.mongodb.client.*;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.function.Consumer;

@WebServlet(name = "PrintingServlet", value = "/print")
public class PrintingServlet extends HttpServlet {

    private int counter;
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        MongoClient client = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = client.getDatabase("database");
        GridFSBucket gridFSBucket = GridFSBuckets.create(database);

        counter = 0;

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();

        out.println("<html><body>");

        gridFSBucket.find().forEach(new Consumer<GridFSFile>() {
            @Override
            public void accept(final GridFSFile gridFSFile) {
                System.out.println(gridFSFile);
                out.println("<p><b>" + ++counter + ".&ensp;name:&ensp;</b>" + gridFSFile.getFilename() + "<br>" +
                        "&emsp; <b>date:</b>&emsp;" + gridFSFile.getUploadDate() + "<br>" +
                        "&emsp; <b>size:</b> &emsp;" + gridFSFile.getLength() + " Byte<br>" +
                        "&emsp; <b>ID:</b> &emsp;&ensp;" + gridFSFile.getObjectId() + "</p>");

                String href = "/MongoCat_war_exploded/download/?load=" + gridFSFile.getFilename();
                out.println("<button onclick=\"location = '" + href + "'\">Download</button>\n");

                out.println("<hr>");
            }
        });

        out.println("Documents stored in total: " + counter + "<br><br>");

        //delete all form
        out.println("<button onclick=\"del()\"> Delete all Files </button>\n" +
                "\n" +
                "<script>\n" +
                "    async function del() {\n" +
                "        await fetch('DeleteCollectionServlet', {\n" +
                "            method: \"POST\",\n" +
                "        });\n" +
                "        alert('The files were deleted successfully.');\n" +
                "    window.location.reload(true); }\n" +
                "</script>");

        out.println("<form><input type=\"button\" value=\"Go back!\" onclick=\"location = '/MongoCat_war_exploded/hello-servlet'\"></form>");
        out.println("</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
