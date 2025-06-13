package com.example;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for test-related functions
 */
public class TestUtils {
    
    /**
     * Safely shuts down an ExecutorService
     * 
     * @param executor The executor service to shut down
     * @param timeoutSeconds Maximum time to wait
     */
    public static void shutdownExecutor(ExecutorService executor, int timeoutSeconds) {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                    System.err.println("ExecutorService did not terminate");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
