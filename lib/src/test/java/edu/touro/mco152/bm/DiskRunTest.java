package edu.touro.mco152.bm;

import static org.junit.jupiter.api.Assertions.*;

import edu.touro.mco152.bm.persist.DiskRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Date;

public class DiskRunTest {

    //startTime = when the benchmark starts (set in constructor)
	//endTime = when the benchmark ends (set manually at the end of a run)

    /**
     * BICEP: Right
     * If startTime and endTime are 3 seconds apart, getDuration() should return String of "3s"
     *
     * CORRECT: C for Conformance also T for Time
     * verifies that the math and formatting are correct
     */
    @Test
    void testGetDuration() throws InterruptedException {
        DiskRun dr = new DiskRun(); //ctor sets start
        Thread.sleep(3000);
        dr.setEndTime(new Date());
        assertEquals("3s", dr.getDuration());
    }

    /**
     * BICEP: B for Boundary
     * tests edge case where endTime is set to the same time as startTime, the duration should be "0s"
     *
     * CORRECT: T for Time
     */
    @Test
    void testGetDurationBoundary(){
        DiskRun dr = new DiskRun();
        dr.setEndTime(new Date()); //set endTime right away
        assertEquals("0s", dr.getDuration());
    }


    /**
     * BICEP: E for Error
     * If endTime is never explicitly set then getDuration() should return "unknown"
     *
     * CORRECT: E for Existence since endTime() never being set means it remains null
     */
    @Test
    void testGetDurationError(){
        DiskRun dr = new DiskRun();
        assertEquals("unknown", dr.getDuration());
    }

    /**
     * BICEP: P for Performance
     * ensures getDuration() executes under 100ms
     *
     * CORRECT: T for Time
     */
    @Test
    void testGetDurationPerformance() {
        DiskRun dr = new DiskRun();
        dr.setEndTime(new Date());

        long start = System.currentTimeMillis();
        dr.getDuration();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 100);
    }


    /**
     * Parameterized Test
     * BICEP: B for Boundary - each edge case double requires rounding so we test that it rounds as expected
     * also a Right Test
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
    void testGetMin(double input, String expected){
        DiskRun dr = new DiskRun();
        dr.setMin(input);
        assertEquals(expected, dr.getMin());
    }

    /**
     * BICEP: E for Error
     * CORRECT: R for Range or E for Existence
     * */
    @Test
    void testGetMinSentinelCase(){
        DiskRun dr = new DiskRun();
        dr.setMin(-1);
        assertEquals("- -", dr.getMin());
    }

}
