package edu.touro.mco152.bm;

import edu.touro.mco152.bm.commands.*;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GeneralUsageBenchmarker implements Benchmarker {
    @Override
    public boolean runBenchmark(BenchmarkSettings settings, GeneralUI ui, DiskWorker worker) throws Exception{

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


        ui.updateLegend();  // init chart legend info

        if (settings.isAutoReset()) {
            settings.resetTestData();
            ui.resetTestData();
        }

        MyInvoker simpleExecutor = new MyInvoker();



        /*
          The GUI allows a Write, Read, or both types of BMs to be started. They are done serially.
         */
        if (settings.isWriteTest()) {
            WriteBenchMarkCommandReceiver writeReceiver = new WriteBenchMarkCommandReceiver();
            WriteBenchmarkCommand writeCommand = new WriteBenchmarkCommand(settings,ui,worker, writeReceiver);
            simpleExecutor.submit(writeCommand);
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
            ReadBenchmarkCommandReceiver readReceiver = new ReadBenchmarkCommandReceiver();
            ReadBenchmarkCommand readCommand = new ReadBenchmarkCommand(settings,ui, worker, readReceiver);
            simpleExecutor.submit(readCommand);
        }

        simpleExecutor.runAll();
        settings.incrementNextMarkNumber();
        return true;
    }
}