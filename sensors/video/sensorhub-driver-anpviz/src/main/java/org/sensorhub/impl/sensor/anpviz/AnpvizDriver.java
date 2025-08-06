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

import java.io.IOException;
import java.net.HttpURLConnection;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.comm.RobustIPConnection;
import org.sensorhub.impl.security.ClientAuth;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;


/**
 * <p>
 * Implementation of sensor interface for Anpviz Cameras using IP protocol Based
 * on Anpviz v1.0.4 API.
 * </p>
 *
 * @author Lee Butler
 * @since September 2016
 */
public class AnpvizDriver extends AbstractSensorModule<AnpvizConfig> {

	private static final Logger logger = LoggerFactory.getLogger(AnpvizDriver.class);
	private AnpvizPtzControl ptzControlInterface;
	RobustIPConnection connection;
	AnpvizVideoOutput videoDataInterface;

	AnpvizDevice device;
	boolean ptzSupported = false;
	String serialNumber;
	String modelNumber;
	String hostUrl;

	protected ScheduledExecutorService executor;

	public AnpvizDriver() {
	}	

	@Override
    protected void doInit() throws SensorHubException {
		// reset internal state in case init() was already called
		super.doInit();
		videoDataInterface = null;
		ptzControlInterface = null;
		ptzSupported = false;

		try {
			device = new AnpvizDevice(config.http.remoteHost, config.http.remotePort,
					config.http.user, config.http.password);
		} catch (Exception e) {
			reportError("Could not connect to Anpviz camera.", e);
		}

		try {
			createVideoInterface();
		} catch (Exception e) {
			logger.error(e.getMessage());
			videoDataInterface = null;
		}

		try {
			createPtzInterface();
		}  catch (Exception e) {
			logger.error(e.getMessage());
			ptzSupported = false;
			ptzControlInterface = null;
		}

		/*
		// PTZ control and status
		try {

		} catch (IOException e) {
			logger.error("Cannot create ptz interfaces", e);
			throw new SensorException("Cannot create ptz interfaces", e);
		}

		 */
	}

	@Override
	protected synchronized void doStart() throws SensorHubException {
		super.doStart();
		if (videoDataInterface != null) {
			videoDataInterface.start();
		}
		// wait for valid connection to camera
		//connection.waitForConnection();

	}

	@Override
	protected synchronized void doStop() throws SensorHubException {
		super.doStop();
		if (videoDataInterface != null) {
			videoDataInterface.stop();
		}

	}

	@Override
	protected void updateSensorDescription() {
		synchronized (sensorDescLock) {
			super.updateSensorDescription();
			if (!sensorDescription.isSetDescription())
				sensorDescription.setDescription("Anpviz Network Camera");
		}
	}

	@Override
	public void setConfiguration(AnpvizConfig config) {
		super.setConfiguration(config);

	};

	@Override
	public boolean isConnected() {
		return device != null;
	}

	protected void createVideoInterface() throws SensorHubException {
		if (videoDataInterface != null) {
			videoDataInterface.stop();
			videoDataInterface = null;
		}

		if (config.ffmpegConnection.connectionString == null || config.ffmpegConnection.connectionString.isBlank()) {
			try {
				config.ffmpegConnection.connectionString = device.getMediaUrl(); // TODO Not sure if this URL is correct
			} catch (Exception e) {
				logger.warn("Could not find A/V media URL.", e);
			}
		}

		try {
			this.videoDataInterface = new AnpvizVideoOutput(config.ffmpegConnection);
			this.videoDataInterface.init();
			addOutput(this.videoDataInterface.getVideoDataInterface(), false);
		} catch (Exception e) {
			videoDataInterface = null;
			logger.warn("Could not connect to A/V stream.", e);
		}
	}

	protected void createPtzInterface() throws SensorException, IOException {

		// add PTZ controller
		this.ptzControlInterface = new AnpvizPtzControl(this);
		addControlInput(ptzControlInterface);
		ptzControlInterface.init();
	}

	@Override
	public void cleanup() {
	}

	private void setAuth() {
		ClientAuth.getInstance().setUser(config.http.user);
		if (config.http.password != null)
			ClientAuth.getInstance().setPassword(config.http.password.toCharArray());
	}

	protected String getHostUrl() {
		setAuth();
		return hostUrl;
	}

}
