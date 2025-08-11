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

import java.util.Collection;

import net.opengis.swe.v20.*;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.vast.data.UnitReferenceImpl;


/**
 * <p>
 * Implementation of a helper class to support Anpviz video cameras with or without 
 * Pan-Tilt-Zoom (PTZ) control
 * </p>
 *
 * @author Lee Butler
 * @since September 2016
 */
public class AnpvizHelper extends VideoCamHelper
{
    // PTZ tasking commands
    public static final String TASKING_PTZPRESET = "preset";
    public static final String TASKING_PTZPRESET_GOTO = "presetGoto";
    public static final String TASKING_PTZPRESET_ADD = "presetAdd";
    public static final String TASKING_PTZPRESET_REMOVE = "presetRemove";
    public static final String TASKING_PTZREL = "relMove";
    public static String TASKING_PTZCONT = "contMove";


    public DataChoice getPtzTaskParameters(String name)
    {
        DataChoice commandData = this.newDataChoice();
        commandData.setName(name);

        // -100 and 100 are arbitrary
        Quantity pan = getPanComponent(-10, 10);
        pan.setUom(null);
        //commandData.addItem(TASKING_PAN, pan);
        Quantity tilt = getTiltComponent(-10, 10);
        tilt.setUom(null);
        //commandData.addItem(TASKING_TILT, tilt);
        Quantity zoom = getZoomComponent(-10, 10);
        zoom.setUom(null);
        //commandData.addItem(TASKING_ZOOM, zoom);
        
        // PTZ Preset Positions
        var presets = newDataChoice();
        presets.setName(TASKING_PTZPRESET);
        presets.setDefinition(getPropertyUri("CameraPresetPositionName"));
        presets.setLabel("Preset Camera Positions");

        var presetGoto = newCategory();
        presetGoto.setName(TASKING_PTZPRESET_GOTO);
        presetGoto.setLabel("Go To Preset");
        var allowedTokens = newAllowedTokens();
        presetGoto.setConstraint(allowedTokens);
        presets.addItem(TASKING_PTZPRESET_GOTO, presetGoto);

        var presetRemove = newCategory();
        presetRemove.setName(TASKING_PTZPRESET_REMOVE);
        presetRemove.setLabel("Remove Preset");
        var allowedTokensRemove = newAllowedTokens();
        presetRemove.setConstraint(allowedTokensRemove);
        presets.addItem(TASKING_PTZPRESET_REMOVE, presetRemove);

        var presetAdd = newCount(DataType.INT);
        presetAdd.setName(TASKING_PTZPRESET_ADD);
        presetAdd.setLabel("Add Preset");
        var range = newAllowedValues();
        range.addInterval(new double[] {1, 255});
        presetAdd.setConstraint(range);
        presets.addItem(TASKING_PTZPRESET_ADD, presetAdd);

        commandData.addItem(TASKING_PTZPRESET, presets);
        
        // PTZ Continuous Movements
        DataRecord contMove = newDataRecord(3);
        contMove.setDefinition(getPropertyUri("CameraContinuousMovementName"));
        contMove.setLabel("Camera Continuous Movement");
        contMove.addComponent("pan", pan.copy());
        contMove.addComponent("tilt", tilt.copy());
        contMove.addComponent("zoom", zoom.copy());
        commandData.addItem(TASKING_PTZCONT, contMove);
        
        return commandData;
    }
}
