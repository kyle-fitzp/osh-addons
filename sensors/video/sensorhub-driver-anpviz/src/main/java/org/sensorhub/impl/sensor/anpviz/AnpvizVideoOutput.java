/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2016 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.anpviz;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import net.opengis.sensorml.v20.DataInterface;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.sensor.ffmpeg.config.Connection;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.impl.sensor.rtpcam.RTPVideoOutput;
import org.sensorhub.impl.sensor.videocam.VideoResolution;


/**
 * <p>
 * Implementation of video output interface for Anpviz cameras using
 * RTSP/RTP protocol
 * </p>
 *
 * @author Mike Botts
 * @since March 2017
 */
public class AnpvizVideoOutput extends FFMPEGSensor
{

    public AnpvizVideoOutput(Connection config) {
        super();
        this.config = new FFMPEGConfig();
        this.config.connection = config;
    }

    public IStreamingDataInterface getVideoDataInterface() {
        return this.videoOutput;
    }

    public IStreamingDataInterface getAudioDataInterface() {
        return this.audioOutput;
    }

}
