package edu.touro.mco152.bm;

import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.persist.EM;

import jakarta.persistence.EntityManager;
import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.touro.mco152.bm.DiskMark.MarkType.READ;
import static edu.touro.mco152.bm.DiskMark.MarkType.WRITE;

/**
 * Run the disk benchmarking as a Swing-compliant thread (only one of these threads can run at
 * once.) Cooperates with Swing to provide and make use of interim and final progress and
 * information, which is also recorded as needed to the persistence store, and log.
 * <p>
 * Depends on static values that describe the benchmark to be done having been set in App and Gui classes.
 * The DiskRun class is used to keep track of and persist info about each benchmark at a higher level (a run),
 * while the DiskMark class described each iteration's result, which is displayed by the UI as the benchmark run
 * progresses.
 * <p>
 * This class only knows how to do 'read' or 'write' disk benchmarks. It is instantiated by the
 * startBenchmark() method.
 * <p>
 * To be Swing compliant this class extends SwingWorker and declares that its final return (when
 * doInBackground() is finished) is of type Boolean, and declares that intermediate results are communicated to
 * Swing using an instance of the DiskMark class.
 */

public class DiskWorker extends SwingWorker<Boolean, DiskMark> {

    // Record any success or failure status returned from SwingWorker (might be us or super)
    Boolean lastStatus = null;  // so far unknown
    private final BenchmarkSettings settings;
    private final GeneralUI ui;
    private final Benchmarker benchmarker;
    public DiskWorker(BenchmarkSettings settings, GeneralUI ui, Benchmarker benchmarker){
        this.settings = settings;
        this.ui = ui;
        this.benchmarker = benchmarker;
    }

    public void publishFromOutside(DiskMark mark) {
        publish(mark);
    }

    public void setProgressFromOutside(int percent) {
        setProgress(percent);
    }

    public boolean isCancelledFromOutside() {
        return isCancelled();
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        return benchmarker.runBenchmark(settings, ui, this);
    }

    /**
     * Process a list of 'chunks' that have been processed, ie that our thread has previously
     * published to Swing. For my info, watch Professor Cohen's video -
     * Module_6_RefactorBadBM Swing_DiskWorker_Tutorial.mp4
     * @param markList a list of DiskMark objects reflecting some completed benchmarks
     */
    @Override
    protected void process(List<DiskMark> markList) {
        markList.stream().forEach((dm) -> {
            if (dm.type == DiskMark.MarkType.WRITE) {
                ui.addWriteMark(dm);
            } else {
                ui.addReadMark(dm);
            }
        });
    }


    @Override
    protected void done() {
        // Obtain final status, might from doInBackground ret value, or SwingWorker error
        try {
            lastStatus = super.get();   // record for future access
        } catch (Exception e) {
            Logger.getLogger(DiskWorker.class.getName()).warning("Problem obtaining final status: " + e.getMessage());
        }

        if (settings.isAutoRemoveData()) {
            Util.deleteDirectory(settings.getDataDir());
        }
        settings.setIdleState();
        ui.adjustSensitivity();
    }

    public Boolean getLastStatus() {
        return lastStatus;
    }
}