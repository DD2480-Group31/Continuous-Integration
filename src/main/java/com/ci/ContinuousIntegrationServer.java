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
import java.nio.file.Path;
import java.io.File;
 
import org.eclipse.jetty.server.Server;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.Node;
/** 
 Skeleton of a ContinuousIntegrationServer which acts as webhook
 See the Jetty documentation for API documentation of those classes.
*/
public class ContinuousIntegrationServer extends AbstractHandler
{  
    final static int GROUP_NUMBER = 31;
    final static int PORT = 8000 + GROUP_NUMBER;

    private String TOKEN;

    private String repOwner;
    private String repName;
    private String sha;
    private String repoCloneURL;
    private String branch;
    private String dirPath = "./target";
    
    private JSONObject pushRequest;

    enum CommitStatus {
        error,
        failure,
        pending,
        success
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

        // here you do all the continuous integration tasks
        // for example
        // 1st clone your repository
        // 2nd compile the code
        response.getWriter().println("CI job done");
    }

    //Method for JUnit to initially try
    public void gradleTest(){
        System.out.println("Gradle/JUnit works");
    }
 
    /**
     * Clones the git repository specified by repoCloneURL into the directory specified by dirPath.
     * @param repoCloneURL The URL of the git repository to clone.
     * @param branch The specific branch of the git repository to be clone.
     * @param dirPath The path to where the repository should be cloned.
     * @return The exit value of the "git clone repoName dirPath" command.
     * @throws IOException
     * @throws InterruptedException
     * 
     */
    private int cloneRepo() throws IOException, InterruptedException{
        String[] cmdarr = {"git", "clone", "-b", branch, repoCloneURL, dirPath};
        Process p = Runtime.getRuntime().exec(cmdarr);

        p.waitFor();
        int exitValue = p.exitValue();
        p.destroy();

        return exitValue;
    }

    private void build() {

    }

    /**
     * Set the commmit status for the current repository and SHA specified by the 
     * <code>repOwner</code>, <code>repName</code>, and <code>sha</code> fields respectively.
     * 
     * @param status the status of the commit message
     * @param description a more helpful description of the status
     * @throws IOException if the request response is not <code>201</code>.
     * @throws Error if all neccessary fields are not set. 
     */
    private void postStatus(CommitStatus status, String description) throws IOException, Error {
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
            throw new IOException(con.getResponseMessage());
        }
        con.disconnect();
    }

    /**
     * Read the XML document containing the results of tests.
     * @return Number of failures in the tests.
     * @throws DocumentException
     * 
     */
    private static int analyzeBuild() throws DocumentException{       
        Document doc = parseXML();
        Node node = doc.selectSingleNode("/testsuite");
        int failures = Integer.parseInt(node.valueOf("@failures"));
        return failures;
    }
    /**
     * Helper function to get J4DOM document object from XML document.
     * @return J4DOM Document
     */
    private static Document parseXML() throws DocumentException { 
        SAXReader reader = new SAXReader();
        Document document = reader.read("D:\\DD2480-Fundamentals\\DD2480-Group31-CI\\build\\test-results\\test\\TEST-com.ci.ContinuousIntegrationServerTest.xml");
        return document;
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
     * Helper-method to specifically delete the 'target'
     * directory where we build/test the system under test.
     */
    private static void cleanTargetDir(){
        Path targetDir = FileSystems.getDefault().getPath("./target");
        cleanup(targetDir.toFile());
    }

    // used to start the CI server in command line
    public static void main(String[] args) throws Exception
    {    
        Server server = new Server(PORT);
        server.setHandler(new ContinuousIntegrationServer()); 
        server.start();
        server.join();

        // Call to cleanup the target directory
        //cleanTargetDir();
    }
}
