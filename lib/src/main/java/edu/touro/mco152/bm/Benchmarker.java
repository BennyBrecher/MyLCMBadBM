package edu.touro.mco152.bm;

public interface Benchmarker {
    boolean runBenchmark(BenchmarkSettings settings, GeneralUI ui, DiskWorker worker) throws Exception;
}
