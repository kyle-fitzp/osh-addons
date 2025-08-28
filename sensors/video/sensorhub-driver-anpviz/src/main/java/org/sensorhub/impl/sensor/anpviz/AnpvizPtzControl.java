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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;

import net.opengis.swe.v20.*;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.anpviz.ptz.AnpvizPtzTuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.AllowedTokensImpl;
import org.vast.data.CategoryImpl;
import org.vast.data.DataChoiceImpl;

import org.vast.swe.SWEHelper;

/**
 * <p>
 * Implementation of sensor interface for Anpviz Cameras using IP protocol. This
 * particular class provides control of the Pan-Tilt-Zoom (PTZ) capabilities.
 * </p>
 *
 * @author Lee Butler
 * @since September 2016
 */
public class AnpvizPtzControl extends AbstractSensorControl<AnpvizDriver> {
	private static final Logger logger = LoggerFactory.getLogger(AnpvizPtzControl.class);

	DataRecord commandData;
	AnpvizDriver DriverDataInterface;
	//AnpvizPTZpresetsHandler presetsHandler;
	//AnpvizPTZrelMoveHandler relMoveHandler;
	SWEHelper helper = new SWEHelper();

	protected AnpvizPtzControl(AnpvizDriver driver) {
		super("ptzControl", driver);
	}

	protected void init() throws SensorException {
		logger.debug("Initializing PTZ control");
		AnpvizConfig config = parentSensor.getConfiguration();
		//relMoveHandler = new AnpvizPTZrelMoveHandler(config.ptz);

		// build SWE data structure for the tasking parameters
		AnpvizHelper anpvizHelper = new AnpvizHelper();

		//Collection<String> relMoveList = relMoveHandler.getRelMoveNames();

		//logger.info("relMoveList = " + relMoveList);
		commandData = anpvizHelper.getPtzTaskParameters(getName());
	}

	protected void start() throws SensorException {
		// reset to Pan=0, Tilt=0, Zoom=0

	}

	protected void stop() {
	}

	@Override
	public DataComponent getCommandDescription() {
		return commandData;
	}

	@Override
	protected boolean execCommand(DataBlock command) throws CommandException {
		// associate command data to msg structure definition
		DataRecord commandMsg = (DataRecord) commandData.copy();
		commandMsg.setData(command);

		try {
			// continuous movement
			logger.info("Tasking with continuous Movement...");
			int pan = (int)command.getFloatValue(0);
			int tilt = (int)command.getFloatValue(1);
			int zoom = (int)command.getFloatValue(2);

			AnpvizPtzTuple moveVec = new AnpvizPtzTuple(pan, tilt, zoom);
			parent.device.ptzMove(moveVec);
		} catch (Exception e) {
			throw new CommandException("Error connecting to Anpviz PTZ control", e);
		}
		return true;
	}
}
