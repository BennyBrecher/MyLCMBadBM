package edu.touro.mco152.bm.commands;

import edu.touro.mco152.bm.*;
import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.persist.EM;
import edu.touro.mco152.bm.DiskMark;
import jakarta.persistence.EntityManager;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.touro.mco152.bm.DiskMark.MarkType.READ;
/**
 * Contains the logic to perform a read‐only disk benchmark to become a command object
 */
public class ReadBenchmarkCommandReceiver {
    public int doRead(BenchmarkSettings settings, GeneralUI ui, DiskWorker worker){
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

        DiskMark rMark;

        int startFileNum = settings.getNextMarkNumber();


        DiskRun run = new DiskRun(DiskRun.IOMode.READ, settings.getBlockSequence());
        run.setNumMarks(settings.getNumOfMarks());
        run.setNumBlocks(settings.getNumOfBlocks());
        run.setBlockSize(settings.getBlockSizeKb());
        run.setTxSize(settings.getTargetTxSizeKb());
        run.setDiskInfo(Util.getDiskInfo(settings.getDataDir()));

        settings.message("disk info: (" + run.getDiskInfo() + ")");

        ui.updateTitle(run.getDiskInfo());

        File testFile = null;
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
            } catch (IOException ex) {
                Logger.getLogger(DiskWorker.class.getName()).log(Level.SEVERE, null, ex);
                String emsg = "May not have done Write Benchmarks, so no data available to read." +
                        ex.getMessage();
                ui.showErrorMessageDialog(emsg, "Unable to READ");
                settings.message(emsg);
                return startFileNum;
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
