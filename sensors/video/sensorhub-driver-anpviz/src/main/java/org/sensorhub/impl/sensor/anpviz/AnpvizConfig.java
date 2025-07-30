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

package org.sensorhub.impl.sensor.anpviz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.PositionConfig.EulerOrientation;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.comm.HTTPConfig;
import org.sensorhub.impl.comm.RobustIPConnectionConfig;
import org.sensorhub.impl.sensor.anpviz.ptz.AnpvizPTZconfig;
import org.sensorhub.impl.sensor.anpviz.ptz.AnpvizPTZpreset;
import org.sensorhub.impl.sensor.anpviz.ptz.AnpvizPTZrelMove;
import org.sensorhub.impl.sensor.rtpcam.RTSPConfig;
import org.sensorhub.impl.sensor.videocam.BasicVideoConfig;
import org.sensorhub.impl.sensor.videocam.VideoResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Configuration parameters for Anpviz camera
 * </p>
 * 
 * @author Lee Butler
 * @since September 2016
 */
public class AnpvizConfig extends SensorConfig {
	private static final Logger logger = LoggerFactory.getLogger(org.sensorhub.impl.sensor.anpviz.AnpvizConfig.class);

	@Required
	@DisplayInfo(label = "Camera ID", desc = "Camera ID to be appended to UID prefix")
	public String cameraID;

	@DisplayInfo(label = "HTTP", desc = "HTTP configuration")
	public HTTPConfig http = new HTTPConfig();

	@DisplayInfo(label = "RTP/RTSP", desc = "RTP/RTSP configuration (Remote host is obtained from HTTP configuration)")
	public RTSPConfig rtsp = new RTSPConfig();

	@DisplayInfo(label = "Connection Options")
	public RobustIPConnectionConfig connection = new RobustIPConnectionConfig();


	@DisplayInfo(desc = "Camera geographic position")
	public PositionConfig position = new PositionConfig();

	@DisplayInfo(label = "PTZ", desc = "Pan-Tilt-Zoom configuration")
	public AnpvizPTZconfig ptz = new AnpvizPTZconfig();

	public int ptzSpeedVal = 2; // Set PTZ Speed (integer value in range 0-4,
								// 0=fast, 4=slow)


	/********************************************************************/

	@Override
	public LLALocation getLocation() {
		return position.location;
	}

	@Override
	public EulerOrientation getOrientation() {
		return position.orientation;
	}
}