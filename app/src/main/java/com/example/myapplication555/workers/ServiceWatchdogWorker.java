package com.example.myapplication555.workers;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myapplication555.service.RemoteControlService;

/**
 * Service Watchdog Worker - Ensures RemoteControlService stays running
 * Uses WorkManager for maximum persistence and automatic retry
 * This is the "bulletproof" layer that guarantees service availability
 */
public class ServiceWatchdogWorker extends Worker {
    private static final String TAG = "ServiceWatchdogWorker";

    public ServiceWatchdogWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "🔍 Watchdog checking service status...");
        
        try {
            Context context = getApplicationContext();
            
            // Check if RemoteControlService is running
            if (!isServiceRunning(context, RemoteControlService.class)) {
                Log.i(TAG, "⚠️ Service not running - restarting...");
                
                // Service is not running, restart it
                Intent serviceIntent = new Intent(context, RemoteControlService.class);
                serviceIntent.putExtra("watchdog_restart", true);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
                
                Log.i(TAG, "✅ Service restarted by watchdog");
                return Result.success();
            } else {
                Log.d(TAG, "✅ Service is running - watchdog check passed");
                return Result.success();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Watchdog error", e);
            return Result.retry(); // Retry on error
        }
    }

    /**
     * Check if a specific service is running
     * @param context Application context
     * @param serviceClass Service class to check
     * @return true if service is running, false otherwise
     */
    private boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }
} 