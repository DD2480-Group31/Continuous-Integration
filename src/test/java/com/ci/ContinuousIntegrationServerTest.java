package com.ci;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.io.File;
import com.ci.ContinuousIntegrationServer.BuildStatus;

public class ContinuousIntegrationServerTest {

        ContinuousIntegrationServer DEFAULT = new ContinuousIntegrationServer("", "master");
        //Example: Typical test syntax
        @Test
        public void verifyNoExceptionsThrown(){
            DEFAULT.gradleTest();
            assertTrue(false);
        }

        @Test
        /**
         * Requirements: See `cleanTarget` documentation
         * Contract:
         *      Precondition: There does not exists a `target` directory.
         *      Postcondition: `cleanTarget` throws `NotDirectoryException`
         */
        public void testNoTargetDirCleanup() throws NotDirectoryException{
            //Retrieve the path of where target "should" exist
            Path targetPath = Path.of("./target");

            //Check that no target directory exists
            assertFalse(Files.isDirectory(targetPath));

            //Check that `cleanTarget` throws `NotDirectoryException` when 
            //no `target` directory exists.
            assertThrows(NotDirectoryException.class, () -> DEFAULT.cleanTarget());

            //Again check that no target directory exists
            assertFalse(Files.isDirectory(targetPath));       
        }

        @Test
        /**
         * Requirements: See `cleanBuild` documentation
         * Contract:
         *      Precondition: There exists a `target` directory but no `target/build` 
         *                    directory.
         *      Postcondition: The `cleanBuild` method throws `NotDirectoryException`
         */
        public void testNoBuildDirCleanup() throws NotDirectoryException{
            //Creates a `target` directory
            new File("./target").mkdirs();

            //Retrieves the path to `target`
            Path targetPath = Path.of("./target");

            //Check that `target` is a directory
            assertTrue(Files.isDirectory(targetPath));

            //Check that the `cleanBuild` method throws the `NotDirectoryException`
            //when it doesn't exist.
            assertThrows(NotDirectoryException.class, () -> DEFAULT.cleanBuild());

            //Check that `target` is still intact
            assertTrue(Files.isDirectory(targetPath));

            //Remove the `target` folder
            DEFAULT.cleanTarget();

            //Check that `target` was actually removed.
            assertFalse(Files.isDirectory(targetPath));
        }


        @Test
        /**
         * Requirements: See `cleanBuild` documentation
         * Contract:
         *      Precondition: There exists a `target` and `target/build` directory.
         *      Postcondition: The `build` directory is removed by `cleanBuild` and
         *                     `target` is removed by `cleanTarget`.
         */
        public void testBuildDirCleanup() throws NotDirectoryException{
            //Create the directory
            new File("./target/build").mkdirs();

            //Retrieve the path to the directories
            Path buildPath = Path.of("./target/build");
            Path targetPath = Path.of("./target");

            //Check that `build` and `target` is a directory
            assertTrue(Files.isDirectory(buildPath));
            assertTrue(Files.isDirectory(targetPath));

            //Remove the `build` directory
            DEFAULT.cleanBuild();

            //Check that `build` is no longer a valid directory
            //but `target` is.
            assertFalse(Files.isDirectory(buildPath));
            assertTrue(Files.isDirectory(targetPath));

            //Removal of the entire `target` directory after tests
            DEFAULT.cleanTarget();

            //Double check `target` deletion
            assertFalse(Files.isDirectory(targetPath));
        }


        @Test
        /**
         * Requirements: See `cleanTarget` documentation
         * Contract:
         *      Precondition: There exists a `target` directory with a text file and
         *                    a subdirectory `target/testSubDir` also containing a text file.
         *      Postcondition: `cleanTarget` removes the entire `target` directory with all its contents.
         */
        public void testTargetDirSubCleanup() throws IOException, NotDirectoryException {
            //Creates the directories and files
            new File("./target").mkdirs();
            new File("./target/testSubDir").mkdirs();
            new File("./target/test.txt").createNewFile();
            new File("./target/testSubDir/test2.txt").createNewFile();

            //Retrieves the paths of the created directories and files
            Path targetPath = Path.of("./target");
            Path targetSubPath = Path.of("./target/testSubDir");
            Path targetTxt = Path.of("./target/test.txt");
            Path targetSubTxt = Path.of("./target/testSubDir/test2.txt");

            //Checks that the directories work and that the files are writable
            assertTrue(Files.isDirectory(targetPath));
            assertTrue(Files.isDirectory(targetSubPath));
            assertTrue(Files.isWritable(targetTxt));
            assertTrue(Files.isWritable(targetSubTxt));

            //Cleanup the `target` folder
            DEFAULT.cleanTarget();

            //Check that the directories and files are deleted
            assertFalse(Files.isDirectory(targetPath));
            assertFalse(Files.isDirectory(targetSubPath));
            assertFalse(Files.isWritable(targetTxt));
            assertFalse(Files.isWritable(targetSubTxt));
        }


        @Test
        /**
         * Requirements: See `cleanBuild` documentation
         * Contract:
         *      Precondition: There exists a `target/build` directory with a text file and
         *                    a subdirectory `target/build/testSubDir` also containing a text file.
         *      Postcondition: `cleanBuild` removes the entire `build` directory with all its contents.
         */
        public void testBuildDirSubCleanup() throws IOException, NotDirectoryException{
            //Creates the directories and files
            new File("./target/build").mkdirs();
            new File("./target/build/testSubDir").mkdirs();
            new File("./target/build/test.txt").createNewFile();
            new File("./target/build/testSubDir/test2.txt").createNewFile();

            //Retrieves the paths of the created directories and files
            Path targetPath = Path.of("./target");
            Path buildPath = Path.of("./target/build");
            Path buildSubPath = Path.of("./target/build/testSubDir");
            Path buildTxt = Path.of("./target/build/test.txt");
            Path buildSubTxt = Path.of("./target/build/testSubDir/test2.txt");

            //Checks that the directories work and that the files are writable
            assertTrue(Files.isDirectory(targetPath));
            assertTrue(Files.isDirectory(buildPath));
            assertTrue(Files.isDirectory(buildSubPath));
            assertTrue(Files.isWritable(buildTxt));
            assertTrue(Files.isWritable(buildSubTxt));
            
            //Cleanup the `Build` folder, but not the `target`
            DEFAULT.cleanBuild();

            //Check that the directories and files are deleted but
            //the target directory remains.
            assertTrue(Files.isDirectory(targetPath));
            assertFalse(Files.isDirectory(buildPath));
            assertFalse(Files.isDirectory(buildSubPath));
            assertFalse(Files.isWritable(buildTxt));
            assertFalse(Files.isWritable(buildSubTxt));

            //Finally clean the target directory
            DEFAULT.cleanTarget();
            assertFalse(Files.isDirectory(targetPath));
        }

        @Test
        /*
         * Requirements: analyzeResults() must return </code>BuildStatus.success</code> if the "failures" attribute of the 
         * "testsuite" element is 0 in the test result XML.
         * Contract:
         *      Precondition: The "testdummies/pass" directory contains a XML testfile where failures = 0.
         *      Postcondition: analyzeResults() returns </code>BuildStatus.success</code>.
         */
        public void testAnalyzeResultsNoTestFails(){
            String testPath = "src/test/testdummies/pass";

            BuildStatus bs = DEFAULT.analyzeResults(testPath);
            assertEquals(BuildStatus.success, bs);
        }
        @Test
        /*
         * Requirements: analyzeResults() must return </code>BuildStatus.testFail</code> if the "failures" attribute 
         * of the "testsuite" element is > 0 in the test result XML.
         * Contract:
         *      Precondition: The "testdummies/fail" directory contains a XML testfile where failures > 0.
         *      Postcondition: analyzeResults() returns </code>BuildStatus.testFail</code>.
         */
        public void testAnalyzeResultsWithTestFails(){
            String testPath = "src/test/testdummies/fail";

            BuildStatus bs = DEFAULT.analyzeResults(testPath);
            assertEquals(BuildStatus.testFail, bs);
        }

        @Test
        /*
         * Requirements: analyzeResults() must return </code>BuildStatus.buildFail</code> if the
         * directory which should contain teh XML file is empty.
         * Contract:
         *      Precondition: The "testdummies/empty" directory is empty.
         *      Postcondition: analyzeResults() returns </code>BuildStatus.buildFail</code>.
         */
        public void testAnalyzeResultsNoXMLFile(){
            String testPath = "src/test/testdummies/empty";

            BuildStatus bs = DEFAULT.analyzeResults(testPath);
            assertEquals(BuildStatus.buildFail, bs);
        }








}
