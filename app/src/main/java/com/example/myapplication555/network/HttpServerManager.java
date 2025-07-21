package com.example.myapplication555.network;

import android.os.Environment;
import android.util.Log;

import com.example.myapplication555.core.CommandProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.Gson;

/**
 * HTTP Server Manager - Manages HTTP server for remote command reception
 * Uses NanoHTTPD library for lightweight HTTP server functionality
 */
public class HttpServerManager extends NanoHTTPD {
    private static final String TAG = "HttpServerManager";
    private static final int PORT = 8080;
    
    private final CommandProcessor commandProcessor;
    private boolean isRunning = false;

    /**
     * Constructor
     * @param commandProcessor Command processor instance to handle requests
     */
    public HttpServerManager(CommandProcessor commandProcessor) {
        super(PORT);
        this.commandProcessor = commandProcessor;
        Log.i(TAG, "HttpServerManager initialized on port " + PORT);
    }

    /**
     * Start the HTTP server
     * @return true if started successfully, false otherwise
     */
    public boolean startServer() {
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            isRunning = true;
            Log.i(TAG, "HTTP Server started on port " + PORT);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to start HTTP server", e);
            return false;
        }
    }

    /**
     * Stop the HTTP server
     */
    public void stopServer() {
        if (isRunning) {
            stop();
            isRunning = false;
            Log.i(TAG, "HTTP Server stopped");
        }
    }

    /**
     * Handle HTTP requests
     * @param session HTTP session containing request details
     * @return HTTP response
     */
    @Override
    public Response serve(IHTTPSession session) {
        String method = session.getMethod().toString();
        String uri = session.getUri();
        
        Log.d(TAG, "Received " + method + " request to " + uri);
        
        // Add CORS headers for cross-origin requests
        Response response = handleRequest(session);
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        return response;
    }

    /**
     * Handle specific HTTP requests
     * @param session HTTP session
     * @return HTTP response
     */
    private Response handleRequest(IHTTPSession session) {
        String method = session.getMethod().toString();
        String uri = session.getUri();
        
        // Handle OPTIONS request for CORS preflight
        if (Method.OPTIONS.equals(session.getMethod())) {
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "");
        }
        
        // Handle GET request for server status and downloads
        if (Method.GET.equals(session.getMethod())) {
            return handleGetRequestWithSession(session);
        }
        
        // Handle POST request for commands
        if (Method.POST.equals(session.getMethod())) {
            return handlePostRequest(session);
        }
        
        // Method not allowed
        return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, 
                                    "application/json", 
                                    "{\"success\": false, \"message\": \"Method not allowed\"}");
    }

    /**
     * Handle GET requests
     * @param uri Request URI
     * @return HTTP response
     */
    private Response handleGetRequest(String uri) {
        Log.d(TAG, "Handling GET request: " + uri);
        
        if ("/".equals(uri) || "/status".equals(uri)) {
            // Server status endpoint
            String statusJson = "{\"success\": true, \"message\": \"Remote Control Server is running\", \"port\": " + PORT + "}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", statusJson);
        }
        
        if ("/health".equals(uri)) {
            // Health check endpoint
            String healthJson = "{\"success\": true, \"status\": \"healthy\", \"timestamp\": " + System.currentTimeMillis() + "}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", healthJson);
        }
        
        // Handle file download requests
        if (uri.startsWith("/download")) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST,
                                        "application/json",
                                        "{\"success\": false, \"message\": \"Use handleGetRequest with session for downloads\"}");
        }
        
        // Not found
        return newFixedLengthResponse(Response.Status.NOT_FOUND, 
                                    "application/json", 
                                    "{\"success\": false, \"message\": \"Endpoint not found\"}");
    }
    
    /**
     * Handle GET requests with session (for downloads)
     * @param session HTTP session
     * @return HTTP response
     */
    private Response handleGetRequestWithSession(IHTTPSession session) {
        String uri = session.getUri();
        Log.d(TAG, "Handling GET request with session: " + uri);
        
        if ("/".equals(uri) || "/status".equals(uri)) {
            // Server status endpoint
            String statusJson = "{\"success\": true, \"message\": \"Remote Control Server is running\", \"port\": " + PORT + "}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", statusJson);
        }
        
        if ("/health".equals(uri)) {
            // Health check endpoint
            String healthJson = "{\"success\": true, \"status\": \"healthy\", \"timestamp\": " + System.currentTimeMillis() + "}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", healthJson);
        }
        
        // Handle file download requests
        if (uri.startsWith("/download")) {
            return handleDownloadRequestWithSession(session);
        }
        
        // Not found
        return newFixedLengthResponse(Response.Status.NOT_FOUND, 
                                    "application/json", 
                                    "{\"success\": false, \"message\": \"Endpoint not found\"}");
    }

    /**
     * Handle POST requests (commands)
     * @param session HTTP session
     * @return HTTP response
     */
    private Response handlePostRequest(IHTTPSession session) {
        Log.d(TAG, "Handling POST request");
        
        try {
            // Parse request body
            Map<String, String> files = new java.util.HashMap<>();
            session.parseBody(files);
            
            String requestBody = files.get("postData");
            if (requestBody == null || requestBody.trim().isEmpty()) {
                Log.w(TAG, "Empty request body");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, 
                                            "application/json", 
                                            "{\"success\": false, \"message\": \"Empty request body\"}");
            }
            
            Log.d(TAG, "Request body: " + requestBody);
            
            // Process command
            String responseJson = commandProcessor.processCommand(requestBody);
            
            Log.d(TAG, "Response: " + responseJson);
            
            return newFixedLengthResponse(Response.Status.OK, "application/json", responseJson);
            
        } catch (IOException e) {
            Log.e(TAG, "Error parsing request body", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, 
                                        "application/json", 
                                        "{\"success\": false, \"message\": \"Error parsing request: " + e.getMessage() + "\"}");
        } catch (ResponseException e) {
            Log.e(TAG, "Response error", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, 
                                        "application/json", 
                                        "{\"success\": false, \"message\": \"Server error: " + e.getMessage() + "\"}");
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error processing request", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, 
                                        "application/json", 
                                        "{\"success\": false, \"message\": \"Unexpected error: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Handle request with authentication (future enhancement)
     * @param session HTTP session
     * @return HTTP response
     */
    private Response handleAuthenticatedRequest(IHTTPSession session) {
        // Check for authorization header
        Map<String, String> headers = session.getHeaders();
        String authHeader = headers.get("authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Log.w(TAG, "Missing or invalid authorization header");
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, 
                                        "application/json", 
                                        "{\"success\": false, \"message\": \"Authorization required\"}");
        }
        
        // Extract token
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        
        // Validate token (implement your token validation logic here)
        if (!isValidToken(token)) {
            Log.w(TAG, "Invalid token");
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, 
                                        "application/json", 
                                        "{\"success\": false, \"message\": \"Invalid token\"}");
        }
        
        // Token is valid, proceed with request
        return handleRequest(session);
    }

    /**
     * Validate authentication token (placeholder implementation)
     * @param token Authentication token
     * @return true if valid, false otherwise
     */
    private boolean isValidToken(String token) {
        // Placeholder implementation
        // In production, implement proper token validation
        return "your_secret_token".equals(token);
    }

    /**
     * Handle file download requests using NanoHTTPD's parameter parsing
     * @param session HTTP session with parameters
     * @return HTTP response with file content or error
     */
    private Response handleDownloadRequestWithSession(IHTTPSession session) {
        String uri = session.getUri();
        Log.d(TAG, "Handling download request with session: " + uri);
        
        // Create debug info object to return with response
        JsonObject debugInfo = new JsonObject();
        debugInfo.addProperty("original_uri", uri);
        debugInfo.addProperty("uri_length", uri.length());
        
        try {
            // Get query parameters using NanoHTTPD's built-in parsing
            Map<String, String> params = session.getParms();
            debugInfo.addProperty("params_count", params.size());
            
            // Add all parameters to debug info
            JsonObject paramsJson = new JsonObject();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                Log.d(TAG, "Parameter: " + key + " = " + value);
                paramsJson.addProperty(key, value);
            }
            debugInfo.add("parameters", paramsJson);
            
            // Get the file parameter
            String filePath = params.get("file");
            debugInfo.addProperty("raw_file_param", filePath);
            
            if (filePath == null || filePath.trim().isEmpty()) {
                Log.w(TAG, "No file parameter found in request");
                debugInfo.addProperty("error", "No file parameter found");
                return createDebugErrorResponse("Missing or empty file parameter", debugInfo);
            }
            
            Log.d(TAG, "File path from parameters: " + filePath);
            debugInfo.addProperty("final_file_path", filePath);
            
            // Security validation
            File requestedFile = new File(filePath);
            debugInfo.addProperty("file_exists", requestedFile.exists());
            debugInfo.addProperty("file_can_read", requestedFile.canRead());
            debugInfo.addProperty("file_absolute_path", requestedFile.getAbsolutePath());
            
            if (!requestedFile.exists()) {
                Log.w(TAG, "Requested file not found: " + filePath);
                debugInfo.addProperty("error", "File does not exist");
                return createDebugErrorResponse("File not found: " + filePath, debugInfo);
            }
            
            if (!requestedFile.canRead()) {
                Log.w(TAG, "Requested file not readable: " + filePath);
                debugInfo.addProperty("error", "File cannot be read");
                return createDebugErrorResponse("File not accessible: " + filePath, debugInfo);
            }
            
            long fileSize = requestedFile.length();
            debugInfo.addProperty("file_size", fileSize);
            
            if (fileSize == 0) {
                Log.w(TAG, "Requested file is empty: " + filePath);
                debugInfo.addProperty("error", "File is empty");
                return createDebugErrorResponse("File is empty: " + filePath, debugInfo);
            }
            
            // Basic security check - ensure file is in expected directories
            String absolutePath = requestedFile.getAbsolutePath();
            String dcimPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
            String picturesPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
            
            debugInfo.addProperty("dcim_path", dcimPath);
            debugInfo.addProperty("pictures_path", picturesPath);
            debugInfo.addProperty("path_check_dcim", absolutePath.startsWith(dcimPath));
            debugInfo.addProperty("path_check_pictures", absolutePath.startsWith(picturesPath));
            
            if (!absolutePath.startsWith(dcimPath) && !absolutePath.startsWith(picturesPath)) {
                Log.w(TAG, "Security check failed - file outside allowed directories: " + filePath);
                debugInfo.addProperty("error", "File outside allowed directories");
                return createDebugErrorResponse("File access denied - outside allowed directories", debugInfo);
            }
            
            // If we get here, everything is good - serve the file
            InputStream fileStream = new FileInputStream(requestedFile);
            String mimeType = getMimeType(requestedFile.getName());
            
            Response response = newChunkedResponse(Response.Status.OK, mimeType, fileStream);
            response.addHeader("Content-Disposition", "attachment; filename=\"" + requestedFile.getName() + "\"");
            response.addHeader("Content-Length", String.valueOf(fileSize));
            
            Log.d(TAG, "Serving file: " + filePath + " (" + fileSize + " bytes, " + mimeType + ")");
            return response;
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling download request: " + uri, e);
            debugInfo.addProperty("exception", e.getClass().getSimpleName());
            debugInfo.addProperty("exception_message", e.getMessage());
            return createDebugErrorResponse("Download error: " + e.getMessage(), debugInfo);
        }
    }
    
    /**
     * Get MIME type for file based on extension
     * @param filename File name
     * @return MIME type string
     */
    private String getMimeType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        
        String extension = filename.toLowerCase();
        if (extension.endsWith(".jpg") || extension.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (extension.endsWith(".png")) {
            return "image/png";
        } else if (extension.endsWith(".gif")) {
            return "image/gif";
        } else if (extension.endsWith(".bmp")) {
            return "image/bmp";
        } else if (extension.endsWith(".webp")) {
            return "image/webp";
        }
        
        return "application/octet-stream";
    }

    /**
     * Get server status
     * @return true if server is running, false otherwise
     */
    public boolean isServerRunning() {
        return isRunning;
    }

    /**
     * Get server port
     * @return Server port number
     */
    public int getServerPort() {
        return PORT;
    }

    /**
     * Create error response
     * @param status HTTP status
     * @param message Error message
     * @return HTTP response
     */
    private Response createErrorResponse(Response.Status status, String message) {
        String errorJson = "{\"success\": false, \"message\": \"" + message + "\"}";
        return newFixedLengthResponse(status, "application/json", errorJson);
    }

    /**
     * Create success response
     * @param data Response data
     * @return HTTP response
     */
    private Response createSuccessResponse(String data) {
        return newFixedLengthResponse(Response.Status.OK, "application/json", data);
    }

    /**
     * Create error response with debugging information
     */
    private Response createDebugErrorResponse(String message, JsonObject debugInfo) {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("message", message);
        response.add("debug_info", debugInfo);
        
        Gson gson = new Gson();
        return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", gson.toJson(response));
    }
} 