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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.anpviz.ptz.AnpvizPTZpreset;
import org.sensorhub.impl.sensor.anpviz.ptz.AnpvizPTZpresetsHandler;
import org.sensorhub.impl.sensor.anpviz.ptz.AnpvizPTZrelMove;
import org.sensorhub.impl.sensor.anpviz.ptz.AnpvizPTZrelMoveHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataChoiceImpl;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;

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

	DataChoice commandData;
	AnpvizDriver DriverDataInterface;
	AnpvizPTZpresetsHandler presetsHandler;
	AnpvizPTZrelMoveHandler relMoveHandler;

	protected AnpvizPtzControl(AnpvizDriver driver) {
		super("ptzControl", driver);
	}

	protected void init() throws SensorException {
		logger.debug("Initializing PTZ control");
		AnpvizConfig config = parentSensor.getConfiguration();

		presetsHandler = new AnpvizPTZpresetsHandler(config.ptz);
		relMoveHandler = new AnpvizPTZrelMoveHandler(config.ptz);

		// build SWE data structure for the tasking parameters
		AnpvizHelper anpvizHelper = new AnpvizHelper();
		Collection<String> presetList = presetsHandler.getPresetNames();
		Collection<String> relMoveList = relMoveHandler.getRelMoveNames();
		logger.info("presetList = " + presetList);
		logger.info("relMoveList = " + relMoveList);
		commandData = anpvizHelper.getPtzTaskParameters(getName(), relMoveList, presetList);
	}

	protected void start() throws SensorException {
		// reset to Pan=0, Tilt=0, Zoom=0
		DataBlock initCmd;
		commandData.setSelectedItem(7);
		initCmd = commandData.createDataBlock();
		
		try
        {
            execCommand(initCmd);
        }
        catch (CommandException e)
        {
            throw new SensorException("Init command failed", e);
        }
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
		DataChoice commandMsg = (DataChoice) commandData.copy();
		commandMsg.setData(command);
		DataComponent component = ((DataChoiceImpl) commandMsg).getSelectedItem();
		DataBlock data = component.getData();

		logger.info("itemID = " + component.getName());

		try {
			// preset position
			if (component.getName().equals(AnpvizHelper.TASKING_PTZPRESET)) {
				AnpvizPTZpreset preset = presetsHandler.getPreset(data.getStringValue());
				logger.info("Tasking with PTZ preset " + preset.name);

				String presetCmd = new String();

				if (preset.name.equalsIgnoreCase("Reset"))
					presetCmd = "ptzReset";
				else
					presetCmd = "ptzGotoPresetPoint&name=" + preset.name;

				// move to preset
				URL optionsURL = new URL("http://" + parentSensor.getConfiguration().http.remoteHost + ":"
						+ Integer.toString(parentSensor.getConfiguration().http.remotePort)
						+ "/cgi-bin/CGIProxy.fcgi?cmd=" + presetCmd + "&usr="
						+ parentSensor.getConfiguration().http.user + "&pwd="
						+ parentSensor.getConfiguration().http.password);

				// add BufferReader and check for error
				InputStream is = optionsURL.openStream();
				BufferedReader reader = null;
				reader = new BufferedReader(new InputStreamReader(is));
				String line;
				while ((line = reader.readLine()) != null) {
					String[] tokens = line.split("<|\\>");
					if (tokens[1].trim().equals("runResult")) {
						if (!tokens[2].trim().equalsIgnoreCase("0"))
							System.err.println("Unrecognized Command");
						else
							logger.info("Successful Command");
					}
				}
				is.close();
			}

			// relative movement
			else if (component.getName().equals(AnpvizHelper.TASKING_PTZREL)) {
				logger.info("Tasking with relative Movement...");
				AnpvizPTZrelMove relMove = relMoveHandler.getRelMove(data.getStringValue());
				logger.info("rel move name = " + relMove.name);
				URL optionsURL = new URL("http://" + parentSensor.getConfiguration().http.remoteHost + ":"
						+ Integer.toString(parentSensor.getConfiguration().http.remotePort)
						+ "/cgi-bin/CGIProxy.fcgi?cmd=" + relMove.cgi + "&usr="
						+ parentSensor.getConfiguration().http.user + "&pwd="
						+ parentSensor.getConfiguration().http.password);
				// add BufferReader and read first line; if "Error", read second
				// line and log error
				InputStream is = optionsURL.openStream();
				BufferedReader reader = null;
				reader = new BufferedReader(new InputStreamReader(is));
				String line;
				while ((line = reader.readLine()) != null) {
					String[] tokens = line.split("<|\\>");
					if (tokens[1].trim().equals("result")) {
						if (!tokens[2].trim().equalsIgnoreCase("0"))
							logger.info("Unrecognized Command");
						else
							logger.info("Successful Command");
					}
				}
				is.close();
				// use timing to control amount of movement
				logger.info("moveTime = " + relMove.moveTime);
				Thread.sleep(relMove.moveTime);

				URL optionsURLstop = new URL("http://" + parentSensor.getConfiguration().http.remoteHost + ":"
						+ Integer.toString(parentSensor.getConfiguration().http.remotePort)
						+ "/cgi-bin/CGIProxy.fcgi?cmd=ptzStopRun" + "&usr=" + parentSensor.getConfiguration().http.user
						+ "&pwd=" + parentSensor.getConfiguration().http.password);
				InputStream isStop = optionsURLstop.openStream();
				isStop.close();
			}

		} catch (Exception e) {
			throw new CommandException("Error connecting to Anpviz PTZ control", e);
		}

		return true;
	}
}
