package com.ci;
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

        String testCloneURLSelf = "https://github.com/DD2480-Group31/Continuous-Integration.git";
        String testBranchSelf = "assessment";
        String testDefaultBranch = "master";
        Git testRepo = GitUtils.updateTarget(testCloneURLSelf, testBranchSelf, testDefaultBranch);

        //org.eclipse.jgit.api.Status statusBefore = testRepo.status().call();

        ProcessBuilder gitDiff = new ProcessBuilder("git", "diff", "origin/assessment");
        gitDiff.directory(new File("./target"));
        gitDiff.redirectOutput(new File("./target/diff.txt"));
        Process process = gitDiff.start();
        process.waitFor();
        

        File diff = new File("./target/diff.txt");
        assertTrue(diff.length() == 0);

        testRepo.close();
        //Clean target
        ContinuousIntegrationServer.cleanTarget();

    }
    
    @Test
    /**
     * Requirements: See `updateTarget` documentation
     * Contract:
     *      Precondition: There is a valid call to `updateTarget` with this 
     *                    repository that checksout the `assessment` branch
     *                    for differences with the remote `issue-9` branch.
     *      Postcondition: The `git diff` redirected output contains more than
     *                     0 characters since the branches contain different code.
     */
    public void testUpdateTargetBranchDiff() throws Exception{

        String testCloneURLSelf = "https://github.com/DD2480-Group31/Continuous-Integration.git";
        String testBranchSelf = "assessment";
        String testDefaultBranch = "master";
        Git testRepo = GitUtils.updateTarget(testCloneURLSelf, testBranchSelf, testDefaultBranch);

        //org.eclipse.jgit.api.Status statusBefore = testRepo.status().call();

        ProcessBuilder gitDiff = new ProcessBuilder("git", "diff", "origin/issue-9");
        gitDiff.directory(new File("./target"));
        gitDiff.redirectOutput(new File("./target/diff.txt"));
        Process process = gitDiff.start();
        process.waitFor();
        

        File diff = new File("./target/diff.txt");
        assertTrue(diff.length() > 0);

        testRepo.close();
        //Clean target
        ContinuousIntegrationServer.cleanTarget();

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

        String testCloneURLSelf = "https://github.com/DD2480-Group31/Continuous-Integration.git";
        String testBranchSelf = "IDoNotExist";
        String testDefaultBranch = "master";

        assertThrows(java.lang.Exception.class, () -> GitUtils.updateTarget(testCloneURLSelf, testBranchSelf, testDefaultBranch));

        //Clean target
        ContinuousIntegrationServer.cleanTarget();

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

        String testCloneURLSelf = "https://github.com/DD2480-Group31/Continuous-Integration.git";
        String testBranchSelf = "assessment";
        String testDefaultBranch = "main"; //branch does not exist

        assertThrows(java.lang.Exception.class, () -> GitUtils.updateTarget(testCloneURLSelf, testBranchSelf, testDefaultBranch));

        //Clean target
        ContinuousIntegrationServer.cleanTarget();

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
        String testCloneURLSelf = "invalidURL";
        String testBranchSelf = "assessment";
        String testDefaultBranch = "master";

        //Invalid URL passed to updateTarget --> throw `java.lang.Exception.class`
        assertThrows(java.lang.Exception.class, () -> GitUtils.updateTarget(testCloneURLSelf, testBranchSelf, testDefaultBranch));

        //Double check that target gets deleted
        try{
            ContinuousIntegrationServer.cleanTarget();
        }catch(NotDirectoryException e){
            //If the directory didn't exist --> Continue
        }
    }



}
