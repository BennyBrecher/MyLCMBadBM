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
