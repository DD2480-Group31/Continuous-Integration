package com.ci;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
 
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.io.File;
 
import org.eclipse.jetty.server.Server;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONException;
import org.json.JSONObject;

import org.eclipse.jgit.api.Git;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;


/** 
 Skeleton of a ContinuousIntegrationServer which acts as webhook
 See the Jetty documentation for API documentation of those classes.
*/ 
public class ContinuousIntegrationServer extends AbstractHandler {  
    final static String DIR_PATH = "target";
    final static String CI_CONTEXT = "Custom CI Server";
    final static String testXMLDIR_PATH = DIR_PATH + "/build/test-results/test/";

    final private String TOKEN;
    final private String MAIN_BRANCH;

    String repOwner;
    String repName;
    String sha;
    private String repoCloneURL;
    private String branch;
    
    private JSONObject pushRequest;
    private Git repository;

    enum CommitStatus {
        error,
        failure,
        pending,
        success
    }

    enum BuildStatus {
        success,
        buildFail,
        testFail
    }

    public ContinuousIntegrationServer(String statusToken, String mainBranch) {
        TOKEN = statusToken;
        MAIN_BRANCH = mainBranch;
    }

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException, ServletException
    {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        
        System.out.println(target);

        pushRequest = new JSONObject(request.getReader().lines().collect(Collectors.joining()));

        repOwner = pushRequest.getJSONObject("repository").getJSONObject("owner").getString("name");
        repName = pushRequest.getJSONObject("repository").getString("name");
        sha = pushRequest.getString("after");
        repoCloneURL = pushRequest.getJSONObject("repository").getString("clone_url");
        branch = pushRequest.getString("ref").split("/")[2];

        // Respond to the Github servers before building anything:
        if (sha.matches("^0+$")) {
            response.getWriter().println("SHA was zero, no build was tested.");
            return;
        } else {
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
            var out = response.getWriter();
            out.println("Recieved push request, building project...");
            out.flush();
            out.close();
        }

        // Set pending status
        postStatus(CommitStatus.pending, "Building repository and running tests...");

        try {
            // Update target repository and checkout to the correct branch.
            repository = GitUtils.updateTarget(repoCloneURL, branch, MAIN_BRANCH);
            // Build the cloneld repository
            this.build();
        } catch (Exception e) {
            e.printStackTrace();
            postStatus(CommitStatus.error, "CI server encountered an error");

            // Close the repository if we're returning early
            if (repository != null) {
                repository.close();
            }
            return;
        }
        
        BuildStatus res = analyzeResults();
        switch (res) {
            case buildFail:
                postStatus(CommitStatus.failure, "Build failed");
                break;
            case testFail:
                postStatus(CommitStatus.failure, "Build complete but one or more tests failed");
                break;
            default:
                postStatus(CommitStatus.success, "Build complete and all tests passed");
        }
        repository.close();
    }


    //Method for JUnit to initially try
    public void gradleTest(){
        System.out.println("Gradle/JUnit works");
    }
 

    /**
     * Builds the branch that was cloned into the target directory.
     */
    private void build() throws IOException, InterruptedException {
        String[] arguments = {"./gradlew", "build"};
        Process process = Runtime.getRuntime().exec(arguments, null, new File(DIR_PATH));
        process.waitFor();
    }

    /**
     * Set the commmit status for the current repository and SHA specified by the 
     * <code>repOwner</code>, <code>repName</code>, and <code>sha</code> fields respectively.
     * 
     * @param status the status of the commit message
     * @param description a more helpful description of the status
     * @throws IOException
     * @throws Error if all neccessary fields are not set.
     * @return the response code for the post request.
     */
    public int postStatus(CommitStatus status, String description) throws IOException, Error {
        if (repOwner == null || repName == null || sha == null) {
            throw new Error("One or more of the necessary fields `repOwner`, `repName`, and `sha` is not set.");
        }
        // API enpoint for setting the commit status  
        URL url = new URL(String.format("https://api.github.com/repos/%s/%s/statuses/%s", repOwner, repName, sha));
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Accept", "application/vnd.github+json"); // Recommended header
        con.setRequestProperty("Authorization", "Bearer " + TOKEN);
        con.setDoOutput(true);

        // Add status and description to body:
        JSONObject body = new JSONObject();
        body.put("state", status.toString());
        body.put("description", description);
        body.put("context", CI_CONTEXT);

        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        out.writeBytes(body.toString());
        out.flush();
        out.close();

        // Send request
        con.connect();

        // Check response code
        int code = con.getResponseCode();
        if (code != 201) {
            System.out.println(String.format("Error when setting commit status! (%d)", code));
            System.out.println(con.getResponseMessage());
        }
        con.disconnect();
        return code;
    }

    /**
     * Read the XML document containing the results of tests.
     * @return The resulting </code>BuildStatus res</code> of analyzing the test results of the build. 
     * </code>BuildStatus.buildFail</code> if an exception is thrown when parsing the XML document,
     * </code>BuildStatus.testFail</code> if the number of failed tests is > 0 and </code>BuildStatus.success</code>
     * if the number of failed tests is 0.
     */
    private BuildStatus analyzeResults(){
        Document[] docs = null;
        BuildStatus res = BuildStatus.success;
        try{
            docs = parseXML();
            for(int i = 0; i < docs.length; i++){
                if(docs[i] == null){
                    continue;
                }
                Node node = docs[i].selectSingleNode("/testsuite"); //select 'testuite' element
                int failures = Integer.parseInt(node.valueOf("@failures")); //select 'failures' attribute
                if(failures > 0){
                    //a test has failed, status must be testFail
                    res = BuildStatus.testFail;
                    break;
                }
            }
        }catch(DocumentException dE){
            //Something went wrong with the XML document, something went wrong before testing.
            res = BuildStatus.buildFail;
        }       
        return res;
    }
    /**
     * Helper function to get J4DOM document object from XML document.
     * @return Array of object </code>Doducment</code> containing XML document with test results.
     */
    private Document[] parseXML() throws DocumentException { 
        SAXReader reader = new SAXReader();
        File filePath = new File(testXMLDIR_PATH);
        File[] allFiles = filePath.listFiles();
        Document[] docs = new Document[allFiles.length]; 
        for(int i = 0; i < docs.length; i++){
            if(!allFiles[i].isDirectory()){
                docs[i] = reader.read(allFiles[i]);
            }
            else{
                docs[i] = null;
            }
        }
        return docs;
    }

    /**
     * Deletes all the contents within the target directory
     * @param targetDir Filepath to the directory to be deleted
     */
    private static void cleanup(File targetDir) {
        File[] allContents = targetDir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                cleanup(file);
            }
        }
        targetDir.delete();
    }

    /**
     * Helper-method to specifically delete the `build`
     * directory within the `target` directory.
     */
    public static void cleanBuild() throws NotDirectoryException{
        Path targetDir = FileSystems.getDefault().getPath("./target/build");
        if(Files.isDirectory(targetDir) == false){
            throw new NotDirectoryException("The directory does not exist.");
        }
        cleanup(targetDir.toFile());
    }

    /**
     * Helper-method to specifically delete the entire `target` directory.
     */
    public static void cleanTarget() throws NotDirectoryException{
        Path targetDir = FileSystems.getDefault().getPath("./target");
        if(Files.isDirectory(targetDir) == false){
            throw new NotDirectoryException("The directory does not exist.");
        }
        cleanup(targetDir.toFile());
    }

    // used to start the CI server in command line
    public static void main(String[] args) throws Exception
    {    
        JSONObject conf = new JSONObject(Files.readString(Path.of("config.json")));
        int port = conf.getInt("port");
        String statusToken = conf.getString("status-token");
        String mainBranch = conf.getString("main-branch");
        Server server = new Server(port);
        server.setHandler(new ContinuousIntegrationServer(statusToken, mainBranch)); 
        server.start();
        server.join();
    }
}
