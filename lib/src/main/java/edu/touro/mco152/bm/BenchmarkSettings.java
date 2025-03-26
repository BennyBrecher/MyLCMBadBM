package edu.touro.mco152.bm;

import edu.touro.mco152.bm.DiskMark;
import edu.touro.mco152.bm.persist.DiskRun;
import java.io.File;
/**
 * Removes the hard coded dependency of App from DiskWorker
 * */
public interface BenchmarkSettings {
    boolean isReadTest();
    boolean isWriteTest();
    boolean isAutoReset();
    boolean isAutoRemoveData();
    boolean isMultiFile();
    boolean isWriteSyncEnabled();

    int getNumOfMarks();
    int getNumOfBlocks();
    int getBlockSizeKb();
    long getTargetTxSizeKb();
    int getNextMarkNumber();
    int getKilobyte();
    int getMegabyte();

    File getDataDir();
    DiskRun.BlockSequence getBlockSequence();

    void resetTestData();
    void updateMetrics(DiskMark mark);
    void incrementNextMarkNumber();
    void setIdleState();
    void message(String message);
}