package org.sensorhub.impl.sensor.onvif;

import de.onvif.discovery.OnvifDiscovery;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.Text;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataChoiceImpl;
import org.vast.data.SWEFactory;
import org.vast.data.TextImpl;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.TreeSet;

public class OnvifDiscoveryControl extends AbstractSensorControl<OnvifCameraDriver> {

    private static final Logger log = LoggerFactory.getLogger(OnvifPresetsControl.class);
    DataChoice commandData = null;
    SWEFactory fac;


    protected OnvifDiscoveryControl(OnvifCameraDriver driver) {
        super("onvifDiscovery", driver);
        fac = new SWEFactory();
    }

    protected void init() {
        if (parentSensor.config.networkConfig.remoteHost != null && !parentSensor.config.networkConfig.remoteHost.isBlank())
            return;

        TreeSet<URL> urls = (TreeSet<URL>) OnvifDiscovery.discoverOnvifURLs();
        commandData = fac.newDataChoice();
        commandData.setName("discoveredUrls");
        commandData.setLabel("Discovered URLs");
        Text tempText = fac.newText();
        tempText.setName("");
        tempText.setValue("");
        commandData.addItem("", tempText);
        for (URL url: urls) {
            Text foundUrl = fac.newText();
            foundUrl.setValue(url.toString());
            foundUrl.setName(url.toString());
            commandData.addItem(foundUrl.getName(), foundUrl);
        }
    }

    @Override
    public DataComponent getCommandDescription()
    {
        return commandData;
    }

    @Override
    protected boolean execCommand(DataBlock command) throws CommandException {
        // associate command data to msg structure definition
        DataChoice commandMsg = (DataChoice) commandData.copy();

        if (command == null)
            return false;
        commandMsg.setData(command);

        DataComponent component = ((DataChoiceImpl) commandMsg).getSelectedItem();
        String itemID = component.getName();
        URI connectionUri = URI.create(itemID);
        parentSensor.config.networkConfig.remoteHost = connectionUri.getHost();
        parentSensor.hostIp = connectionUri.getHost();
        parentSensor.config.networkConfig.remotePort = connectionUri.getPort();
        parentSensor.hostPort = connectionUri.getPort();

        try {
            parentSensor.stop();
        } catch (SensorHubException e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }
}
