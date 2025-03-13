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

import java.net.URL;
import java.util.*;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;

import net.opengis.swe.v20.Vector;
import org.onvif.ver10.schema.*;
import org.onvif.ver20.ptz.wsdl.PTZ;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.sensorhub.impl.sensor.videocam.ptz.PtzConfig;
import org.sensorhub.impl.sensor.videocam.ptz.PtzPreset;
import org.sensorhub.impl.sensor.videocam.ptz.PtzPresetsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockString;
import org.vast.data.DataChoiceImpl;

import de.onvif.soap.OnvifDevice;
import org.vast.data.SWEFactory;

import javax.xml.datatype.DatatypeFactory;

/**
 * <p>
 * Implementation of sensor interface for generic cameras using ONVIF 
 * protocol. This particular class provides control of the Pan-Tilt-Zoom
 * (PTZ) capabilities.
 * </p>
 * 
 * @author Joshua Wolfe <developer.wolfe@gmail.com>
 * @since June 13, 2017
 */

public class OnvifPtzControl extends AbstractSensorControl<OnvifCameraDriver>
{
	private static final Logger log = LoggerFactory.getLogger(OnvifPtzControl.class);
    // define and set default values
    double minPan = -180.0;
    double maxPan = 180.0;
    double minTilt = -180.0;
    double maxTilt = 0.0;
    double minZoom = 1.0;
    double maxZoom = 9999;
	PTZSpeed speed = new PTZSpeed();

    PtzPresetsHandler presetsHandler;
	PTZ ptz = null;
    Map<PtzPreset, String> presetsMap;
    URL optionsURL = null;
	PTZConfiguration devicePtzConfig = null;

	DataChoice commandDataCopy = null;
    
	DataChoice commandData = null;

    protected OnvifPtzControl(OnvifCameraDriver driver)
    {
        super("ptzControl", driver);


    }


    protected void init()
    {
		if (parentSensor.ptzProfile != null) {
			PTZConfiguration devicePtzConfig = parentSensor.ptzProfile.getPTZConfiguration();
			if (devicePtzConfig != null) {
				PanTiltLimits panTiltLimits = devicePtzConfig.getPanTiltLimits();
				if (panTiltLimits != null) {
					minPan = panTiltLimits.getRange().getXRange().getMin();
					maxPan = panTiltLimits.getRange().getXRange().getMax();
					minTilt = panTiltLimits.getRange().getYRange().getMin();
					maxTilt = panTiltLimits.getRange().getYRange().getMax();
				}
				ZoomLimits zoomLimits = devicePtzConfig.getZoomLimits();
				if (zoomLimits != null) {
					minZoom = zoomLimits.getRange().getXRange().getMin();
					maxZoom = zoomLimits.getRange().getXRange().getMax();
				}
				if (devicePtzConfig.getDefaultPTZSpeed() != null)
					speed = devicePtzConfig.getDefaultPTZSpeed();
			}
		}

		PtzConfig ptzConfig = new PtzConfig();
		presetsMap = new LinkedHashMap<PtzPreset, String>();

		if (parentSensor.ptzProfile != null) {
			ptz = parentSensor.camera.getPtz();
			List<PTZPreset> presets = parentSensor.camera.getPtz().getPresets(parentSensor.ptzProfile.getToken());
			for (PTZPreset p : presets) {
				PtzPreset preset = new PtzPreset();
				PTZVector ptzPos = p.getPTZPosition();

				if (ptzPos != null) {
					preset.name = p.getName();
					Vector2D panTiltVec = ptzPos.getPanTilt();
					preset.pan = panTiltVec.getX();
					preset.tilt = panTiltVec.getY();
					Vector1D zoomVec = ptzPos.getZoom();
					preset.zoom = zoomVec.getX();
				}

				ptzConfig.presets.add(preset);
				presetsMap.put(preset, p.getToken());
			}
			presetsHandler = new PtzPresetsHandler(ptzConfig);
		}
		log.debug("Control 115");

        // build SWE data structure for the tasking parameters
        VideoCamHelper videoHelper = new VideoCamHelper();
        Collection<String> presetList = presetsHandler.getPresetNames();
		// TODO Configure commandData to only include commands supported by the camera's ptz config

		commandData = videoHelper.getPtzTaskParameters(getName(), minPan, maxPan, minTilt, maxTilt, minZoom, maxZoom, presetList);

		// Added support for continuous move
		Vector speedComponent = videoHelper.createVelocityVector(null).build();
		commandData.addItem("continuous", speedComponent);

		commandDataCopy = commandData.copy();

	}
    
    @Override
    protected boolean execCommand(DataBlock command) throws CommandException
    {

		log.debug("Control 129");
    	// associate command data to msg structure definition
        DataChoice commandMsg = (DataChoice) commandData.copy();

		log.debug("Control 132");
		if (command == null)
			return false;
		log.debug("Control 136");
        commandMsg.setData(command);
		log.debug("Control 138");

        DataComponent component = ((DataChoiceImpl) commandMsg).getSelectedItem();
		log.debug("Control 141");
        String itemID = component.getName();
		log.debug("Control 143");
		DataBlock data = component.getData();
		log.debug("Control 145");

		//var configToken = ptz.getCompatibleConfigurations(parentSensor.profile.)
		//var configOpts = ptz.getConfigurationOptions(ptz.get);

        try
        {
			log.debug("Control 152");
        	OnvifDevice camera = parentSensor.camera;
        	Profile profile = parentSensor.ptzProfile;
			PTZConfiguration config = profile.getPTZConfiguration();
			//PTZVector position = camera.getPtz().getPosition(profile.getToken());
			log.debug("Control 157");
			var status = ptz.getStatus(profile.getToken());
			log.debug("Control 159");
			var position = status.getPosition();
			log.debug("Control 161");

			// Note: Some tasking is not supported for certain cameras
        	if (itemID.equals(VideoCamHelper.TASKING_PAN))
        	{
				log.debug("Control 165");
        		float pan = data.getFloatValue();
				Vector2D targetPanTilt = new Vector2D();
				targetPanTilt.setSpace(config.getDefaultAbsolutePantTiltPositionSpace());
				targetPanTilt.setX(pan);
				targetPanTilt.setY(position.getPanTilt().getY());
				position.setPanTilt(targetPanTilt);
				ptz.absoluteMove(profile.getToken(), position, speed);

        	}
        	else if (itemID.equals(VideoCamHelper.TASKING_TILT))
        	{
        		//float pan = position.getPanTilt().getX();
        		float tilt = data.getFloatValue();
        		//float zoom = position.getZoom().getX();
				Vector2D targetPanTilt = new Vector2D();
				targetPanTilt.setSpace(config.getDefaultAbsolutePantTiltPositionSpace());
				targetPanTilt.setX(position.getPanTilt().getX());
				targetPanTilt.setY(tilt);
				position.setPanTilt(targetPanTilt);

				camera.getPtz().absoluteMove(profile.getToken(), position, speed);
        	}
        	else if (itemID.equals(VideoCamHelper.TASKING_ZOOM))
        	{
        		//float pan = position.getPanTilt().getX();
        		//float tilt = position.getPanTilt().getY();
        		float zoom = data.getFloatValue();
				Vector1D zoomVec = new Vector1D();
				zoomVec.setSpace(config.getDefaultAbsoluteZoomPositionSpace());
				zoomVec.setX(zoom);
				position.setZoom(zoomVec);

        		camera.getPtz().absoluteMove(profile.getToken(), position, speed);
        	}
        	else if (itemID.equals(VideoCamHelper.TASKING_RPAN))
        	{
        		float rpan = data.getFloatValue();
				/*
				Vector2D targetPanTilt = new Vector2D();
				targetPanTilt.setX(rpan);
				targetPanTilt.setY(0.0f);
				position.setPanTilt(targetPanTilt);

				Vector1D zoomVec = new Vector1D();
				zoomVec.setX(0.0f);
				position.setZoom(zoomVec);

        		camera.getPtz().relativeMove(profile.getToken(), position, speed);
				*/

				//float tilt = position.getPanTilt().getY();
				//float zoom = position.getZoom().getX();
				Vector2D targetPanTilt = new Vector2D();
				targetPanTilt.setX(rpan);
				targetPanTilt.setY(0.0f);
				targetPanTilt.setSpace(config.getDefaultRelativePanTiltTranslationSpace());
				position.setPanTilt(targetPanTilt);

				camera.getPtz().relativeMove(profile.getToken(), position, speed);
				//var testDur = DatatypeFactory.newInstance().newDuration(500);
				//ptz.continuousMove(profile.getToken(), speed, testDur);

        	}
        	else if (itemID.equals(VideoCamHelper.TASKING_RTILT))
        	{
        		float rtilt = data.getFloatValue();
				Vector2D targetPanTilt = new Vector2D();
				targetPanTilt.setX(0.0f);
				targetPanTilt.setY(rtilt);
				targetPanTilt.setSpace(config.getDefaultRelativePanTiltTranslationSpace());
				position.setPanTilt(targetPanTilt);

				camera.getPtz().relativeMove(profile.getToken(), position, speed);
        	}
        	else if (itemID.equals(VideoCamHelper.TASKING_RZOOM))
        	{
				float rzoom = data.getFloatValue();

				Vector1D zoomVec = new Vector1D();
				zoomVec.setX(rzoom);
				zoomVec.setSpace(config.getDefaultRelativeZoomTranslationSpace());
				position.setZoom(zoomVec);

        		camera.getPtz().relativeMove(profile.getToken(), position, speed);
        	}
        	else if (itemID.equals(VideoCamHelper.TASKING_PTZPRESET))
        	{
				boolean hasPreset = false;
				String requestedPreset = data.getStringValue();
				if (requestedPreset == null)
					return false;
				for (String presetName : presetsHandler.getPresetNames()) {
					if (presetName.equals(requestedPreset)) {
						hasPreset = true;
						break;
					}
				}
				if (hasPreset) {
					PtzPreset preset = presetsHandler.getPreset(requestedPreset);
					camera.getPtz().gotoPreset(profile.getToken(), presetsMap.get(preset), speed);
				}
        	}
        	else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_PTZ_POS))
        	{
				float pan = component.getComponent("pan").getData().getFloatValue();
				float tilt = component.getComponent("tilt").getData().getFloatValue();
				float zoom = component.getComponent("zoom").getData().getFloatValue();

				Vector2D targetPanTilt = new Vector2D();
				targetPanTilt.setX(pan);
				targetPanTilt.setY(tilt);
				targetPanTilt.setSpace(config.getDefaultAbsolutePantTiltPositionSpace());
				position.setPanTilt(targetPanTilt);

				Vector1D zoomVec = new Vector1D();
				zoomVec.setX(zoom);
				zoomVec.setSpace(config.getDefaultAbsoluteZoomPositionSpace());
				position.setZoom(zoomVec);

        		camera.getPtz().absoluteMove(profile.getToken(), position, speed);
        	}
			else if (itemID.equalsIgnoreCase("continuous"))
			{
				PTZSpeed speedVec = new PTZSpeed();
				Vector2D panTiltSpeed = new Vector2D();
				panTiltSpeed.setX(0.0f);
				panTiltSpeed.setY(0.0f);
				panTiltSpeed.setX(data.getFloatValue(0));
				panTiltSpeed.setY(data.getFloatValue(1));
				panTiltSpeed.setSpace(config.getDefaultContinuousPanTiltVelocitySpace());
				speedVec.setPanTilt(panTiltSpeed);

				Vector1D zoomSpeed = new Vector1D();
				zoomSpeed.setX(0.0f);
				zoomSpeed.setX(data.getFloatValue(2));
				zoomSpeed.setSpace(config.getDefaultContinuousZoomVelocitySpace());
				speedVec.setZoom(zoomSpeed);
				// Note: Duration does not seem to work (at least on camera used for testing).
				camera.getPtz().continuousMove(profile.getToken(), speedVec, DatatypeFactory.newInstance().newDuration(500));
			}
	    }
	    catch (Exception e)
	    {	    	
	        throw new CommandException("Error sending PTZ command via ONVIF", e);
	    }        
       
        return true;
    }
    
    @Override
    public DataComponent getCommandDescription()
    {    
        return commandDataCopy;
    }


    protected void start() throws SensorException
    {

		// Immediately stop ptz output if ptz is not supported
		/*if (parentSensor.ptzProfile == null) {
			stop();
			return;
		}*/
		log.debug("---------SENSOR STARTING---------");
		DataBlock initCmd;
		commandData.setSelectedItem(6);
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

	public void stop()
	{
		commandData = commandDataCopy.copy();
		//commandData.clearData();
		//commandData.assignNewDataBlock();
		//commandData.assignNewDataBlock();
	}
}
