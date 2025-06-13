package com.example;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for test-related functions
 */
public class TestUtils {
    
    /**
     * Safely shuts down an ExecutorService with proper timeout handling
     * 
     * @param executor The executor service to shut down
     * @param timeoutSeconds Maximum time to wait for termination
     */
    public static void shutdownExecutor(ExecutorService executor, int timeoutSeconds) {
        if (executor == null) return;
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                    System.err.println("ExecutorService did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
