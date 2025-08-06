# Anpviz U-Series PTZ (Pan-Tilt-Zoom) Video Camera

OSH sensor adaptor supporting output (video and PTZ settings) and tasking (camera and PTZ) for Anpviz U-Series cameras.

This driver depends on the following modules at runtime:
  * sensorhub-driver-ffmpeg
  * sensorhub-driver-videocam

---

## Config
<br>

### General
* **Camera ID**: Unique ID for this camera.

### HTTP
* **Remote Host** (Required): IP address of the camera. This should not contain the protocol, port, or credentials.
  * Example: `192.168.1.105`
* **Local Address**: Leave this set to `AUTO`.
* **User Name & Password** (Required): User name and password account credentials associated with this camera.
  * An account must be created on the camera before using this driver.
* **Resource Path**: Leave this blank.
* **Remote Port**: The web port associated with this camera.
  * Default: `80`

### FFmpeg Connection
* **Connection String**: The URL for the video server. If blank, the URL will be discovered automatically.
* **FPS**: No effect unless streaming from a file.
* **Loop**: No effect unless streaming from a file.
* **Inject Extradata for Streaming**: Leave enabled. Injects SPS and PPS header data before every keyframe video packet in case the camera is not already doing so.

---

## Control
<br>

### Presets
* **Preset Add**: Add a preset for the current PTZ position.
  * Preset name must be an integer `1` to `255` (inclusive).
* **Preset Remove**: Remove a preset.
  * If an added preset does not appear in the dropdown, refresh the page.
* **Go To Preset**: Select a preset and go to its saved PTZ position.
  * If an added preset does not appear in the dropdown, refresh the page.

### PTZ Movement
* **Pan/Tilt/Zoom**: Set to an integer `-10` to `10` (inclusive). 
  * Positive integers pan right, tilt up, or zoom in,
  while negative integers pan left, tilt down, or zoom out. 
  * The value's magnitude determines the respective pan/tilt speed (zoom speed is not variable). 
  * PTZ movement is continuous and only stops when 0 is entered for all fields.