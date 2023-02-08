package com.ci;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;
import java.io.ObjectInputFilter.Status;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.List;
import java.io.File;

import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.transport.FetchResult;

public class GitUtilsTest {

    GitUtils DefGitUtils = new GitUtils();

    static Git testRepo;

    @BeforeClass
    public static void initTarget() throws Exception {
        //Properly set up the arguments to clone the repo and checkout the `assessment` branch
        String testCloneURLSelf = "https://github.com/DD2480-Group31/Continuous-Integration.git";
        String testBranchSelf = "assessment";
        String testDefaultBranch = "master";

        //Update the local repo
        testRepo = GitUtils.updateTarget(testCloneURLSelf, testBranchSelf, testDefaultBranch);
    }

    @AfterClass
    public static void cleanTarget() {
        testRepo.close();
        cleanTarget();
    }

    @Test
    /**
     * Requirements: See `updateTarget` documentation
     * Contract:
     *      Precondition: There is a valid call to `updateTarget` with this 
     *                    repository that checksout the `assessment` branch
     *                    for differences with the remote `origin/assessment` branch.
     *      Postcondition: The `git diff` redirected output contains exactly
     *                     0 characters since `updateTarget` should have successfully
     *                     updated the local repository.
     */
    public void testUpdateTargetSelf() throws Exception{
        //Retrieve the difference between the local `assessment` branch and the 
        //remote `origin/assessment` branch.
        ProcessBuilder gitDiff = new ProcessBuilder("git", "diff", "origin/assessment");
        gitDiff.directory(new File("./target"));
        gitDiff.redirectOutput(new File("./target/diff.txt"));
        Process process = gitDiff.start();
        process.waitFor();
        
        //The redirected output from the difference between the branches has to be 0 when updated.
        File diff = new File("./target/diff.txt");
        assertTrue(diff.length() == 0);
    }
    
    @Test
    /**
     * Requirements: See `updateTarget` documentation
     * Contract:
     *      Precondition: There is a valid call to `updateTarget` with this 
     *                    repository that checksout the `assessment` branch
     *                    for differences with the remote `origin/dummy-1` branch.
     *      Postcondition: The `git diff` redirected output contains more than
     *                     0 characters since the branches contain different code.
     */
    public void testUpdateTargetBranchDiff() throws Exception{
        //Retrieve the difference between the local `assessment` branch and the 
        //remote `origin/origin/dummy-1` branch.
        ProcessBuilder gitDiff = new ProcessBuilder("git", "diff", "origin/dummy-1");
        gitDiff.directory(new File("./target"));
        gitDiff.redirectOutput(new File("./target/diff.txt"));
        Process process = gitDiff.start();
        process.waitFor();
        
        //The redirected output from the difference between the branches has to be 
        //greater than 0 since they contain different code.
        File diff = new File("./target/diff.txt");
        assertTrue(diff.length() > 0);
    }


    @Test
    /**
     * Requirements: See `updateTarget` documentation
     * Contract:
     *      Precondition: There is an invalid branch name passed to the `updateTarget`
     *                    method with a valid URL and default branch name.
     *      Postcondition: `updateTarget` throws `java.lang.Exception.class`
     */
    public void testUpdateTargetInvalidBranch() throws Exception{
        //Setup arguments for updateTarget with an invalid checkout branch.
        String testCloneURLSelf = "https://github.com/DD2480-Group31/Continuous-Integration.git";
        String testBranchSelf = "IDoNotExist"; //Branch does not exist
        String testDefaultBranch = "master";

        //Check that the invalid call throws `java.lang.Exception.class`
        assertThrows(java.lang.Exception.class, () -> GitUtils.updateTarget(testCloneURLSelf, testBranchSelf, testDefaultBranch));
    }



    @Test
    /**
     * Requirements: See `updateTarget` documentation
     * Contract:
     *      Precondition: There is an invalid `default` branch name passed to
     *                    `updateTarget` with a valid URL and branch.
     *      Postcondition: `updateTarget` throws `java.lang.Exception.class`
     */
    public void testUpdateTargetInvalidMain() throws Exception{
        //Setup arguments for updateTarget with an invalid `default` branch.
        String testCloneURLSelf = "https://github.com/DD2480-Group31/Continuous-Integration.git";
        String testBranchSelf = "assessment";
        String testDefaultBranch = "main"; //branch does not exist, master is the default branch.

        //Check that the invalid call throws `java.lang.Exception.class`
        assertThrows(java.lang.Exception.class, () -> GitUtils.updateTarget(testCloneURLSelf, testBranchSelf, testDefaultBranch));
    }



    @Test
    /**
     * Requirements: See `updateTarget` documentation
     * Contract:
     *      Precondition: There is an invalid URL passed to the `updateTarget` method
     *                    with a valid branch and default branch for cloning this repository.
     *      Postcondition: `updateTarget` throws `java.lang.Exception`
     */
    public void testUpdateTargetInvalidURL() throws Exception{
        
        //Setup arguments for updateTarget with an invalid URL.
        String testCloneURLSelf = "invalidURL"; //Invalid
        String testBranchSelf = "assessment";
        String testDefaultBranch = "master";

        //Invalid URL passed to updateTarget --> throw `java.lang.Exception.class`
        assertThrows(java.lang.Exception.class, () -> GitUtils.updateTarget(testCloneURLSelf, testBranchSelf, testDefaultBranch));

    }



}
