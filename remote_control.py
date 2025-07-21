#!/usr/bin/env python3
"""
Remote Control Application for Android Device
Sends commands to Android device over WiFi network

Usage:
    python remote_control.py <device_ip> <command> [options]
    
Commands:
    - open_camera: Open camera on device
    - take_picture: Take picture with rear camera
    - download_picture <image_path> [save_path]: Download image from device
    - take_and_download [save_path]: Take picture and download it immediately
    - get_property <property_name>: Get device property
    - status: Get server status
    - interactive: Enter interactive mode
    
Examples:
    python remote_control.py 192.168.1.100 open_camera
    python remote_control.py 192.168.1.100 take_picture
    python remote_control.py 192.168.1.100 download_picture "/storage/emulated/0/DCIM/Camera/IMG_20241215_143022_123.jpg"
    python remote_control.py 192.168.1.100 take_and_download
    python remote_control.py 192.168.1.100 get_property ro.product.model
    python remote_control.py 192.168.1.100 interactive

Note: Downloaded images are automatically saved to your desktop.
"""

import requests
import json
import sys
import time
import argparse
from typing import Dict, Any, Optional

class AndroidRemoteController:
    """
    Android Remote Controller - Sends commands to Android device
    """
    
    def __init__(self, device_ip: str, port: int = 8080, timeout: int = 30):
        """
        Initialize remote controller
        
        Args:
            device_ip: IP address of Android device
            port: Port number (default: 8080)
            timeout: Request timeout in seconds
        """
        self.device_ip = device_ip
        self.port = port
        self.timeout = timeout
        self.base_url = f"http://{device_ip}:{port}"
        
        # Default headers
        self.headers = {
            'Content-Type': 'application/json',
            'User-Agent': 'AndroidRemoteController/1.0'
        }
        
        print(f"🔗 Connected to device at {self.base_url}")
    
    def send_command(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """
        Send command to Android device
        
        Args:
            command: Command dictionary
            
        Returns:
            Response dictionary
        """
        try:
            print(f"📤 Sending command: {json.dumps(command)}")
            
            response = requests.post(
                self.base_url,
                json=command,
                headers=self.headers,
                timeout=self.timeout
            )
            
            response.raise_for_status()
            result = response.json()
            
            print(f"📥 Response: {json.dumps(result, indent=2)}")
            return result
            
        except requests.exceptions.ConnectionError:
            error_msg = f"❌ Connection failed. Is the device at {self.device_ip} reachable?"
            print(error_msg)
            return {"success": False, "error": "Connection failed", "message": error_msg}
            
        except requests.exceptions.Timeout:
            error_msg = f"⏱️ Request timed out after {self.timeout} seconds"
            print(error_msg)
            return {"success": False, "error": "Timeout", "message": error_msg}
            
        except requests.exceptions.HTTPError as e:
            error_msg = f"❌ HTTP Error: {e}"
            print(error_msg)
            return {"success": False, "error": "HTTP Error", "message": str(e)}
            
        except json.JSONDecodeError:
            error_msg = "❌ Invalid JSON response from device"
            print(error_msg)
            return {"success": False, "error": "Invalid JSON", "message": error_msg}
            
        except Exception as e:
            error_msg = f"❌ Unexpected error: {e}"
            print(error_msg)
            return {"success": False, "error": "Unexpected error", "message": str(e)}
    
    def get_server_status(self) -> Dict[str, Any]:
        """
        Get server status
        
        Returns:
            Status response dictionary
        """
        try:
            print("🔍 Checking server status...")
            response = requests.get(f"{self.base_url}/status", timeout=self.timeout)
            response.raise_for_status()
            result = response.json()
            
            if result.get("success", False):
                print("✅ Server is running")
            else:
                print("❌ Server status check failed")
                
            return result
            
        except Exception as e:
            print(f"❌ Status check failed: {e}")
            return {"success": False, "error": str(e)}
    
    def open_camera(self) -> Dict[str, Any]:
        """
        Open camera on device
        
        Returns:
            Response dictionary
        """
        print("📷 Opening camera...")
        command = {"action": "open_camera"}
        return self.send_command(command)
    
    def take_picture(self) -> Dict[str, Any]:
        """
        Take picture with rear camera
        
        Returns:
            Response dictionary with image path if successful
        """
        print("📸 Taking picture...")
        command = {"action": "take_picture"}
        result = self.send_command(command)
        
        if result.get("success", False) and "image_path" in result:
            print(f"📁 Image saved to: {result['image_path']}")
        
        return result
    
    def get_property(self, property_name: str) -> Dict[str, Any]:
        """
        Get device property using getprop
        
        Args:
            property_name: Property name to retrieve
            
        Returns:
            Response dictionary with property value
        """
        print(f"🔍 Getting property: {property_name}")
        command = {"action": "get_property", "property": property_name}
        result = self.send_command(command)
        
        if result.get("success", False) and "value" in result:
            print(f"📋 {property_name} = {result['value']}")
        
        return result
    
    def get_device_info(self) -> Dict[str, Any]:
        """
        Get common device properties
        
        Returns:
            Dictionary with device information
        """
        print("📱 Getting device information...")
        
        properties = [
            "ro.product.model",
            "ro.product.manufacturer",
            "ro.product.brand",
            "ro.build.version.release",
            "ro.build.version.sdk",
            "ro.build.id"
        ]
        
        device_info = {}
        for prop in properties:
            result = self.get_property(prop)
            if result.get("success", False):
                device_info[prop] = result.get("value", "unknown")
        
        return device_info
    
    def download_picture(self, image_path: str, save_path: Optional[str] = None) -> Dict[str, Any]:
        """
        Download a picture from the Android device to local machine
        
        Args:
            image_path: Path to the image on the Android device
            save_path: Local path to save the image (optional, will generate if not provided)
            
        Returns:
            Response dictionary with download status
        """
        print(f"📥 Downloading picture: {image_path}")
        
        try:
            # First, get download info
            command = {"action": "download_picture", "image_path": image_path}
            result = self.send_command(command)
            
            if not result.get("success", False):
                print(f"❌ Failed to prepare download: {result.get('message', 'Unknown error')}")
                return result
            
            download_url = result.get("download_url")
            file_name = result.get("file_name", "downloaded_image.jpg")
            file_size = result.get("file_size", 0)
            
            if not download_url:
                error_msg = "No download URL provided by server"
                print(f"❌ {error_msg}")
                return {"success": False, "error": error_msg}
            
            if file_size == 0:
                print("⚠️ Warning: File size is 0 - file might still be processing")
            
            # Generate local save path if not provided
            if save_path is None:
                import os
                # Save to desktop with timestamp
                import time
                timestamp = time.strftime("%Y%m%d_%H%M%S")
                desktop_path = os.path.expanduser("~/Desktop")
                save_path = os.path.join(desktop_path, f"downloaded_{timestamp}_{file_name}")
            
            # Download the file
            print(f"📁 Downloading {file_name} ({file_size} bytes) to {save_path}...")
            
            full_url = f"{self.base_url}{download_url}"
            print(f"🔗 Download URL: {full_url}")
            
            response = requests.get(full_url, timeout=self.timeout, stream=True)
            
            # Better error handling for HTTP errors
            if response.status_code != 200:
                try:
                    error_response = response.json()
                    error_msg = error_response.get('message', f'HTTP {response.status_code} error')
                    
                    # Display debug info if available
                    if 'debug_info' in error_response:
                        print(f"🐛 Debug Information:")
                        debug_info = error_response['debug_info']
                        for key, value in debug_info.items():
                            print(f"   {key}: {value}")
                        print()
                    
                except:
                    error_msg = f'HTTP {response.status_code}: {response.text[:200]}'
                print(f"❌ Server error: {error_msg}")
                return {"success": False, "error": "Server error", "message": error_msg}
            
            response.raise_for_status()
            
            # Check if response has content
            content_length = response.headers.get('content-length')
            if content_length and int(content_length) == 0:
                error_msg = "Server returned empty file"
                print(f"❌ {error_msg}")
                return {"success": False, "error": "Empty file", "message": error_msg}
            
            # Save file to local disk
            bytes_downloaded = 0
            with open(save_path, 'wb') as f:
                for chunk in response.iter_content(chunk_size=8192):
                    if chunk:
                        f.write(chunk)
                        bytes_downloaded += len(chunk)
            
            # Verify file size
            import os
            actual_size = os.path.getsize(save_path)
            if actual_size == 0:
                error_msg = "Downloaded file is empty"
                print(f"❌ {error_msg}")
                try:
                    os.remove(save_path)  # Clean up empty file
                except:
                    pass
                return {"success": False, "error": "Empty file", "message": error_msg}
            
            if actual_size != file_size and file_size > 0:
                print(f"⚠️ Warning: Downloaded file size ({actual_size} bytes) doesn't match expected size ({file_size} bytes)")
            
            print(f"✅ Download completed: {save_path} ({actual_size} bytes)")
            
            return {
                "success": True,
                "message": f"Image downloaded successfully to {save_path}",
                "local_path": save_path,
                "file_size": actual_size,
                "original_path": image_path
            }
            
        except requests.exceptions.RequestException as e:
            error_msg = f"Download failed: {e}"
            print(f"❌ {error_msg}")
            return {"success": False, "error": "Download error", "message": error_msg}
            
        except IOError as e:
            error_msg = f"Failed to save file: {e}"
            print(f"❌ {error_msg}")
            return {"success": False, "error": "File save error", "message": error_msg}
            
        except Exception as e:
            error_msg = f"Unexpected error during download: {e}"
            print(f"❌ {error_msg}")
            return {"success": False, "error": "Unexpected error", "message": error_msg}
    
    def take_and_download_picture(self, save_path: Optional[str] = None) -> Dict[str, Any]:
        """
        Take a picture and immediately download it
        
        Args:
            save_path: Local path to save the image (optional)
            
        Returns:
            Response dictionary with combined operation status
        """
        print("📸 Taking and downloading picture...")
        
        # Take picture first
        take_result = self.take_picture()
        if not take_result.get("success", False):
            return take_result
        
        image_path = take_result.get("image_path")
        if not image_path:
            error_msg = "No image path returned from take_picture"
            print(f"❌ {error_msg}")
            return {"success": False, "error": error_msg}
        
        # Download the picture
        download_result = self.download_picture(image_path, save_path)
        
        # Combine results
        combined_result = {
            "success": download_result.get("success", False),
            "take_picture": take_result,
            "download_picture": download_result
        }
        
        if combined_result["success"]:
            combined_result["message"] = f"Picture taken and downloaded successfully to {download_result.get('local_path')}"
            combined_result["local_path"] = download_result.get("local_path")
            combined_result["remote_path"] = image_path
        else:
            combined_result["message"] = "Failed to complete take and download operation"
        
        return combined_result
    
    def test_download_simple(self) -> Dict[str, Any]:
        """
        Test download with a simple file path for debugging
        
        Returns:
            Response dictionary with test results
        """
        print("🧪 Testing download with simple path...")
        
        # Use a simple test path
        test_path = "/storage/emulated/0/test.txt"
        command = {"action": "download_picture", "image_path": test_path}
        
        print(f"📤 Sending test command: {json.dumps(command)}")
        result = self.send_command(command)
        
        print(f"📥 Test result: {json.dumps(result, indent=2)}")
        return result
    
    def interactive_mode(self):
        """
        Enter interactive mode for sending commands
        """
        print("\n🎮 Interactive Mode - Type 'help' for available commands")
        print("Type 'exit' to quit\n")
        
        while True:
            try:
                user_input = input("📱 Command: ").strip()
                
                if user_input.lower() in ['exit', 'quit', 'q']:
                    print("👋 Goodbye!")
                    break
                
                elif user_input.lower() == 'help':
                    self.show_help()
                
                elif user_input.lower() == 'status':
                    self.get_server_status()
                
                elif user_input.lower() == 'open_camera':
                    self.open_camera()
                
                elif user_input.lower() == 'take_picture':
                    self.take_picture()
                
                elif user_input.lower() == 'device_info':
                    info = self.get_device_info()
                    print(json.dumps(info, indent=2))
                
                elif user_input.lower().startswith('download_picture '):
                    image_path = user_input[17:].strip()
                    if image_path:
                        if image_path.startswith('"') and image_path.endswith('"'):
                            image_path = image_path[1:-1]  # Remove quotes
                        self.download_picture(image_path)
                    else:
                        print("❌ Please specify image path")
                        print("💡 Usage: download_picture /path/to/image.jpg")
                
                elif user_input.lower() == 'take_and_download':
                    self.take_and_download_picture()
                
                elif user_input.lower() == 'test_download':
                    self.test_download_simple()
                
                elif user_input.lower().startswith('get_property '):
                    prop_name = user_input[13:].strip()
                    if prop_name:
                        self.get_property(prop_name)
                    else:
                        print("❌ Please specify property name")
                
                elif user_input.lower().startswith('getprop '):
                    prop_name = user_input[8:].strip()
                    if prop_name:
                        self.get_property(prop_name)
                    else:
                        print("❌ Please specify property name")
                
                else:
                    print("❌ Unknown command. Type 'help' for available commands")
                
                print()  # Add blank line for readability
                
            except KeyboardInterrupt:
                print("\n👋 Goodbye!")
                break
            except Exception as e:
                print(f"❌ Error: {e}")
    
    def show_help(self):
        """
        Show available commands
        """
        help_text = """
📖 Available Commands:
    
    status          - Check server status
    open_camera     - Open camera on device
    take_picture    - Take picture with rear camera
    download_picture <path> - Download image from device (use path from take_picture)
    take_and_download - Take picture and download it immediately
    test_download   - Test download functionality with debugging
    get_property <name> - Get device property (e.g., ro.product.model)
    getprop <name>  - Same as get_property
    device_info     - Get common device properties
    help            - Show this help message
    exit/quit/q     - Exit interactive mode
    
💡 Examples:
    take_picture
    download_picture "/storage/emulated/0/DCIM/Camera/IMG_20241215_143022_123.jpg"
    take_and_download
    get_property ro.product.model
    get_property ro.build.version.release
    getprop ro.product.manufacturer

📁 Note: Downloaded images are automatically saved to your desktop with timestamps.
"""
        print(help_text)


def main():
    """
    Main function - Parse arguments and execute commands
    """
    parser = argparse.ArgumentParser(
        description="Remote Control for Android Device",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
    python remote_control.py 192.168.1.100 open_camera
    python remote_control.py 192.168.1.100 take_picture
    python remote_control.py 192.168.1.100 get_property ro.product.model
    python remote_control.py 192.168.1.100 interactive
    """
    )
    
    parser.add_argument("device_ip", help="IP address of Android device")
    parser.add_argument("command", nargs='?', help="Command to execute")
    parser.add_argument("args", nargs='*', help="Command arguments")
    parser.add_argument("--port", type=int, default=8080, help="Port number (default: 8080)")
    parser.add_argument("--timeout", type=int, default=30, help="Request timeout in seconds")
    
    args = parser.parse_args()
    
    # Initialize controller
    controller = AndroidRemoteController(args.device_ip, args.port, args.timeout)
    
    # Execute command
    if not args.command:
        print("❌ No command specified. Use 'interactive' for interactive mode.")
        print("Use --help for usage information.")
        return
    
    command = args.command.lower()
    
    if command == "interactive":
        controller.interactive_mode()
    
    elif command == "status":
        controller.get_server_status()
    
    elif command == "open_camera":
        result = controller.open_camera()
        if result.get("success", False):
            print("✅ Camera opened successfully")
        else:
            print("❌ Failed to open camera")
    
    elif command == "take_picture":
        result = controller.take_picture()
        if result.get("success", False):
            print("✅ Picture taken successfully")
        else:
            print("❌ Failed to take picture")
    
    elif command == "get_property":
        if args.args:
            prop_name = args.args[0]
            result = controller.get_property(prop_name)
            if result.get("success", False):
                print(f"✅ {prop_name} = {result.get('value', 'unknown')}")
            else:
                print(f"❌ Failed to get property: {prop_name}")
        else:
            print("❌ Please specify property name")
    
    elif command == "device_info":
        info = controller.get_device_info()
        print("\n📱 Device Information:")
        print(json.dumps(info, indent=2))
    
    elif command == "download_picture":
        if args.args:
            image_path = args.args[0]
            save_path = args.args[1] if len(args.args) > 1 else None
            result = controller.download_picture(image_path, save_path)
            if result.get("success", False):
                print(f"✅ Image downloaded successfully to: {result.get('local_path')}")
            else:
                print(f"❌ Failed to download image: {result.get('message', 'Unknown error')}")
        else:
            print("❌ Please specify image path")
            print("💡 Usage: python remote_control.py <device_ip> download_picture \"/path/to/image.jpg\" [local_save_path]")
    
    elif command == "take_and_download":
        save_path = args.args[0] if args.args else None
        result = controller.take_and_download_picture(save_path)
        if result.get("success", False):
            print(f"✅ Picture taken and downloaded successfully to: {result.get('local_path')}")
            print(f"📁 Remote path: {result.get('remote_path')}")
        else:
            print("❌ Failed to take and download picture")
    
    else:
        print(f"❌ Unknown command: {command}")
        print("Available commands: interactive, status, open_camera, take_picture, download_picture, take_and_download, get_property, device_info")


if __name__ == "__main__":
    main() 