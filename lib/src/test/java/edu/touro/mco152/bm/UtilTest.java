package edu.touro.mco152.bm;

import edu.touro.mco152.bm.Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 Challenges:
 1) In our zoom meeting discussing the refactoring homework you mentioned i submitted my code even though it didn't run
 so i tracked down the error and realized it was due to my mistake of forgetting to inject my new dependencies i made into the DiskWorker construction on line 266 in the App class
 i then changed the instantiation to "worker = new DiskWorker(new AppBenchmarkSettings(), new SwingUI(), new GeneralUsageBenchmarker());
 instead  of the no args construction like it previously had which reflected my DIP work done last time, and then it ran smooth as usual


 2) when I had to enable JUnit I was following the article yu gave and it told me to use:
 testImplementation 'org.junit.jupiter:junit-jupiter-api:5.8.1'
 testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.8.1'

 But my dependencies in build.gradle already included:
 testImplementation libs.junit.jupiter
 testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

 I decided to keep what was already in my build.gradle since i was unsure about how up to date the article mightve been
 (i did attempt running tests with both after a build sync and each let tests run and pass properly so it didn't make a difference but im still unsure what the real difference in the dependencies are)

 3) I had a bit of trouble making my tests be specific to one of the Right BICEP's exclusively since my normal way i do testing kinda overlaps each so the lines were a bit blurred for me in the beginning but i think my tests came out neat enough in the end

 4) I wasn't as familiar with CORRECT as i was for BICEP and i misunderstood it to only be applicable for Boundary tests but after a quick search it seemed that CORRECT is used for any test i do so i added in each tests doc which CORRECT each related to after

 ./gradlew clean jar test --info
 TODO: Break shit
 **/


public class UtilTest {

    /**
     * Right Check: Makes temp dir that has file and sub-dir with file (which is regular use case)
     * checks if normal call to deleteDirectory() does what it's supposed to
     *
     * BICEP: C for Cross Check - after the Right check we verify using the exists() method to prove it worked
     *
     * CORRECT: C for Conformance as we are testing a regular case use
     * */
    @Test
    public void testDeleteDirectory() throws IOException {
        Path tempDir = Files.createTempDirectory("testDir");
        File dir = tempDir.toFile();
        //this could be a good time to call assertFalse since dir is made but empty
        File file1 = new File(dir, "file1.txt");
        file1.createNewFile();

        File subDir = new File(dir, "subdir");
        subDir.mkdir();

        File file2 = new File(subDir, "file2.txt");
        file2.createNewFile();

        assertTrue(dir.exists());
        assertTrue(Util.deleteDirectory(dir)); //heres the Right check
        assertFalse(dir.exists());  //heres the Cross check
    }

    /**
     * BICEP: B for Boundary — deleting an empty directory
     * (can also be said that it's doing a Right check and a Cross check in the asserts similar to above)
     *
     * CORRECT: E for Existence check
     */
    @Test
    public void testDeleteEmptyDirectory() throws IOException {
        Path tempDir = Files.createTempDirectory("emptyDir");
        File dir = tempDir.toFile();

        assertTrue(dir.exists());
        assertTrue(Util.deleteDirectory(dir));
        assertFalse(dir.exists());
    }

    /**
     * BICEP: E for Error Handling — deleting a directory that does not exist
     * CORRECT: E for Existence check
     */
    @Test
    public void testDeleteNonExistentDirectory() {
        File fakeDir = new File("some/nonexistent/path");
        assertFalse(Util.deleteDirectory(fakeDir));
    }


    /**
     * Parameterized Test
     * BICEP: B for Boundary - each edge case double requires rounding so we test that it rounds as expected
     * CORRECT: C for Conformance
     * */
    @ParameterizedTest
    @CsvSource({
            "123.456, 123.46",
            "0.004, 0",
            "9999.999, 10000",
            "-45.678, -45.68",
            "7.0, 7"
    })
    void testDisplayString(double input, String expected) {
        String result = Util.displayString(input);
        System.out.println("Input: " + input + "    Expected: " + expected + "    Output: " + result);
        assertEquals(expected, result);
    }

    /**
     * Parameterized Test
     * Bicep B for Boundaries: Tests how displayString() handles the extreme edge cases of infinity values
     * CORRECT: R for Range
     */
    @ParameterizedTest
    @ValueSource(doubles = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void testDisplayStringBoundaries(double input) {
        String result = Util.displayString(input);
        System.out.println("Input: " + input + "    Output: " + result);
        assertTrue(result.contains("∞") || result.contains("-∞"));
    }
}
