package edu.touro.mco152.bm;

import edu.touro.mco152.bm.persist.DiskRun;
import java.io.File;

public class AppBenchmarkSettings implements BenchmarkSettings {
    @Override public boolean isReadTest() { return App.readTest; }
    @Override public boolean isWriteTest() { return App.writeTest; }
    @Override public boolean isAutoReset() { return App.autoReset; }
    @Override public boolean isAutoRemoveData() { return App.autoRemoveData; }
    @Override public boolean isMultiFile() { return App.multiFile; }
    @Override public boolean isWriteSyncEnabled() { return App.writeSyncEnable; }

    @Override public int getNumOfMarks() { return App.numOfMarks; }
    @Override public int getNumOfBlocks() { return App.numOfBlocks; }
    @Override public int getBlockSizeKb() { return App.blockSizeKb; }
    @Override public long getTargetTxSizeKb() { return App.targetTxSizeKb(); }
    @Override public int getNextMarkNumber() { return App.nextMarkNumber; }
    @Override public int getKilobyte() {
        return App.KILOBYTE;
    }
    @Override public int getMegabyte() {
        return App.MEGABYTE;
    }

    @Override public File getDataDir() { return App.dataDir; }
    @Override public DiskRun.BlockSequence getBlockSequence() { return App.blockSequence; }

    @Override public void resetTestData() { App.resetTestData(); }
    @Override public void updateMetrics(DiskMark mark) { App.updateMetrics(mark); }
    @Override public void incrementNextMarkNumber() { App.nextMarkNumber += App.numOfMarks; }
    @Override public void setIdleState() { App.state = App.State.IDLE_STATE; }
    @Override public void message(String message) { App.msg(message);}
    @Override
    public void setNextMarkNumber(int newNextMarkNumber) {
        App.nextMarkNumber = newNextMarkNumber;
    }
}