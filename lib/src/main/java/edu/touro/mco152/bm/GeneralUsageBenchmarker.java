package edu.touro.mco152.bm;

import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.persist.EM;
import jakarta.persistence.EntityManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.touro.mco152.bm.DiskMark.MarkType.READ;
import static edu.touro.mco152.bm.DiskMark.MarkType.WRITE;

public class GeneralUsageBenchmarker implements Benchmarker {
    @Override
    public boolean runBenchmark(BenchmarkSettings settings, GeneralUI ui, DiskWorker worker) throws Exception{
        File testFile = null;
         /*
          We 'got here' because: 1: End-user clicked 'Start' on the benchmark UI,
          which triggered the start-benchmark event associated with the App::startBenchmark()
          method.  2: startBenchmark() then instantiated a DiskWorker, and called
          its (super class's) execute() method, causing Swing to eventually
          call this doInBackground() method.
         */
        Logger.getLogger(DiskWorker.class.getName()).log(Level.INFO, "*** New worker thread started ***");
        settings.message("Running readTest " + settings.isReadTest() + "   writeTest " + settings.isWriteTest());
        settings.message("num files: " + settings.getNumOfMarks() + ", num blks: " + settings.getNumOfBlocks()
                + ", blk size (kb): " + settings.getBlockSizeKb() + ", blockSequence: " + settings.getBlockSequence());

        /*
          init local vars that keep track of benchmarks, and a large read/write buffer
         */
        int wUnitsComplete = 0, rUnitsComplete = 0, unitsComplete;
        int wUnitsTotal = settings.isWriteTest() ? settings.getNumOfBlocks() * settings.getNumOfMarks() : 0;
        int rUnitsTotal = settings.isReadTest() ? settings.getNumOfBlocks() * settings.getNumOfMarks() : 0;
        int unitsTotal = wUnitsTotal + rUnitsTotal;
        float percentComplete;

        int blockSize = settings.getBlockSizeKb() * settings.getKilobyte();
        byte[] blockArr = new byte[blockSize];
        for (int b = 0; b < blockArr.length; b++) {
            if (b % 2 == 0) {
                blockArr[b] = (byte) 0xFF;
            }
        }

        DiskMark wMark, rMark;  // declare vars that will point to objects used to pass progress to UI

        ui.updateLegend();  // init chart legend info

        if (settings.isAutoReset()) {
            settings.resetTestData();
            ui.resetTestData();
        }

        int startFileNum = settings.getNextMarkNumber();

        /*
          The GUI allows a Write, Read, or both types of BMs to be started. They are done serially.
         */
        if (settings.isWriteTest()) {
            DiskRun run = new DiskRun(DiskRun.IOMode.WRITE, settings.getBlockSequence());
            run.setNumMarks(settings.getNumOfMarks());
            run.setNumBlocks(settings.getNumOfBlocks());
            run.setBlockSize(settings.getBlockSizeKb());
            run.setTxSize(settings.getTargetTxSizeKb());
            run.setDiskInfo(Util.getDiskInfo(settings.getDataDir()));

            // Tell logger and GUI to display what we know so far about the Run
            settings.message("disk info: (" + run.getDiskInfo() + ")");

            ui.updateTitle(run.getDiskInfo());

            // Create a test data file using the default file system and config-specified location
            if (!settings.isMultiFile()) {
                testFile = new File(settings.getDataDir().getAbsolutePath() + File.separator + "testdata.jdm");
            }

            /*
              Begin an outer loop for specified duration (number of 'marks') of benchmark,
              that keeps writing data (in its own loop - for specified # of blocks). Each 'Mark' is timed
              and is reported to the GUI for display as each Mark completes.
             */
            for (int m = startFileNum; m < startFileNum + settings.getNumOfMarks() && !worker.isCancelledFromOutside(); m++) {

                if (settings.isMultiFile()) {
                    testFile = new File(settings.getDataDir().getAbsolutePath()
                            + File.separator + "testdata" + m + ".jdm");
                }
                wMark = new DiskMark(WRITE);    // starting to keep track of a new benchmark
                wMark.setMarkNum(m);
                long startTime = System.nanoTime();
                long totalBytesWrittenInMark = 0;

                String mode = "rw";
                if (settings.isWriteSyncEnabled()) {
                    mode = "rwd";
                }

                try {
                    try (RandomAccessFile rAccFile = new RandomAccessFile(testFile, mode)) {
                        for (int b = 0; b < settings.getNumOfBlocks(); b++) {
                            if (settings.getBlockSequence() == DiskRun.BlockSequence.RANDOM) {
                                int rLoc = Util.randInt(0, settings.getNumOfBlocks() - 1);
                                rAccFile.seek((long) rLoc * blockSize);
                            } else {
                                rAccFile.seek((long) b * blockSize);
                            }
                            rAccFile.write(blockArr, 0, blockSize);
                            totalBytesWrittenInMark += blockSize;
                            wUnitsComplete++;
                            unitsComplete = rUnitsComplete + wUnitsComplete;
                            percentComplete = (float) unitsComplete / (float) unitsTotal * 100f;

                            /*
                              Report to GUI what percentage level of Entire BM (#Marks * #Blocks) is done.
                             */
                            worker.setProgressFromOutside((int) percentComplete);
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(DiskWorker.class.getName()).log(Level.SEVERE, null, ex);
                }

                /*
                  Compute duration, throughput of this Mark's step of BM
                 */
                long endTime = System.nanoTime();
                long elapsedTimeNs = endTime - startTime;
                double sec = (double) elapsedTimeNs / (double) 1000000000;
                double mbWritten = (double) totalBytesWrittenInMark / (double) settings.getMegabyte();
                wMark.setBwMbSec(mbWritten / sec);
                settings.message("m:" + m + " write IO is " + wMark.getBwMbSecAsString() + " MB/s     "
                        + "(" + Util.displayString(mbWritten) + "MB written in "
                        + Util.displayString(sec) + " sec)");
                settings.updateMetrics(wMark);

                /*
                  Let the GUI know the interim result described by the current Mark
                 */
                worker.publishFromOutside(wMark);

                // Keep track of statistics to be displayed and persisted after all Marks are done.
                run.setRunMax(wMark.getCumMax());
                run.setRunMin(wMark.getCumMin());
                run.setRunAvg(wMark.getCumAvg());
                run.setEndTime(new Date());
            } // END outer loop for specified duration (number of 'marks') for WRITE benchmark

            /*
              Persist info about the Write BM Run (e.g. into Derby Database) and add it to a GUI panel
             */
            EntityManager em = EM.getEntityManager();
            em.getTransaction().begin();
            em.persist(run);
            em.getTransaction().commit();

            ui.addRun(run);
        }

        /*
          Most benchmarking systems will try to do some cleanup in between 2 benchmark operations to
          make it more 'fair'. For example a networking benchmark might close and re-open sockets,
          a memory benchmark might clear or invalidate the Op Systems TLB or other caches, etc.
         */

        // try renaming all files to clear catch
        if (settings.isReadTest() && settings.isWriteTest() && !worker.isCancelledFromOutside()) {
            ui.showPlainMessageDialog(
                    """
                            For valid READ measurements please clear the disk cache by
                            using the included RAMMap.exe or flushmem.exe utilities.
                            Removable drives can be disconnected and reconnected.
                            For system drives use the WRITE and READ operations\s
                            independantly by doing a cold reboot after the WRITE""",
                    "Clear Disk Cache Now");
        }

        // Same as above, just for Read operations instead of Writes.
        if (settings.isReadTest()) {
            DiskRun run = new DiskRun(DiskRun.IOMode.READ, settings.getBlockSequence());
            run.setNumMarks(settings.getNumOfMarks());
            run.setNumBlocks(settings.getNumOfBlocks());
            run.setBlockSize(settings.getBlockSizeKb());
            run.setTxSize(settings.getTargetTxSizeKb());
            run.setDiskInfo(Util.getDiskInfo(settings.getDataDir()));

            settings.message("disk info: (" + run.getDiskInfo() + ")");

            ui.updateTitle(run.getDiskInfo());

            for (int m = startFileNum; m < startFileNum + settings.getNumOfMarks() && !worker.isCancelledFromOutside(); m++) {

                if (settings.isMultiFile()) {
                    testFile = new File(settings.getDataDir().getAbsolutePath()
                            + File.separator + "testdata" + m + ".jdm");
                }
                rMark = new DiskMark(READ);  // starting to keep track of a new benchmark
                rMark.setMarkNum(m);
                long startTime = System.nanoTime();
                long totalBytesReadInMark = 0;

                try {
                    try (RandomAccessFile rAccFile = new RandomAccessFile(testFile, "r")) {
                        for (int b = 0; b < settings.getNumOfBlocks(); b++) {
                            if (settings.getBlockSequence() == DiskRun.BlockSequence.RANDOM) {
                                int rLoc = Util.randInt(0, settings.getNumOfBlocks() - 1);
                                rAccFile.seek((long) rLoc * blockSize);
                            } else {
                                rAccFile.seek((long) b * blockSize);
                            }
                            rAccFile.readFully(blockArr, 0, blockSize);
                            totalBytesReadInMark += blockSize;
                            rUnitsComplete++;
                            unitsComplete = rUnitsComplete + wUnitsComplete;
                            percentComplete = (float) unitsComplete / (float) unitsTotal * 100f;
                            worker.setProgressFromOutside((int) percentComplete);
                        }
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(DiskWorker.class.getName()).log(Level.SEVERE, null, ex);
                    String emsg = "May not have done Write Benchmarks, so no data available to read." +
                            ex.getMessage();
                    ui.showErrorMessageDialog(emsg, "Unable to READ");
                    settings.message(emsg);
                    return false;
                }
                long endTime = System.nanoTime();
                long elapsedTimeNs = endTime - startTime;
                double sec = (double) elapsedTimeNs / (double) 1000000000;
                double mbRead = (double) totalBytesReadInMark / (double) settings.getMegabyte();
                rMark.setBwMbSec(mbRead / sec);
                settings.message("m:" + m + " READ IO is " + rMark.getBwMbSec() + " MB/s    "
                        + "(MBread " + mbRead + " in " + sec + " sec)");
                settings.updateMetrics(rMark);
                worker.publishFromOutside(rMark);

                run.setRunMax(rMark.getCumMax());
                run.setRunMin(rMark.getCumMin());
                run.setRunAvg(rMark.getCumAvg());
                run.setEndTime(new Date());
            }

            /*
              Persist info about the Read BM Run (e.g. into Derby Database) and add it to a GUI panel
             */
            EntityManager em = EM.getEntityManager();
            em.getTransaction().begin();
            em.persist(run);
            em.getTransaction().commit();

            ui.addRun(run);
        }
        settings.incrementNextMarkNumber();
        return true;
    }
}