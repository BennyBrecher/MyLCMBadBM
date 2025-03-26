package edu.touro.mco152.bm;

import edu.touro.mco152.bm.persist.DiskRun;

public interface GeneralUI {
    void updateLegend();
    void resetTestData();
    void addWriteMark(DiskMark mark);
    void addReadMark(DiskMark mark);
    void updateTitle(String text);
    void adjustSensitivity();
    void showPlainMessageDialog(String message, String title);
    void showErrorMessageDialog(String message, String title);
    void addRun(DiskRun run);
}
