package com.ci;

import org.junit.Test;
import static org.junit.Assert.*;

import javax.servlet.http.HttpServletResponse;

import com.ci.ContinuousIntegrationServer.CommitStatus;

/**
 * Tests written for the `postStatus method`
 */
public class PostStatusTests {

    @Test
    /**
     * If <code>postStatues</code> cannot make a request, it should throw an <code>Error</code>.
     * Contract:
     *      Preconditions:  The fields required to make a status post request are not set.
     *      Postcondition:  The method throws an <code>Error</code>
     */
    public void errorWithNullFields() {
        var ci = new ContinuousIntegrationServer("", "master");
        assertThrows(
            Error.class, 
            () -> ci.postStatus(CommitStatus.success, "Hello World!")
        );
        ci.repOwner = "DD2480-Group31";
        assertThrows(
            Error.class, 
            () -> ci.postStatus(CommitStatus.success, "Hello World!")
        );
        ci.repName = "Continuous-Integration";
        assertThrows(
            Error.class, 
            () -> ci.postStatus(CommitStatus.success, "Hello World!")
        );
    }

    @Test
    /**
     * Test that Github recieves the request. 
     * Since we don't have an access token here, the response code should be 401.
     * 
     * Contract:
     *      Precondition:   The required fields are set but the server lacks an access token.
     *      Postcondition:  The method gets a response with error code 401 (unauthorized).
     */
    public void getsResponse() {
        var ci = new ContinuousIntegrationServer("", "master");
        ci.repOwner = "DD2480-Group31";
        ci.repName = "Continuous-Integration";
        ci.sha = "1b69dbad8b8818b80d0a4e70127d5ef27ed7198d"; // This is valid commit
        try {
            int code = ci.postStatus(CommitStatus.success, "This is a test");
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, code);
        } catch (Exception e) {
            assertTrue("postStatus should not throw", false);
        }   
    }
}
