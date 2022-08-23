package com.example.mongocat;

import com.mongodb.client.*;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.io.*;
import java.util.function.Consumer;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

@WebServlet(name = "helloServlet", value = "/hello-servlet")
@MultipartConfig(
        fileSizeThreshold = 0,
        maxFileSize = -1L,
        maxRequestSize = -1L
)
public class HelloServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        out.println("<html><body>");

        //upload form
        out.println("<h4>Upload a new file</h4>\n" +
                "\n" +
                "<input id=\"ajaxfile\" type=\"file\"/> <br/>\n" +
                "<button onclick=\"uploadFile()\"> Upload </button>\n" +
                "\n" +
                "<script>\n" +
                "    async function uploadFile() {\n" +
                "        let formData = new FormData();\n" +
                "        formData.append(\"file\", ajaxfile.files[0]);\n" +
                "        alert('Request verified, currently uploading...');\n" +
                "        await fetch('hello-servlet', {\n" +
                "            method: \"POST\",\n" +
                "            body: formData\n" +
                "        });\n" +
                "        alert('The file was uploaded successfully.');\n" +
                "    }\n" +
                "</script>");


        //print all form
        out.println("<h4>Manage your files</h4>\n" +
                "<button onclick=\"window.location.href='http://localhost:8080/MongoCat_war_exploded/print';\">\n" +
                "    View\n" +
                "</button>");

        //search form
        out.println("<h4>Search your files</h4>\n" +
                "\n" +
                "<form action=\"/MongoCat_war_exploded/search/\">\n" +
                "  <label for=\"search\">Specify:</label>\n" +
                "  <input type=\"search\" id=\"search\" name=\"search\"><br><br>\n" +
                "  <button onclick=\"search()\" id=\"connect\">Search</button>" +
                "</form>\n" +
                "\n" +
                "<p>Click the \"Search\" button and your input will be searched for from the database.</p>");
        out.println("<script type=\"text/javascript\">\n" +
                "function search(){\n" +
                "    // get the user input\n" +
                "    var user_value = document.getElementById('search').value;\n" +
                "    // check the validation\n" +
                "    if(user_value!=''){\n" +
                "        // redirect\n" +
                "       location.href='rooms/' + user_value;\n" +
                "    }else{\n" +
                "        // alert error message\n" +
                "        alert(\"validation error\");\n" +
                "    }\n" +
                "}\n" +
                "</script>");

        out.println("</body></html>");
    }

    //used for uploading
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("Request verified...");

        Part filePart = req.getPart("file");
        String fileName = filePart.getSubmittedFileName();

        System.out.println("Establishing connection to database...");

        MongoClient client = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = client.getDatabase("my_database");
        GridFSBucket gridFSBucket = GridFSBuckets.create(database);

        System.out.println("Currently uploading to database...");
        try (InputStream streamToUploadFrom = filePart.getInputStream()) {
            GridFSUploadOptions options = new GridFSUploadOptions()
                    .chunkSizeBytes(1048576) //1mb
                    .metadata(new Document("type", fileName));
            ObjectId fileId = gridFSBucket.uploadFromStream(fileName, streamToUploadFrom, options);
            System.out.println("The file id of the uploaded file is: " + fileId.toHexString());


            Bson query = Filters.eq("_id", fileId);
            gridFSBucket.find(query)
                    .forEach(new Consumer<GridFSFile>() {
                        @Override
                        public void accept(final GridFSFile gridFSFile) {
                            System.out.println("\n\nFOLLOWING IS IMPORTANT: \n\n");

                            InputStream in = gridFSBucket.openDownloadStream(fileId).batchSize(1); //1mb, first chunk

                            try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                                String temp = br.readLine();
                                while (temp.charAt(0) == '#' && temp.charAt(1) == '#') {
                                    System.out.println(temp); //do something
                                    temp = br.readLine();
                                }
                            } catch (IOException e) {
                                System.err.println("The header extraction failed: " + e.getMessage());
                            }
                        }
                    });
        } catch (Exception e) {
            System.err.println("The file upload failed: " + e);
        }
    }
}