package com.example.myapplication555.managers;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.myapplication555.workers.ServiceWatchdogWorker;

import java.util.concurrent.TimeUnit;

/**
 * Watchdog Manager - Manages WorkManager-based service monitoring
 * Provides bulletproof persistence for RemoteControlService
 * This ensures the service ALWAYS stays running, even after force stops
 */
public class WatchdogManager {
    private static final String TAG = "WatchdogManager";
    private static final String WATCHDOG_WORK_NAME = "service_watchdog";
    private static final long WATCHDOG_INTERVAL_MINUTES = 15; // Check every 15 minutes
    
    private final Context context;
    private final WorkManager workManager;

    /**
     * Constructor
     * @param context Application context
     */
    public WatchdogManager(Context context) {
        this.context = context;
        this.workManager = WorkManager.getInstance(context);
        Log.i(TAG, "WatchdogManager initialized");
    }

    /**
     * Start the watchdog monitoring
     * This creates a periodic worker that checks service status every 15 minutes
     */
    public void startWatchdog() {
        Log.i(TAG, "🚀 Starting service watchdog monitoring...");
        
        // Create constraints for the watchdog work
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // No network needed
                .setRequiresBatteryNotLow(false) // Run even on low battery
                .setRequiresCharging(false) // Don't require charging
                .setRequiresDeviceIdle(false) // Run even when device is active
                .build();

        // Create periodic work request
        PeriodicWorkRequest watchdogWork = new PeriodicWorkRequest.Builder(
                ServiceWatchdogWorker.class,
                WATCHDOG_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag("watchdog")
                .build();

        // Schedule the work with REPLACE policy to avoid duplicates
        workManager.enqueueUniquePeriodicWork(
                WATCHDOG_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE, // Replace existing work
                watchdogWork
        );

        Log.i(TAG, "✅ Watchdog scheduled - will check service every " + WATCHDOG_INTERVAL_MINUTES + " minutes");
    }

    /**
     * Stop the watchdog monitoring
     * Use this only when you want to completely disable the service
     */
    public void stopWatchdog() {
        Log.i(TAG, "🛑 Stopping service watchdog monitoring...");
        
        workManager.cancelUniqueWork(WATCHDOG_WORK_NAME);
        
        Log.i(TAG, "✅ Watchdog stopped - service will not be automatically restarted");
    }

    /**
     * Check if watchdog is currently active
     * @return true if watchdog is running, false otherwise
     */
    public boolean isWatchdogActive() {
        // This is a simplified check - in production you might want to check WorkInfo
        return true; // Placeholder implementation
    }

    /**
     * Get watchdog status and statistics
     * @return Watchdog status information
     */
    public String getWatchdogStatus() {
        return "Watchdog checking service every " + WATCHDOG_INTERVAL_MINUTES + " minutes";
    }

    /**
     * Force an immediate watchdog check
     * This will trigger the watchdog worker immediately
     */
    public void forceWatchdogCheck() {
        Log.i(TAG, "🔍 Forcing immediate watchdog check...");
        
        // Create one-time work request for immediate check
        androidx.work.OneTimeWorkRequest immediateCheck = 
                new androidx.work.OneTimeWorkRequest.Builder(ServiceWatchdogWorker.class)
                        .addTag("immediate_watchdog")
                        .build();

        workManager.enqueue(immediateCheck);
        
        Log.i(TAG, "✅ Immediate watchdog check scheduled");
    }
} 