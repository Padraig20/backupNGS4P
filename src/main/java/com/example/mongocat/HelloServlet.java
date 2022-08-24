package com.example.mongocat;

import com.mongodb.client.*;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import com.mongodb.client.model.Filters;

import java.io.*;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
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

        //print patients form
        out.println("<h4>Manage your patients</h4>\n" +
                "<button onclick=\"window.location.href='http://localhost:8080/MongoCat_war_exploded/patients';\">\n" +
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
        MongoDatabase database = client.getDatabase("database");
        GridFSBucket gridFSBucket = GridFSBuckets.create(database);

        //create IDs; PATIENT ONLY IF NECESSARY, CONNECT TO PATIENTS COLLECTION FIRST
        ObjectId fileId = new ObjectId();
        ObjectId vioId = new ObjectId();

        System.out.println("Connecting to \"patients\" collection...");

        String[] arr = fileName.split("\\.");

        //patient-collection automatically created when not yet existing
        MongoIterable<String> iterable = database.listCollectionNames();
        Iterator<String> iterator = iterable.iterator();
        boolean equal = false;
        while(iterator.hasNext()) {
            if(iterator.next().equals("patients")) {
                equal = true;
                break;
            }
        }
        if(!equal)
            database.createCollection("patients");

        MongoCollection<Document> collection = database.getCollection("patients");

        Document patientOld = collection.find(Filters.eq("identifier", arr[0])).first();

        ObjectId patientId = new ObjectId();
        if(patientOld != null) {
            patientId = (ObjectId) patientOld.get("_id");
        }

        System.out.println("Currently uploading to database...");
        try (InputStream streamToUploadFrom = filePart.getInputStream()) {
            GridFSUploadOptions options = new GridFSUploadOptions()
                    .chunkSizeBytes(1048576) //1mb
                    .metadata(new Document("type", fileName)
                            .append("patientId", patientId) //connection to patient object
                            .append("vioId", vioId)); //connection to vio
            fileId = gridFSBucket.uploadFromStream(fileName, streamToUploadFrom, options);
            System.out.println("The file id of the uploaded file is: " + fileId.toHexString());
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        }

        System.out.println("Beginning extraction of header-data...");
        //EXTRACTION OF HEADER DATA
        Document patientinfo = new Document().append("_id", patientId);
        Document vio = new Document().append("_id", vioId);

        GZIPInputStream in = new GZIPInputStream(gridFSBucket.openDownloadStream(fileId).batchSize(1)); //1mb, only first chunk is read

        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line = br.readLine();
            while (line.charAt(0) == '#' && line.charAt(1) == '#') {
                String todo = line.substring(2);
                String[] info = todo.split("=");
                if(info[0].equals("db_version") || info[0].equals("HIS_version"))
                    vio.append(info[0], info[1]);
                else if(info[0].equals("lName") || info[0].equals("fName") || info[0].equals("address"))
                    patientinfo.append(info[0], info[1]);
                //goto next line
                line = br.readLine();
            }
        } catch (IOException e) {
            System.err.println("The header extraction failed: " + e.getMessage());
        }

        try {
            patientinfo.append("identifier", arr[0]);

            if(patientOld == null) {
                patientinfo.append("sample_1", fileId); //connection to sample
                patientinfo.append("samples_amount", 1);
                collection.insertOne(patientinfo);
            } else {
                int amount = patientOld.getInteger("samples_amount");
                amount++;
                for (int i = 1; i < amount; i++) {
                    patientinfo.append("sample_" + i, patientOld.get("sample_" + i));
                }
                patientinfo.append("sample_" + amount, fileId);
                patientinfo.append("samples_amount", amount);
                patientinfo.append("_id", patientOld.get("_id"));
                collection.replaceOne(Filters.eq("identifier", arr[0]), patientinfo);
            }

            System.out.println("Connecting to \"vios\" collection...");

            //vios-collection automatically created when not yet existing
            iterable = database.listCollectionNames();
            iterator = iterable.iterator();
            equal = false;
            while(iterator.hasNext()) {
                if(iterator.next().equals("vios")) {
                    equal = true;
                    break;
                }
            }
            if(!equal)
                database.createCollection("vios");
            collection = database.getCollection("vios");

            //Document oldVio = collection.find(Filters.and(Filters.eq("db_version", vio.get("db_version")),
            //        Filters.eq("HIS_version", vio.get("HIS_version")))).first();

            collection.insertOne(vio);

            System.out.println("All successful...");
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }
}