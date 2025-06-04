// File: src/test/java/edu/touro/mco152/bm/CommandPatternTests.java

package edu.touro.mco152.bm;

import edu.touro.mco152.bm.commands.ReadBenchmarkCommand;
import edu.touro.mco152.bm.commands.ReadBenchmarkCommandReceiver;
import edu.touro.mco152.bm.commands.WriteBenchMarkCommandReceiver;
import edu.touro.mco152.bm.commands.WriteBenchmarkCommand;
import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.ui.ConsoleUI;
import edu.touro.mco152.bm.ui.Gui;
import edu.touro.mco152.bm.ui.MainFrame;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class CommandPatternTests {

    private File tempDir;

    /**
     * (1)  The bruteforce setUp that Prof. provided in the previous BadBM assignment.
     */
    private static void setupDefaultAsPerProperties() {
        Gui.mainFrame = new MainFrame();

        App.p = new Properties();
        App.loadConfig();

        Gui.progressBar = Gui.mainFrame.getProgressBar();
        System.setProperty("derby.system.home", App.APP_CACHE_DIR);

        if (App.locationDir == null) {
            App.locationDir = new File(System.getProperty("user.home"));
        }
        App.dataDir = new File(App.locationDir.getAbsolutePath() + File.separator + App.DATADIRNAME);

        if (App.dataDir.exists()) {
            App.dataDir.delete();
        }
        App.dataDir.mkdirs();
    }

    @BeforeEach
    void setUp() {
        setupDefaultAsPerProperties();

        tempDir = new File(System.getProperty("java.io.tmpdir"), "badbm_test_" + System.nanoTime());
        tempDir.mkdirs();
        App.dataDir = tempDir;

        App.numOfMarks    = 25;
        App.numOfBlocks   = 128;
        App.blockSizeKb   = 2048;
        App.blockSequence = DiskRun.BlockSequence.SEQUENTIAL;

        App.nextMarkNumber = 0;
    }

    @AfterEach
    void tearDown() {
        deleteRecursively(tempDir);
    }

    private void deleteRecursively(File f) {
        if (f.isDirectory()) {
            for (File child : f.listFiles()) {
                deleteRecursively(child);
            }
        }
        f.delete();
    }

    private static class StubWorker extends DiskWorker {
        private final GeneralUI ui;

        StubWorker(BenchmarkSettings settings, GeneralUI ui) {
            // pass dummy benchmarker; we never call doInBackground()
            super(settings, ui, (s, u, w) -> true);
            this.ui = ui;
        }

        @Override
        public void publishFromOutside(DiskMark mark) {
            // As soon as doWrite()/doRead() calls worker.publishFromOutside(mark),
            // immediately forward that DiskMark to ui.
            ui.addWriteMark(mark);
            ui.addReadMark(mark);
        }

        @Override
        public boolean isCancelledFromOutside() {
            return false;
        }

        @Override
        public void setProgressFromOutside(int percent) {
            // no‐op in tests
        }
    }

    /**
    Verify 25 files exist and are the correct size; nextMarkNumber should be 25.
     */
    @Test
    public void testWriteBenchmarkCommand() throws Exception {
        // ===== 1) Turn “on” write, “off” read =====
        App.writeTest = true;
        App.readTest  = false;

        // ===== 2) Build basic settings UI and worker to pass =====
        BenchmarkSettings settings = new AppBenchmarkSettings();
        ConsoleUI ui = new ConsoleUI();
        StubWorker       worker   = new StubWorker(settings, ui);

        // ===== 4) Create and run the Write command =====
        WriteBenchMarkCommandReceiver receiver = new WriteBenchMarkCommandReceiver();
        WriteBenchmarkCommand writeCmd = new WriteBenchmarkCommand(settings, ui, worker, receiver);

        writeCmd.execute();  // ← running should create 25 files

        // ===== 5) After execute, nextMarkNumber must be 25 =====
        assertEquals(25, settings.getNextMarkNumber());

        // ===== 6) Verify exactly 25 files named testdata0.jdm … testdata24.jdm exist & are correct size =====
        long expectedSize = 128L * 2048L * 1024L;  // 128 blocks × 2048KB × 1024 bytes
        for (int i = 0; i < 25; i++) {
            File f = new File(tempDir, "testdata" + i + ".jdm");
            assertTrue(f.exists(), "Expected file: " + f.getName());
            assertEquals(expectedSize, f.length());
        }
    }

    /**
     Verify nextMarkNumber is 25 and ConsoleUI saw exactly 25 addReadMark(...) calls.
     */
    @Test
    public void testReadBenchmarkCommand() throws Exception {
        // --- STEP A) Build & run Write pass first so files exist ---
        App.writeTest = true;
        App.readTest  = false;

        BenchmarkSettings writeSettings = new AppBenchmarkSettings();
        ConsoleUI writeUI = new ConsoleUI();

        StubWorker       writeWorker   = new StubWorker(writeSettings, writeUI);

        WriteBenchMarkCommandReceiver wrReceiver = new WriteBenchMarkCommandReceiver();
        WriteBenchmarkCommand wrCmd = new WriteBenchmarkCommand(writeSettings, writeUI, writeWorker, wrReceiver);

        wrCmd.execute();  // ← now 25 files exist

        // sanity‐check that files exist
        for (int i = 0; i < 25; i++) {
            File f = new File(tempDir, "testdata" + i + ".jdm");
            assertTrue(f.exists());
        }

        // Reset nextMarkNumber back to 0 so read will start at testdata0.jdm again
        writeSettings.setNextMarkNumber(0);

        // --- STEP B) Now do the Read ---
        App.writeTest = false;
        App.readTest  = true;

        BenchmarkSettings readSettings = new AppBenchmarkSettings();
        ConsoleUI readUI = new ConsoleUI();

        DiskWorker readWorker = new DiskWorker(readSettings, readUI, new GeneralUsageBenchmarker()) {
            @Override
            public void publishFromOutside(DiskMark mark) {
                // Immediately forward to addReadMark(mark) so we get a count
                readUI.addReadMark(mark);
            }
            @Override
            public void setProgressFromOutside(int percent) { }
            @Override
            public boolean isCancelledFromOutside() { return false; }
        };

        ReadBenchmarkCommandReceiver rdReceiver = new ReadBenchmarkCommandReceiver();
        ReadBenchmarkCommand rdCmd = new ReadBenchmarkCommand(readSettings, readUI, readWorker, rdReceiver);

        rdCmd.execute();  // ← should loop exactly 25 times

        // --- STEP C) nextMarkNumber must be 25 now ---
        assertEquals(25, readSettings.getNextMarkNumber());

        // --- STEP D) ConsoleUI must have seen 25 calls to addReadMark(...) ---
        assertEquals(25, readUI.readMarksCount());
    }
}