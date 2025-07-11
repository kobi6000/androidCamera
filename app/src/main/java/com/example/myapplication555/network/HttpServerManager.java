package com.example.myapplication555.network;

import android.util.Log;

import com.example.myapplication555.core.CommandProcessor;

import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

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
        
        // Handle GET request for server status
        if (Method.GET.equals(session.getMethod())) {
            return handleGetRequest(uri);
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
} 