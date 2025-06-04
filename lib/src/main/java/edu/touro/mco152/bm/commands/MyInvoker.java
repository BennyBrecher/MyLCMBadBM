package edu.touro.mco152.bm.commands;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Simple serial executor that queues and runs submitted commands one by one.
 */
public class MyInvoker {
    Queue<CommandInterface> queue = new LinkedList<>();

    public void submit(CommandInterface command){
        queue.offer(command);
    }

    public void runAll()throws Exception{
        while(!queue.isEmpty()){
            CommandInterface command = queue.poll();
            command.execute();
        }
    }
}
