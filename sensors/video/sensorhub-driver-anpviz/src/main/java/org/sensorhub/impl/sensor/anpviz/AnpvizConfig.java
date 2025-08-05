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

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.PositionConfig.EulerOrientation;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.comm.HTTPConfig;
import org.sensorhub.impl.sensor.ffmpeg.config.Connection;
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

	@DisplayInfo(label = "FFmpeg Connection", desc = "FFmpeg configuration for audio/visual streaming")
	public Connection ffmpegConnection = new Connection();

	@DisplayInfo(desc = "Camera geographic position")
	public PositionConfig position = new PositionConfig();

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