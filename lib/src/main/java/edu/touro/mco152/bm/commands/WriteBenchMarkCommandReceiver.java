package edu.touro.mco152.bm.commands;

import edu.touro.mco152.bm.*;
import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.DiskMark;
import edu.touro.mco152.bm.persist.EM;
import jakarta.persistence.EntityManager;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.touro.mco152.bm.DiskMark.MarkType.WRITE;
/**
 * Contains the logic to perform a write‐only disk benchmark action to become our command
 */
public class WriteBenchMarkCommandReceiver {
    public int doWrite(BenchmarkSettings settings, GeneralUI ui, DiskWorker worker){
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

        DiskMark wMark;

        int startFileNum = settings.getNextMarkNumber();

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
        File testFile = null;
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
        try {
            EntityManager em = EM.getEntityManager();
            em.getTransaction().begin();
            em.persist(run);
            em.getTransaction().commit();

            ui.addRun(run);
        } catch (Exception e) {

        }

        return startFileNum + settings.getNumOfMarks();
    }
}
