/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.onvif;


import java.net.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

//import javax.xml.soap.SOAPException;
import jakarta.xml.ws.http.HTTPException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.onvif.ver10.schema.*;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.ffmpeg.outputs.AudioOutput;
import org.sensorhub.impl.sensor.ffmpeg.outputs.VideoOutput;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.onvif.soap.OnvifDevice;

/**
 * <p>
 * Implementation of sensor interface for generic Cameras using IP
 * protocol
 * </p>
 * 
 * @author Joshua Wolfe <developer.wolfe@gmail.com>
 * @since June 13, 2017
 */
public class OnvifCameraDriver extends AbstractSensorModule<OnvifCameraConfig>
{
    private static final Logger log = LoggerFactory.getLogger(OnvifCameraDriver.class);

    //RTPVideoOutput <OnvifCameraDriver> h264VideoOutput;
    //OnvifVideoOutput mpeg4VideoOutput;
    OnvifVideoControl videoControlInterface;
    VideoOutput<OnvifCameraDriver> videoOutput;
    AudioOutput<OnvifCameraDriver> audioOutput;
    MpegTsProcessor mpegTsProcessor;
    protected ScheduledExecutorService executor;
    OnvifPtzOutput ptzPosOutput;
    OnvifPtzControl ptzControlInterface;

    String hostIp;
    Integer hostPort;
    String user;
    String password;
    String onvifPath;
    boolean preferMjpeg;
    String mediaServiceAddress;

    OnvifDevice camera;
    Profile ptzProfile;
    Profile streamingProfile;

    String serialNumber;
    String modelNumber;
    String shortName;
    String longName;
    URI streamURI;
    String visualConnectionString;

    Thread discoveryThread;
    OnvifCameraConfig config;

    public OnvifCameraDriver() throws SensorHubException {
        super();
        //onvifNetwork = new OnvifNetwork();
        //onvifNetwork.start();
    }


    @Override
    public void setConfiguration(final OnvifCameraConfig config) {
        super.setConfiguration(config);

        hostIp = config.networkConfig.remoteHost;
        hostPort = config.networkConfig.remotePort;
        user = (config.networkConfig.user == null) ? "" : config.networkConfig.user;
        password = (config.networkConfig.password == null) ? "" : config.networkConfig.password;
        onvifPath = (config.networkConfig.onvifPath == null) ? "" : config.networkConfig.onvifPath;
        preferMjpeg = config.preferMjpeg;
    };

    @Override
    public boolean isConnected() {
        return camera != null;
    }

    @Override
    protected void doInit() throws SensorHubException {

        if(ptzPosOutput != null) {
            ptzPosOutput.stop();
            ptzPosOutput = null;
        }

        if (mpegTsProcessor != null) {
            stopStream();
            shutdownExecutor();
            mpegTsProcessor = null;
        }
        streamingProfile = null;
        videoOutput = null;
        audioOutput = null;

        if (hostIp == null) {
            throw new SensorHubException("No host IP address provided in config");
        }

        try {
            String resolvePort = (hostPort == 0) ? "" : ":" + hostPort.toString();
            String resolvePath = (onvifPath == null) ? "" : onvifPath;
            camera = new OnvifDevice(hostIp + resolvePort, user, password, resolvePath);

        } catch (ConnectException e) {
            throw new SensorHubException("Exception occured when connecting to camera");
        } catch (HTTPException e) {
            throw new SensorHubException(e.toString());
        } catch (Exception e) {
            throw new SensorHubException(e.toString());
        }

        List<Profile> profiles = camera.getMedia().getProfiles();

        if (profiles == null || profiles.isEmpty()) {
            throw new SensorHubException("Camera does not have any profiles to use");
        }

        log.debug("I AM HERE 129");
        Profile tempMedia = null;
        for (Profile p: profiles) {
            // Select a profile that supports ptz
            if(ptzProfile == null && p.getPTZConfiguration() != null){
                ptzProfile = p;
                log.debug(p.getPTZConfiguration().toString());
            }
            // Select a profile that supports video
            if (streamingProfile == null && p.getVideoEncoderConfiguration() != null) {
                tempMedia = p;
                // If MJPEG is preferred and supported, select it
                if (preferMjpeg && p.getVideoEncoderConfiguration().getEncoding() != null && camera.getMedia().getVideoEncoderConfigurationOptions(null, p.getToken()).getJPEG() != null) {
                    streamingProfile = p;
                    streamingProfile.getVideoEncoderConfiguration().setEncoding(VideoEncoding.JPEG);
                    log.debug("Successfully switched to JPEG: {}", streamingProfile.getVideoEncoderConfiguration().getEncoding().toString());
                }
            }
            if (ptzProfile != null && streamingProfile != null)
                break;
            /*
            if (camera.getPtz().isAbsoluteMoveSupported(token) &&
                    camera.getPtz().isRelativeMoveSupported(token) &&
                    camera.getPtz().isPtzOperationsSupported(token)) {
                profile = p;
                break;
            }*/
        }

        // If MJPEG is preferred but not found, fall back to other discovered video profile
        if (streamingProfile == null && tempMedia != null)
            streamingProfile = tempMedia;


        log.debug(camera.getStreamUri());

        //camera.getDevice().setDiscoveryMode();
        log.debug("I AM HERE 149");
        if (ptzProfile == null) {
            log.trace("Camera has no profiles capable of PTZ");
            log.debug("Camera has no profiles capable of PTZ");
        }
        if (streamingProfile == null) {
            log.trace("Camera has no profiles capable of video streaming");
            log.debug("Camera has no profiles capable of video streaming");
        }

        serialNumber = camera.getDeviceInfo().getSerialNumber().trim();
        modelNumber = camera.getDeviceInfo().getModel().trim();
        shortName = camera.getDeviceInfo().getManufacturer().trim();
        longName = shortName + "_" + modelNumber + "_" + serialNumber;
        log.debug("I AM HERE 158");
        // generate identifiers
        generateUniqueID("urn:onvif:cam:", serialNumber);
        generateXmlID("ONVIF_CAM_", serialNumber);

        log.debug("I AM HERE 194");

        if (ptzProfile != null) {
            // add PTZ output
            if (ptzPosOutput == null) {
                ptzPosOutput = new OnvifPtzOutput(this);
                addOutput(ptzPosOutput, false);
                ptzPosOutput.init();
            }
            log.debug("I AM HERE 199");
            // add PTZ controller
            if (ptzControlInterface == null) {
                ptzControlInterface = new OnvifPtzControl(this);
                addControlInput(ptzControlInterface);

            }
            ptzControlInterface.init();
        }

        if (streamingProfile != null) {
            if (mpegTsProcessor != null) {
                mpegTsProcessor.closeStream();
            }
            mpegTsProcessor = null;
            videoOutput = null;
            audioOutput = null;

            streamURI = URI.create(camera.getStreamUri(streamingProfile.getToken()));

            if (user != null && password != null)
                visualConnectionString = "rtsp://" + user + ":" + password + "@" + streamURI.getHost() + ":" + streamURI.getPort() + streamURI.getPath();
            else
                visualConnectionString = streamURI.toString();

            setupStream();
        }
    }
    // TODO: Restarting causes error computing hashcode, invalid data component. Figure out why.
    @Override
    public void doStart() throws SensorHubException {
        super.doStart();

        if (ptzProfile != null && ptzPosOutput != null) {
            ptzPosOutput.start();
            if (ptzControlInterface != null) {
                ptzControlInterface.start();
            }
        }
        if (streamingProfile != null && mpegTsProcessor != null && videoOutput != null && audioOutput != null) {
            setupStream();
            startStream();
        }
    }

    @Override
    public void doStop() throws SensorHubException {
        //super.doStop();
        if(ptzPosOutput != null) {
            ptzPosOutput.stop();
        }
        if (ptzControlInterface != null) {
            ptzControlInterface.stop();
        }
        if (mpegTsProcessor != null) {
            stopStream();
            shutdownExecutor();
        }

    }

    protected void setupStream() throws SensorHubException {
        setupExecutor();
        openStreamVisual();
    }

    protected void setupExecutor() {
        if (executor == null)
            executor = Executors.newSingleThreadScheduledExecutor();

        if (videoOutput != null)
            videoOutput.setExecutor(executor);
        if (audioOutput != null)
            audioOutput.setExecutor(executor);

    }

    protected void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    protected void openStreamVisual() throws SensorHubException{
    if (mpegTsProcessor == null) {
        mpegTsProcessor = new MpegTsProcessor(visualConnectionString);

        // Initialize the MPEG transport stream processor from the source named in the configuration.
        if (mpegTsProcessor.openStream()) {
            // If there is a video content in the stream
            if (mpegTsProcessor.hasVideoStream()) {
                // In case we were waiting until we got video data to make the video frame output,
                // we go ahead and do that now.
                if (videoOutput == null) {
                    videoOutput = new VideoOutput<>(this, mpegTsProcessor.getVideoStreamFrameDimensions(), mpegTsProcessor.getVideoCodecName(), "visual", "Visual Camera", "Visual stream using ffmpeg library");
                    if (executor != null) {
                        videoOutput.setExecutor(executor);
                    }
                    addOutput(videoOutput, false);
                    videoOutput.doInit();
                }
                // Set video stream packet listener to video output
                mpegTsProcessor.setVideoDataBufferListener(videoOutput);
            }

            // If there is an audio content in the stream
            if (mpegTsProcessor.hasAudioStream()) {
                // In case we were waiting until we got audio data to make the audio output,
                // we go ahead and do that now.
                if (audioOutput == null) {
                    audioOutput = new AudioOutput<>(this, mpegTsProcessor.getAudioSampleRate(), mpegTsProcessor.getAudioCodecName());
                    if (executor != null) {
                        audioOutput.setExecutor(executor);
                    }
                    addOutput(audioOutput, false);
                    audioOutput.doInit();
                }
                // Set audio stream packet listener to audio output
                mpegTsProcessor.setAudioDataBufferListener(audioOutput);
            }
        } else {
            throw new SensorHubException("Unable to open stream from data source");
        }
    }
}

    protected void startStream() throws SensorHubException {
        try {
            if (mpegTsProcessor != null) {
                mpegTsProcessor.processStream();
                mpegTsProcessor.setReconnect(true);
            }
        } catch (IllegalStateException e) {
            String message = "Failed to start stream processor";
            log.error(message);
            throw new SensorHubException(message, e);
        }
    }

    protected void stopStream() throws SensorHubException {

        log.info("Stopping MPEG TS processor for {}", getUniqueIdentifier());

        if (mpegTsProcessor != null) {
            mpegTsProcessor.stopProcessingStream();

            try {
                // Wait for thread to finish
                log.info("Waiting for stream processor to stop");
                mpegTsProcessor.join(1000);
            } catch (InterruptedException e) {
                log.error("Interrupted waiting for stream processor to stop", e);
                Thread.currentThread().interrupt();
                throw new SensorHubException("Interrupted waiting for stream processor to stop", e);
            } finally {
                // Close stream and cleanup resources
                mpegTsProcessor.closeStream();
                mpegTsProcessor = null;
            }
        }
    }

}
