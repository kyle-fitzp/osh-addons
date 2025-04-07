/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ffmpeg;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;

/**
 * Class named in META-INF/services/org.sensorhub.api.module.IModuleProvider that informs OpenSensorHub of
 * a sensor driver and its configuration class.
 * In this case, it tells OSH about the {@link FFMPEGSensor}.
 */
public class FFMPEGDescriptor extends JarModuleProvider implements IModuleProvider {
    /**
     * Retrieves the class implementing the OpenSensorHub interface necessary to
     * perform SOS/SPS/SOS-T operations.
     *
     * @return The class used to interact with the sensor/sensor platform.
     */
    @Override
    public Class<? extends IModule<?>> getModuleClass() {
        return FFMPEGSensor.class;
    }

    /**
     * Identifies the class used to configure this driver
     *
     * @return The java class used to exposing configuration settings for the driver.
     */
    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass() {
        return FFMPEGConfig.class;
    }
}
