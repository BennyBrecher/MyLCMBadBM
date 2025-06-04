package edu.touro.mco152.bm.ui;

import edu.touro.mco152.bm.DiskMark;
import edu.touro.mco152.bm.GeneralUI;
import edu.touro.mco152.bm.persist.DiskRun;

/**
 * A minimal, non‑Swing implementation of GeneralUI for command‐pattern tests.
 * • Counts how many read marks have been reported.
 * • All other methods are no‑ops.
 */
public class ConsoleUI implements GeneralUI {

    private int readMarksCount = 0;

    /**
     * Returns how many times addReadMark(...) was called.
     */
    public int readMarksCount() {
        return readMarksCount;
    }

    // ───────────────────────────────────────────────────────────────────────────────────
    // Implement exactly the methods declared in your  GeneralUI.
    // Everything is a no‑op except addReadMark increments our counter for testing.
    // ───────────────────────────────────────────────────────────────────────────────────

    @Override
    public void updateLegend() {
        // no‑op
    }

    @Override
    public void resetTestData() {
        // no‑op
    }

    @Override
    public void addWriteMark(DiskMark mark) {
        // no‑op for write marks
    }

    @Override
    public void addReadMark(DiskMark mark) {
        readMarksCount++;
    }

    @Override
    public void updateTitle(String text) {
        // no‑op
    }

    @Override
    public void adjustSensitivity() {
        // no‑op
    }

    @Override
    public void showPlainMessageDialog(String message, String title) {
        // no‑op
    }

    @Override
    public void showErrorMessageDialog(String message, String title) {
        // no‑op
    }

    @Override
    public void addRun(DiskRun run) {
        // no‑op
    }
}