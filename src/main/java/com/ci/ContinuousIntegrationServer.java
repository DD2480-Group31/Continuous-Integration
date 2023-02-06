package com.ci;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;

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
 
    
    private void cloneRepo() {

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

    private void cleanup() {
        
    }

    // used to start the CI server in command line
    public static void main(String[] args) throws Exception
    {    
        Server server = new Server(PORT);
        server.setHandler(new ContinuousIntegrationServer()); 
        server.start();
        server.join();
    }
}
