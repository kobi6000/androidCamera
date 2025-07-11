package com.example.myapplication555.controllers;

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
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;

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
 * Camera Controller - Handles camera operations using Camera2 API
 * Focuses on rear camera for picture taking as per requirements
 */
public class CameraController {
    private static final String TAG = "CameraController";
    
    private final Context context;
    private CameraManager cameraManager;
    private String rearCameraId;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private final Semaphore cameraOpenCloseLock = new Semaphore(1);
    private boolean isCameraOpen = false;

    /**
     * Constructor
     * @param context Application context
     */
    public CameraController(Context context) {
        this.context = context;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        findRearCamera();
        startBackgroundThread();
        
        Log.i(TAG, "CameraController initialized");
    }

    /**
     * Open the rear camera
     * @return true if successful, false otherwise
     */
    public boolean openCamera() {
        Log.d(TAG, "Opening camera");
        
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                Log.e(TAG, "Time out waiting to lock camera opening");
                return false;
            }
            
            if (rearCameraId == null) {
                Log.e(TAG, "No rear camera found");
                return false;
            }
            
            if (isCameraOpen) {
                Log.d(TAG, "Camera already open");
                return true;
            }
            
            cameraManager.openCamera(rearCameraId, stateCallback, backgroundHandler);
            return true;
            
        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot access camera", e);
            return false;
        } catch (SecurityException e) {
            Log.e(TAG, "Camera permission not granted", e);
            return false;
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for camera", e);
            return false;
        }
    }

    /**
     * Take a picture using the rear camera
     * @return file path of captured image, null if failed
     */
    public String takePicture() {
        Log.d(TAG, "Taking picture");
        
        if (!isCameraOpen) {
            Log.w(TAG, "Camera not open, attempting to open");
            if (!openCamera()) {
                Log.e(TAG, "Failed to open camera for picture taking");
                return null;
            }
            
            // Wait a bit for camera to be ready
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while waiting for camera to be ready", e);
                return null;
            }
        }
        
        try {
            // Create image file
            File imageFile = createImageFile();
            if (imageFile == null) {
                Log.e(TAG, "Failed to create image file");
                return null;
            }
            
            // Setup image reader
            setupImageReader(imageFile);
            
            // Create capture session and take picture
            return captureStillPicture(imageFile);
            
        } catch (Exception e) {
            Log.e(TAG, "Error taking picture", e);
            return null;
        }
    }

    /**
     * Find the rear camera ID
     */
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

    /**
     * Create a unique image file for storing the captured picture
     * Pictures will be saved to DCIM/Camera folder and appear in Gallery
     */
    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "IMG_" + timeStamp + "_";
            
            // Save to DCIM/Camera folder (visible in Gallery)
            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/Camera");
            
            if (storageDir != null && !storageDir.exists()) {
                storageDir.mkdirs();
            }
            
            File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
            Log.d(TAG, "Created image file: " + imageFile.getAbsolutePath());
            return imageFile;
            
        } catch (IOException e) {
            Log.e(TAG, "Error creating image file", e);
            return null;
        }
    }

    /**
     * Setup image reader for capturing images
     */
    private void setupImageReader(File imageFile) {
        imageReader = ImageReader.newInstance(1920, 1080, 
                android.graphics.ImageFormat.JPEG, 1);
        
        imageReader.setOnImageAvailableListener(new OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                saveImage(reader, imageFile);
            }
        }, backgroundHandler);
    }

    /**
     * Capture still picture
     */
    private String captureStillPicture(File imageFile) {
        if (cameraDevice == null) {
            Log.e(TAG, "Camera device is null");
            return null;
        }
        
        try {
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
                                
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Error capturing image", e);
                            }
                        }
                        
                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Failed to configure capture session");
                        }
                    }, backgroundHandler);
            
            return imageFile.getAbsolutePath();
            
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error creating capture session", e);
            return null;
        }
    }

    /**
     * Save captured image to file and notify Gallery
     */
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
                    Log.d(TAG, "Image saved to: " + file.getAbsolutePath());
                    
                    // Notify media scanner - improved for Android 13+
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(android.net.Uri.fromFile(file));
                    context.sendBroadcast(mediaScanIntent);
                    
                    // Additional notification for gallery refresh
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, 
                        android.net.Uri.parse("file://" + Environment.getExternalStorageDirectory())));
                    
                } catch (IOException e) {
                    Log.e(TAG, "Error saving image", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
        } finally {
            if (image != null) {
                image.close();
            }
        }
    }

    /**
     * Camera device state callback
     */
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraOpenCloseLock.release();
            cameraDevice = camera;
            isCameraOpen = true;
            Log.d(TAG, "Camera opened successfully");
        }
        
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
            isCameraOpen = false;
            Log.d(TAG, "Camera disconnected");
        }
        
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
            isCameraOpen = false;
            Log.e(TAG, "Camera error: " + error);
        }
    };

    /**
     * Start background thread for camera operations
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    /**
     * Stop background thread
     */
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

    /**
     * Clean up camera resources
     */
    public void cleanup() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
            isCameraOpen = false;
        }
        
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        
        stopBackgroundThread();
    }
} 