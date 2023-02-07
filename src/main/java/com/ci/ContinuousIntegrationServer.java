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
import java.nio.file.Path;
import java.io.File;
 
import org.eclipse.jetty.server.Server;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONException;
import org.json.JSONObject;

//import org.eclipse.jgit.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

/** 
 Skeleton of a ContinuousIntegrationServer which acts as webhook
 See the Jetty documentation for API documentation of those classes.
*/
public class ContinuousIntegrationServer extends AbstractHandler {  
    final static String DIR_PATH = "target";
    final private String TOKEN;

    private String repOwner;
    private String repName;
    private String sha;
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

    public ContinuousIntegrationServer(String statusToken) {
        TOKEN = statusToken;
        System.out.println(statusToken);
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

        // Set pending status
        postStatus(CommitStatus.pending, "Building repository and running tests...");

        try {
            // Download target repository
            this.cloneRepo();
            // Build the cloneld repository
            this.build();
        } catch (InterruptedException | JSONException | GitAPIException e) {
            e.printStackTrace();
            postStatus(CommitStatus.error, "CI server encountered an error");
            response.getWriter().println("Server error");
            response.setStatus(500);
            return;
        }
        
        var res = analyzeResults();

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

        response.getWriter().println("CI job done");
    }

    // TODO: Implement this method!
    private BuildStatus analyzeResults() {
        return BuildStatus.success;
    }

    //Method for JUnit to initially try
    public void gradleTest(){
        System.out.println("Gradle/JUnit works");
    }
 
    /**
     * Clones the git repository specified by repoCloneURL into the directory specified by dirPath.
     * @param repoCloneURL The URL of the git repository to clone.
     * @return The exit value of the "git clone repoName dirPath" command.
     * @throws InvalidRemoteException
     * @throws TransportException
     * @throws GitAPIException
     */
    private void cloneRepo() throws InvalidRemoteException, TransportException, JSONException, GitAPIException {
        repository = Git.cloneRepository()
            .setURI(repoCloneURL)
            .setDirectory(new File(DIR_PATH))
            .call();
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
        JSONObject conf = new JSONObject(Files.readString(Path.of("config.json")));
        int port = conf.getInt("port");
        String statusToken = conf.getString("status-token");
        Server server = new Server(port);
        server.setHandler(new ContinuousIntegrationServer(statusToken)); 
        server.start();
        server.join();

        // Call to cleanup the target directory
        //cleanTargetDir();
    }
}
