package com.example.myapplication555;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.myapplication555.databinding.ActivityMainBinding;
import com.example.myapplication555.service.RemoteControlService;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * Main Activity for Remote Control Android Application
 * Handles permissions and starts the remote control service
 */
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 123;
    
    // Required permissions for remote control functionality
    // Note: Boot permission is automatically granted on install
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE
    };

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // Start/Stop Remote Control Service
        binding.fab.setOnClickListener(this::toggleRemoteControlService);
        
        // Check and request permissions on startup
        checkPermissions();
        
        // Check if service is already running (auto-started)
        checkServiceStatus();
        
        Log.i(TAG, "MainActivity created");
    }

    /**
     * Toggle remote control service on/off
     */
    private void toggleRemoteControlService(View view) {
        if (hasAllPermissions()) {
            if (!isServiceRunning) {
                startRemoteControlService();
            } else {
                stopRemoteControlService();
            }
        } else {
            requestPermissions();
        }
    }

    /**
     * Start the remote control service
     */
    private void startRemoteControlService() {
        Intent serviceIntent = new Intent(this, RemoteControlService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        isServiceRunning = true;
        
        // Show device IP address for remote connection
        String ipAddress = getDeviceIPAddress();
        String message = "✅ Remote Control Started!\n\nDevice IP: " + ipAddress + ":8080\n\nUse this IP in your Python control app.";
        
        Snackbar.make(binding.getRoot(), "Remote Control Started - IP: " + ipAddress + ":8080", 
                     Snackbar.LENGTH_LONG).show();
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.i(TAG, "Remote control service started on IP: " + ipAddress + ":8080");
    }

    /**
     * Stop the remote control service
     */
    private void stopRemoteControlService() {
        Intent serviceIntent = new Intent(this, RemoteControlService.class);
        stopService(serviceIntent);
        
        isServiceRunning = false;
        
        Snackbar.make(binding.getRoot(), "Remote Control Stopped", Snackbar.LENGTH_SHORT).show();
        Log.i(TAG, "Remote control service stopped");
    }

    /**
     * Check if all required permissions are granted
     */
    private boolean hasAllPermissions() {
        return EasyPermissions.hasPermissions(this, REQUIRED_PERMISSIONS);
    }

    /**
     * Check and request permissions if needed
     */
    private void checkPermissions() {
        if (!hasAllPermissions()) {
            requestPermissions();
        }
    }

    /**
     * Request all required permissions
     */
    private void requestPermissions() {
        EasyPermissions.requestPermissions(
                this,
                "This app needs camera and network permissions to work properly.",
                REQUEST_CODE_PERMISSIONS,
                REQUIRED_PERMISSIONS
        );
    }

    /**
     * Get device IP address for WiFi connection
     */
    private String getDeviceIPAddress() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        if (wifiManager != null) {
            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
            return String.format("%d.%d.%d.%d",
                    (ipAddress & 0xff),
                    (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff),
                    (ipAddress >> 24 & 0xff));
        }
        return "Unable to get IP";
    }

    /**
     * Check if RemoteControlService is already running (auto-started)
     */
    private void checkServiceStatus() {
        if (isServiceRunning(RemoteControlService.class)) {
            isServiceRunning = true;
            
            // Show that service is already running
            String ipAddress = getDeviceIPAddress();
            String message = "🤖 Remote Control Auto-Started!\n\nDevice IP: " + ipAddress + ":8080\n\nTap button to STOP service.";
            
            Snackbar.make(binding.getRoot(), "Remote Control Running - IP: " + ipAddress + ":8080", 
                         Snackbar.LENGTH_LONG).show();
            
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.i(TAG, "Remote control service detected running on IP: " + ipAddress + ":8080");
        }
    }

    /**
     * Check if a specific service is running
     */
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    // Permission callbacks
    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "Permissions granted: " + perms);
        Toast.makeText(this, "Permissions granted! You can now start remote control.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.e(TAG, "Permissions denied: " + perms);
        Toast.makeText(this, "Required permissions denied. App won't work properly.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}