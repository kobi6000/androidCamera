package com.example.myapplication555.core;

import android.content.Context;
import android.util.Log;

import com.example.myapplication555.controllers.CameraController;
import com.example.myapplication555.controllers.PropertyController;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import android.os.Environment;

/**
 * Command Processor - Routes and processes incoming commands
 * Implements command pattern for extensibility and clean architecture
 */
public class CommandProcessor {
    private static final String TAG = "CommandProcessor";
    
    // Command constants
    public static final String CMD_OPEN_CAMERA = "open_camera";
    public static final String CMD_TAKE_PICTURE = "take_picture";
    public static final String CMD_GET_PROPERTY = "get_property";
    public static final String CMD_DOWNLOAD_PICTURE = "download_picture";
    
    private final Context context;
    private final CameraController cameraController;
    private final PropertyController propertyController;
    private final Gson gson;

    /**
     * Constructor
     * @param context Application context
     */
    public CommandProcessor(Context context) {
        this.context = context;
        this.cameraController = new CameraController(context);
        this.propertyController = new PropertyController();
        this.gson = new Gson();
        
        Log.i(TAG, "CommandProcessor initialized");
    }

    /**
     * Process incoming command and return response
     * @param commandJson JSON command string from remote client
     * @return JSON response string
     */
    public String processCommand(String commandJson) {
        Log.d(TAG, "Processing command: " + commandJson);
        
        try {
            // Parse incoming JSON command
            JsonObject command = JsonParser.parseString(commandJson).getAsJsonObject();
            
            if (!command.has("action")) {
                return createErrorResponse("Missing 'action' field in command");
            }
            
            String action = command.get("action").getAsString();
            
            // Route command to appropriate handler
            switch (action) {
                case CMD_OPEN_CAMERA:
                    return handleOpenCamera();
                    
                case CMD_TAKE_PICTURE:
                    return handleTakePicture();
                    
                case CMD_GET_PROPERTY:
                    if (!command.has("property")) {
                        return createErrorResponse("Missing 'property' field for get_property command");
                    }
                    String property = command.get("property").getAsString();
                    return handleGetProperty(property);
                    
                case CMD_DOWNLOAD_PICTURE:
                    if (!command.has("image_path")) {
                        return createErrorResponse("Missing 'image_path' field for download_picture command");
                    }
                    String imagePath = command.get("image_path").getAsString();
                    return handleDownloadPicture(imagePath);
                    
                default:
                    return createErrorResponse("Unknown command: " + action);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing command", e);
            return createErrorResponse("Command processing error: " + e.getMessage());
        }
    }

    /**
     * Handle open camera command
     * @return JSON response
     */
    private String handleOpenCamera() {
        Log.d(TAG, "Handling open camera command");
        
        try {
            boolean success = cameraController.openCamera();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("message", success ? "Camera opened successfully" : "Failed to open camera");
            response.addProperty("action", CMD_OPEN_CAMERA);
            
            return gson.toJson(response);
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera", e);
            return createErrorResponse("Camera error: " + e.getMessage());
        }
    }

    /**
     * Handle take picture command (rear camera only)
     * @return JSON response with image path if successful
     */
    private String handleTakePicture() {
        Log.d(TAG, "Handling take picture command");
        
        try {
            String imagePath = cameraController.takePicture();
            boolean success = imagePath != null;
            
            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("action", CMD_TAKE_PICTURE);
            
            if (success) {
                response.addProperty("message", "Picture taken successfully");
                response.addProperty("image_path", imagePath);
            } else {
                response.addProperty("message", "Failed to take picture");
            }
            
            return gson.toJson(response);
        } catch (Exception e) {
            Log.e(TAG, "Error taking picture", e);
            return createErrorResponse("Picture capture error: " + e.getMessage());
        }
    }

    /**
     * Handle get property command using getprop
     * @param property Property name to fetch
     * @return JSON response with property value
     */
    private String handleGetProperty(String property) {
        Log.d(TAG, "Handling get property command for: " + property);
        
        try {
            String value = propertyController.getProperty(property);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", value != null);
            response.addProperty("action", CMD_GET_PROPERTY);
            response.addProperty("property", property);
            
            if (value != null) {
                response.addProperty("value", value);
                response.addProperty("message", "Property retrieved successfully");
            } else {
                response.addProperty("message", "Property not found or access denied");
            }
            
            return gson.toJson(response);
        } catch (Exception e) {
            Log.e(TAG, "Error getting property", e);
            return createErrorResponse("Property retrieval error: " + e.getMessage());
        }
    }

    /**
     * Handle download picture command
     * @param imagePath Path to the image file to download
     * @return JSON response with file info if successful
     */
    private String handleDownloadPicture(String imagePath) {
        Log.d(TAG, "Handling download picture command for: " + imagePath);
        
        try {
            // Validate and check if file exists with retry mechanism
            File imageFile = new File(imagePath);
            
            // Retry mechanism - sometimes files need time to be fully written
            int maxRetries = 5;
            long retryDelay = 500; // 500ms
            
            for (int i = 0; i < maxRetries; i++) {
                if (imageFile.exists() && imageFile.length() > 0) {
                    Log.d(TAG, "File found and has content on attempt " + (i + 1));
                    break;
                } else if (i < maxRetries - 1) {
                    Log.d(TAG, "File not ready on attempt " + (i + 1) + ", retrying in " + retryDelay + "ms");
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        Log.w(TAG, "Interrupted during file wait", e);
                    }
                }
            }
            
            if (!imageFile.exists()) {
                Log.w(TAG, "Image file not found after retries: " + imagePath);
                return createErrorResponse("Image file not found: " + imagePath);
            }
            
            if (!imageFile.canRead()) {
                Log.w(TAG, "Image file not readable: " + imagePath);
                return createErrorResponse("Image file not accessible: " + imagePath);
            }
            
            long fileSize = imageFile.length();
            if (fileSize == 0) {
                Log.w(TAG, "Image file is empty: " + imagePath);
                return createErrorResponse("Image file is empty or still being written: " + imagePath);
            }
            
            // Basic security check - ensure file is in expected directories
            String absolutePath = imageFile.getAbsolutePath();
            String dcimPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
            String picturesPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
            
            if (!absolutePath.startsWith(dcimPath) && !absolutePath.startsWith(picturesPath)) {
                Log.w(TAG, "Security check failed - file outside allowed directories: " + imagePath);
                return createErrorResponse("File access denied - invalid path");
            }
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("action", CMD_DOWNLOAD_PICTURE);
            response.addProperty("message", "File ready for download");
            response.addProperty("image_path", imagePath);
            response.addProperty("file_size", fileSize);
            response.addProperty("file_name", imageFile.getName());
            response.addProperty("download_url", "/download?file=" + java.net.URLEncoder.encode(imagePath, "UTF-8"));
            
            Log.d(TAG, "File ready for download: " + imagePath + " (" + fileSize + " bytes)");
            return gson.toJson(response);
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing download request", e);
            return createErrorResponse("Download preparation error: " + e.getMessage());
        }
    }

    /**
     * Create standardized error response
     * @param error Error message
     * @return JSON error response
     */
    private String createErrorResponse(String error) {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", error);
        response.addProperty("message", "Command failed: " + error);
        
        return gson.toJson(response);
    }

    /**
     * Create standardized success response
     * @param message Success message
     * @return JSON success response
     */
    private String createSuccessResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", message);
        
        return gson.toJson(response);
    }
} 