/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc.. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.onvif;

import org.sensorhub.api.comm.ICommConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.SensorConfig;
import de.onvif.discovery.OnvifDiscovery;
import org.sensorhub.impl.comm.TCPConfig;
import org.sensorhub.impl.sensor.rtpcam.RTPCameraConfig;

import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.sensorhub.impl.sensor.ffmpeg.config.*;

/**
 * <p>
 * Implementation of ONVIF interface for generic cameras using SOAP ONVIF
 * protocol. This particular class stores configuration parameters.
 * </p>
 * 
 * @author Joshua Wolfe <developer.wolfe@gmail.com>
 * @since May 22, 2017
 */
public class OnvifCameraConfig extends SensorConfig {


    @Required
    @DisplayInfo(label="ONVIF Connection Options", desc="Configure ONVIF remote address and port")
    public OnvifConfig networkConfig = new OnvifConfig();

    @Required
    @DisplayInfo(label="Prefer MJPEG", desc="Choose MJPEG when available")
    public boolean preferMjpeg = false;

    public class OnvifConfig extends TCPConfig implements ICommConfig{
        public OnvifConfig() {
            this.remotePort = 80;
            this.user = "";
            this.password = "";
        }

        @DisplayInfo(label = "ONVIF Path", desc="Path for ONVIF device services.")
        public String onvifPath = "/onvif/device_service";
    }

    /*
	@Required
    @DisplayInfo(label="Host IP Address", desc="IP Address of the camera")
    public String hostIp = "";

    @DisplayInfo(label="Host Port", desc="ONVIF port on the camera")
    public Integer hostPort = 80;

	@DisplayInfo(label="Local UDP Streaming Port", desc="Local port for incoming camera video stream")
	public Integer localUdpPort = 56000;
	
    @DisplayInfo(label="User Login", desc="User that will be logged into for issuing PTZ commands")
    public String user = "";

    @DisplayInfo(label="Password", desc="Password used to login to user")
    public String password = "";
	
	@DisplayInfo(label="Path", desc="ONVIF route of the camera")
	public String deviceService = "/onvif/device_service";
	
	@DisplayInfo(label="Timeout(ms)", desc="Timeout of connection to the camera")
	public Integer timeout = 31000;*/



}
