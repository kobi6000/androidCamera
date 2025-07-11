#!/usr/bin/env python3
"""
Example Usage Script for Android Remote Control Application
Demonstrates how to use the AndroidRemoteController class

Make sure to:
1. Install the Android app on your device
2. Start the remote control service
3. Connect both devices to the same WiFi network
4. Replace DEVICE_IP with your Android device's IP address
"""

import time
import json
from remote_control import AndroidRemoteController

# Replace with your Android device's IP address
DEVICE_IP = "192.168.1.100"  # Change this to your device's IP

def main():
    print("🚀 Android Remote Control - Example Usage")
    print("=" * 50)
    
    # Initialize controller
    controller = AndroidRemoteController(DEVICE_IP)
    
    # Example 1: Check server status
    print("\n📋 Example 1: Checking Server Status")
    status_result = controller.get_server_status()
    if status_result.get("success"):
        print("✅ Server is running and reachable")
    else:
        print("❌ Server is not reachable. Check your setup.")
        return
    
    # Example 2: Get device information
    print("\n📋 Example 2: Getting Device Information")
    device_info = controller.get_device_info()
    print("📱 Device Information:")
    for prop, value in device_info.items():
        print(f"   {prop}: {value}")
    
    # Example 3: Get specific property
    print("\n📋 Example 3: Getting Specific Property")
    model_result = controller.get_property("ro.product.model")
    if model_result.get("success"):
        print(f"📱 Device Model: {model_result.get('value')}")
    
    # Example 4: Open camera
    print("\n📋 Example 4: Opening Camera")
    camera_result = controller.open_camera()
    if camera_result.get("success"):
        print("📷 Camera opened successfully")
        
        # Wait a moment for camera to initialize
        print("⏱️ Waiting for camera to initialize...")
        time.sleep(3)
        
        # Example 5: Take picture
        print("\n📋 Example 5: Taking Picture")
        picture_result = controller.take_picture()
        if picture_result.get("success"):
            print("📸 Picture taken successfully!")
            if "image_path" in picture_result:
                print(f"📁 Image saved to: {picture_result['image_path']}")
        else:
            print("❌ Failed to take picture")
    else:
        print("❌ Failed to open camera")
    
    # Example 6: Get multiple properties
    print("\n📋 Example 6: Getting Multiple Properties")
    properties_to_check = [
        "ro.product.manufacturer",
        "ro.build.version.release",
        "ro.build.version.sdk",
        "ro.product.brand"
    ]
    
    print("📋 Device Properties:")
    for prop in properties_to_check:
        result = controller.get_property(prop)
        if result.get("success"):
            print(f"   {prop}: {result.get('value')}")
        else:
            print(f"   {prop}: Failed to retrieve")
    
    print("\n✅ Example usage completed!")
    print("\n💡 Tips:")
    print("   - Use interactive mode: python remote_control.py {} interactive".format(DEVICE_IP))
    print("   - Check server status: python remote_control.py {} status".format(DEVICE_IP))
    print("   - Take quick picture: python remote_control.py {} take_picture".format(DEVICE_IP))

if __name__ == "__main__":
    main() 