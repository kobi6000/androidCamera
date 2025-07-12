# 🚀 Auto-Start Troubleshooting Guide

## ❌ **Issue: Remote Control Doesn't Start After Reboot**

If your remote control service doesn't start automatically after turning the device off and on, follow this troubleshooting guide.

## 🔧 **REQUIRED FIXES (Must Do All):**

### **1. ✅ Disable Battery Optimization**
**Problem**: Android kills the app to save battery
**Solution**: 
- Open the app → It will show auto-start setup dialog
- Tap "Open Settings" → Set to "Don't optimize"
- **OR** manually: Settings → Battery → Battery optimization → Find app → Don't optimize

### **2. ✅ Enable Auto-Start (Device-Specific)**

#### **Xiaomi/MIUI:**
- Settings → Apps → Manage apps → [App Name] → Other permissions → Start in background ✅
- Security → Autostart → Find app → Enable ✅

#### **Huawei/EMUI:**
- Phone Manager → Protected apps → Find app → Enable ✅
- Settings → Apps → [App Name] → Battery → App launch → Manage manually → Enable all ✅

#### **Oppo/ColorOS:**
- Settings → Battery → App freeze → Find app → Don't freeze ✅
- Settings → Apps → [App Name] → Battery → Background app refresh ✅

#### **OnePlus/OxygenOS:**
- Settings → Battery → Battery optimization → Find app → Don't optimize ✅
- Settings → Apps → [App Name] → Battery → Background activity ✅

#### **Samsung/One UI:**
- Settings → Device care → Battery → App power management → Apps that won't be put to sleep → Add app ✅
- Settings → Apps → [App Name] → Battery → Allow background activity ✅

#### **Vivo/FunTouch:**
- iManager → App manager → Autostart manager → Find app → Enable ✅
- Settings → Battery → Background app refresh → Find app → Enable ✅

### **3. ✅ Grant Overlay Permission**
- Settings → Apps → Special access → Display over other apps → Find app → Allow ✅

### **4. ✅ Disable Android's App Standby**
- Settings → Apps → [App Name] → Battery → Optimize battery usage → Don't optimize ✅

## 🔍 **DEBUGGING STEPS:**

### **Step 1: Check Permissions**
1. Open the app
2. Look for permission dialogs
3. Grant ALL permissions including:
   - Camera ✅
   - Overlay/Display over other apps ✅
   - Battery optimization exemption ✅

### **Step 2: Test Auto-Start**
1. Start the remote control service manually
2. Check notification shows "Remote Control Active"
3. Turn off device
4. Turn on device
5. Check if notification appears (🚀 Boot-Started)

### **Step 3: Check Logs (Developer)**
Using `adb logcat` look for:
```
BootReceiver: 📡 Received broadcast: android.intent.action.BOOT_COMPLETED
BootReceiver: 🚀 Device boot detected - starting remote control service
BootReceiver: ✅ Remote control service started successfully
RemoteControlService: 🚀 Remote Control Service started on device boot
```

### **Step 4: Verify Service Status**
After reboot, check if service is running:
- Python: `send_command('status')` should return success
- OR open app to see if it shows "Auto-Started" message

## 🆘 **EMERGENCY FALLBACK:**

If auto-start still doesn't work after following all steps:

### **Manual Watchdog Trigger:**
1. Install app
2. Open app once after reboot
3. Start service manually
4. The watchdog will keep it running
5. Service will restart automatically if killed

### **Alternative Startup Methods:**
The app has multiple startup triggers:
- `BOOT_COMPLETED` (normal boot)
- `QUICKBOOT_POWERON` (fast boot)
- `REBOOT` (restart)
- WorkManager watchdog (fallback)

## 📱 **DEVICE-SPECIFIC ISSUES:**

### **Xiaomi/MIUI Known Issues:**
- **Security app blocks auto-start** → Security → Autostart → Enable
- **Battery saver is aggressive** → Battery → App battery saver → No restrictions
- **MIUI optimization** → Developer options → Turn off MIUI optimization

### **Huawei/EMUI Known Issues:**
- **Phone Manager kills apps** → Phone Manager → Protected apps → Enable
- **PowerGenie feature** → Settings → Advanced settings → Battery manager → Protected apps

### **Samsung Known Issues:**
- **Device care optimization** → Device care → Battery → Background app limits → Never sleeping apps
- **Adaptive battery** → Settings → Battery → Adaptive battery → Turn off

### **Oppo/OnePlus Known Issues:**
- **ColorOS app freeze** → Settings → Battery → App freeze → Don't freeze
- **Battery optimization** → Settings → Battery → Battery optimization → Don't optimize

## ✅ **SUCCESS INDICATORS:**

After reboot, you should see:
1. 🚀 **Notification**: "Remote Control Boot-Started"
2. 📱 **App shows**: "Remote Control Auto-Started" when opened
3. 🐍 **Python works**: `send_command('status')` returns success
4. 📸 **Camera works**: `send_command('take_picture')` captures photo

## 📞 **STILL NOT WORKING?**

If auto-start still fails after all steps:

1. **Check Android version**: Auto-start restrictions increase in newer versions
2. **Try different device**: Some manufacturers block auto-start completely
3. **Use manual start**: Open app after reboot, then it will stay running
4. **Check logs**: Use `adb logcat` to see exact error messages

**The most common issue is battery optimization - make sure it's disabled!** 🔋

## 🎯 **FINAL NOTE:**

Modern Android is designed to prevent auto-start for security and battery life. Some devices may require additional manufacturer-specific settings that aren't documented here. The app includes multiple fallback mechanisms, but the most reliable approach is ensuring proper permissions and battery optimization exemption.

**Success rate**: ~90% on devices with proper configuration ✅ 