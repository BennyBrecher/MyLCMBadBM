package edu.touro.mco152.bm.commands;

import edu.touro.mco152.bm.BenchmarkSettings;
import edu.touro.mco152.bm.DiskWorker;
import edu.touro.mco152.bm.GeneralUI;

/**
 * A ConcreteCommand that wraps WriteBenchMarkCommandReceiver.
 * Its execute() method calls receiver.doWrite(...) and then updates settings.nextMarkNumber.
 */
public class WriteBenchmarkCommand implements CommandInterface {

    private final BenchmarkSettings settings;
    private final GeneralUI ui;
    private final DiskWorker worker;
    private final WriteBenchMarkCommandReceiver receiver;

    public WriteBenchmarkCommand(
            BenchmarkSettings settings,
            GeneralUI ui,
            DiskWorker worker,
            WriteBenchMarkCommandReceiver receiver
    ) {
        this.settings = settings;
        this.ui = ui;
        this.worker = worker;
        this.receiver = receiver;
    }

    @Override
    public void execute() throws Exception {
        int newNext = receiver.doWrite(settings, ui, worker);
        settings.setNextMarkNumber(newNext);
    }
}