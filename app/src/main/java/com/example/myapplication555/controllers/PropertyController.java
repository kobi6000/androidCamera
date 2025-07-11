package com.example.myapplication555.controllers;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Property Controller - Handles device property retrieval using getprop command
 * Provides secure access to Android system properties
 */
public class PropertyController {
    private static final String TAG = "PropertyController";
    private static final int TIMEOUT_SECONDS = 10;

    /**
     * Constructor
     */
    public PropertyController() {
        Log.i(TAG, "PropertyController initialized");
    }

    /**
     * Get a specific device property using getprop command
     * @param property the property name to retrieve
     * @return property value or null if not found/error
     */
    public String getProperty(String property) {
        Log.d(TAG, "Getting property: " + property);
        
        if (property == null || property.trim().isEmpty()) {
            Log.w(TAG, "Property name is null or empty");
            return null;
        }
        
        // Sanitize property name to prevent command injection
        String sanitizedProperty = sanitizePropertyName(property);
        if (sanitizedProperty == null) {
            Log.w(TAG, "Invalid property name: " + property);
            return null;
        }
        
        try {
            // Execute getprop command
            Process process = Runtime.getRuntime().exec(new String[]{"getprop", sanitizedProperty});
            
            // Read output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            
            // Wait for process to complete with timeout
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                Log.e(TAG, "getprop command timed out for property: " + property);
                process.destroyForcibly();
                return null;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0 && line != null && !line.trim().isEmpty()) {
                String result = line.trim();
                Log.d(TAG, "Property " + property + " = " + result);
                return result;
            }
            
            Log.w(TAG, "Property not found or empty: " + property + " (exit code: " + exitCode + ")");
            return null;
            
        } catch (IOException e) {
            Log.e(TAG, "IO error getting property: " + property, e);
            return null;
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while getting property: " + property, e);
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error getting property: " + property, e);
            return null;
        }
    }

    /**
     * Get commonly used device properties as a formatted string
     * @return JSON-like string with multiple properties
     */
    public String getDeviceInfo() {
        Log.d(TAG, "Getting device info");
        
        StringBuilder info = new StringBuilder();
        info.append("{\n");
        
        // Common Android properties
        String[] commonProperties = {
            "ro.product.model",
            "ro.product.manufacturer", 
            "ro.product.brand",
            "ro.build.version.release",
            "ro.build.version.sdk",
            "ro.build.id",
            "ro.product.device",
            "ro.build.version.incremental",
            "ro.build.display.id"
        };
        
        String[] propertyNames = {
            "model",
            "manufacturer",
            "brand", 
            "android_version",
            "api_level",
            "build_id",
            "device",
            "build_incremental",
            "build_display"
        };
        
        for (int i = 0; i < commonProperties.length; i++) {
            String value = getProperty(commonProperties[i]);
            info.append("  \"").append(propertyNames[i]).append("\": \"");
            info.append(value != null ? value : "unknown").append("\"");
            if (i < commonProperties.length - 1) {
                info.append(",");
            }
            info.append("\n");
        }
        
        info.append("}");
        
        String result = info.toString();
        Log.d(TAG, "Device info: " + result);
        return result;
    }

    /**
     * Get specific property categories
     * @param category Property category (e.g., "build", "product", "system")
     * @return Formatted string with category properties
     */
    public String getPropertyCategory(String category) {
        Log.d(TAG, "Getting property category: " + category);
        
        if (category == null || category.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Execute getprop command with grep to filter by category
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "getprop | grep " + category});
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                Log.e(TAG, "getprop category command timed out");
                process.destroyForcibly();
                return null;
            }
            
            String output = result.toString().trim();
            Log.d(TAG, "Category " + category + " properties: " + output);
            return output.isEmpty() ? null : output;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting property category: " + category, e);
            return null;
        }
    }

    /**
     * Sanitize property name to prevent command injection
     * @param property Property name to sanitize
     * @return Sanitized property name or null if invalid
     */
    private String sanitizePropertyName(String property) {
        if (property == null) {
            return null;
        }
        
        // Remove any potentially dangerous characters
        String sanitized = property.replaceAll("[^a-zA-Z0-9._-]", "");
        
        // Check if it's a valid property name pattern
        if (sanitized.matches("^[a-zA-Z][a-zA-Z0-9._-]*$")) {
            return sanitized;
        }
        
        return null;
    }

    /**
     * Check if getprop command is available
     * @return true if getprop is available, false otherwise
     */
    public boolean isGetPropAvailable() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"which", "getprop"});
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            
            if (finished && process.exitValue() == 0) {
                Log.d(TAG, "getprop command is available");
                return true;
            }
            
            Log.w(TAG, "getprop command is not available");
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking getprop availability", e);
            return false;
        }
    }

    /**
     * Get all system properties (use with caution - can be large)
     * @return All system properties as string
     */
    public String getAllProperties() {
        Log.d(TAG, "Getting all properties");
        
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"getprop"});
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                Log.e(TAG, "getprop all properties command timed out");
                process.destroyForcibly();
                return null;
            }
            
            String output = result.toString().trim();
            Log.d(TAG, "Retrieved all properties (" + output.length() + " characters)");
            return output.isEmpty() ? null : output;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting all properties", e);
            return null;
        }
    }
} 