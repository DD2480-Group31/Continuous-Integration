package com.ci;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;

/**
 * Various utility functions for Git stuff.
 */
public class GitUtils {
    /**
     * Clones a git specified repository by into the directory specified by <code>DIR_PATH</code>.
     * 
     * @param url The URL of the git repository to clone.
     * @return the created <code>Git</code> object.
     * @throws Exception if an error occours when cloning the repository.
     */
    public static Git cloneRepo(String url) throws Exception {
        try {
            Git repository = Git.cloneRepository()
                .setURI(url)
                .setDirectory(new File(ContinuousIntegrationServer.DIR_PATH))
                .setCloneAllBranches(true)
                .call();
            return repository;
        } catch (Exception e) {
            // TODO: Better error handling?
            throw new Exception("Error encountered in `cloneRepo`");
        }
    }

    /**
     * Pull the repository declared in the <code>repository</code> field and 
     * checkout to the branch specified by the <code>branch</code> field.
     * 
     * @param repository the repository to update and checkout.
     * @param branch the name of the branch to checkout. 
     * @throws Exception if an error occours when either pulling or branching.
     */
    public static void pullAndBranch(Git repository, String branch) throws Exception {
        try {
            repository.pull().call();
            repository.checkout().addPath("origin/" + branch).call();
        } catch (Exception e) {
            // TODO: Better error handling?
            e.printStackTrace();
            throw new Exception("Error encountered in `pullAndBranch`");
        }
        
    }

    /**
     * Update the target repository either by pulling the main branch or cloning the repository.
     * Then checkout to the specified branch.
     * 
     * @param url the clone URL of the repository.
     * @param branch the name of the branch to chekcout.
     * @throws Exception 
     */
    public static Git updateTarget(String url, String branch) throws Exception {
        File gitDir = new File(ContinuousIntegrationServer.DIR_PATH + "/.git");
        Git repository;
        try {
            repository = Git.open(gitDir);     
        } catch (IOException e) {
            repository = cloneRepo(url);
        }
        pullAndBranch(repository, branch);
        return repository;
    }
}
