package edu.touro.mco152.bm.commands;


import edu.touro.mco152.bm.BenchmarkSettings;
import edu.touro.mco152.bm.DiskWorker;
import edu.touro.mco152.bm.GeneralUI;
/**
 * Executes a read benchmark by delegating to its receiver.
 */
public class ReadBenchmarkCommand implements CommandInterface{

    private final BenchmarkSettings settings;
    private final GeneralUI ui;
    private final DiskWorker worker;
    private final ReadBenchmarkCommandReceiver receiver;

    public ReadBenchmarkCommand(BenchmarkSettings settings, GeneralUI ui, DiskWorker worker, ReadBenchmarkCommandReceiver receiver){
        this.settings = settings;
        this.ui = ui;
        this.worker = worker;
        this.receiver = receiver;

    }

    @Override
    public void execute(){
        int newNext = receiver.doRead(settings, ui, worker);
        settings.setNextMarkNumber(newNext);
    }
}
