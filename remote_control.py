#!/usr/bin/env python3
"""
Remote Control Application for Android Device
Sends commands to Android device over WiFi network

Usage:
    python remote_control.py <device_ip> <command> [options]
    
Commands:
    - open_camera: Open camera on device
    - take_picture: Take picture with rear camera
    - get_property <property_name>: Get device property
    - status: Get server status
    - interactive: Enter interactive mode
    
Examples:
    python remote_control.py 192.168.1.100 open_camera
    python remote_control.py 192.168.1.100 take_picture
    python remote_control.py 192.168.1.100 get_property ro.product.model
    python remote_control.py 192.168.1.100 interactive
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
    get_property <name> - Get device property (e.g., ro.product.model)
    getprop <name>  - Same as get_property
    device_info     - Get common device properties
    help            - Show this help message
    exit/quit/q     - Exit interactive mode
    
💡 Examples:
    get_property ro.product.model
    get_property ro.build.version.release
    getprop ro.product.manufacturer
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
    
    else:
        print(f"❌ Unknown command: {command}")
        print("Available commands: interactive, status, open_camera, take_picture, get_property, device_info")


if __name__ == "__main__":
    main() 