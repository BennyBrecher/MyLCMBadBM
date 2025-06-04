package edu.touro.mco152.bm.commands;
/**
 * Command interface representing a single, executable benchmarking action request
 */
public interface CommandInterface {
    void execute()throws Exception;
}
