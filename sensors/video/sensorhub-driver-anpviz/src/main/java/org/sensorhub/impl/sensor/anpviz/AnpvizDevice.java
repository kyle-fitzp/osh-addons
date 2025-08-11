package org.sensorhub.impl.sensor.anpviz;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.ParserConfigurationException;

import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.anpviz.ptz.AnpvizPtzTuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class AnpvizDevice {
    private enum AnpvizRequest {
        LOGIN_PATH("ipcLogin"),
        USER_CONFIG_PATH("getUserConfigPwdEntrypt"),
        MEDIA_CONFIG_PATH("getMediaVideoConfig"),
        SYSTEM_VERSION_PATH("getSystemVersionInfo"),
        PTZ_CONFIG_PATH("getPtzConfig"),
        GET_PRESET_LIST_PATH("getPresetList"),
        MOD_PRESET_LIST_PATH("PresetList"),
        TIME_CONFIG_PATH("getTimeConfig"),
        MEDIA_STREAM_CONFIG_PATH("getMediaStreamConfig"),
        SET_PTZ_CMD_PATH("setPTZCmd");

        private AnpvizRequest(String requestPath) {
            this.requestPath = requestPath;
        };
        public final String requestPath;
    }

    private static final Logger logger = LoggerFactory.getLogger(AnpvizDevice.class);
    private static final String KEY = "WebLogin"; // Encryption key was stored as plain text in the JS

    String remoteHost;
    int remotePort;
    String user;
    String rawPasswd;
    String md5Pass;
    String desPass;
    String userGroup;
    int rtspPort = -1;
    int[] presets;

    public AnpvizDevice(String remoteHost, int remotePort, String user, String passwd) throws IOException, ParserConfigurationException, SAXException, SensorException {
        this.remoteHost = remoteHost;
        this.user = user;
        this.rawPasswd = passwd;
        this.remotePort = remotePort;
        this.md5Pass = encryptMd5(rawPasswd);
        this.desPass = encryptDes(rawPasswd, KEY);
        login();
        getUserConfig();
        getMediaStreamConfig();
        updatePresetList();
    }

    public void login() throws IOException, SensorException {
        sendRequest("", AnpvizRequest.LOGIN_PATH);
    }

    public void getUserConfig() throws IOException, ParserConfigurationException, SAXException, SensorException {
        String response = sendRequest("",  AnpvizRequest.USER_CONFIG_PATH);
        this.userGroup = getXmlAttribute(response, "Group", response.indexOf(user));
    }

    public void getMediaStreamConfig() throws SensorException, IOException {
        String response = sendRequest("",  AnpvizRequest.MEDIA_STREAM_CONFIG_PATH);
        this.rtspPort = Integer.parseInt(getXmlAttribute(response, "VideoPort"));
    }

    public void updatePresetList() throws SensorException, IOException {
        String response = sendRequest("", AnpvizRequest.GET_PRESET_LIST_PATH);
        response = response.substring(response.indexOf("<PresetList>") + "<PresetList>".length(),
                response.indexOf("</PresetList>"));

        try {
            this.presets = Arrays.stream(response.replaceAll("</?p>", " ").trim().split("\\s+"))
                    .mapToInt(Integer::parseInt)
                    .toArray();
        } catch (Exception e) {
            // No p elems means no presets. Create empty array.
            this.presets = new int[0];
        }

    }

    public int[] getPresets() {
        return this.presets;
    }

    public void gotoPreset(int preset) throws SensorException, IOException {
        boolean hasPreset = false;
        for (int i = 0; i < this.presets.length; i++) {
            if (presets[i] == preset) {
                hasPreset = true;
                break;
            }
        }
        if (!hasPreset) {
            throw new SensorException("Preset " + preset + " not available");
        } else {
            String xml = "<xml><cmd>callpreset</cmd><preset>" + preset + "</preset></xml>";
            sendRequest(xml, AnpvizRequest.MOD_PRESET_LIST_PATH);
        }
    }

    public void addPreset(int preset) throws SensorException, IOException {
        String xml = "<xml><cmd>setpreset</cmd><preset>" + preset + "</preset><flag>1</flag></xml>";
        sendRequest(xml, AnpvizRequest.MOD_PRESET_LIST_PATH);
        updatePresetList();
    }

    public void removePreset(int preset) throws SensorException, IOException {
        String xml = "<xml><cmd>clearpreset</cmd><preset>" + preset + "</preset></xml>";
        sendRequest(xml, AnpvizRequest.MOD_PRESET_LIST_PATH);
        updatePresetList();
    }

    protected String getXmlAttribute(String xml, String attributeName) {
        attributeName += "=\"";
        String value = "";
        int index = -1;
        if ((index = xml.indexOf(attributeName)) > -1) {
            value = xml.substring(index + attributeName.length(), xml.indexOf("\"", index + attributeName.length() + 1));
        }
        return value;
    }

    protected String getXmlAttribute(String xml, String attributeName, int from) {
        attributeName += "=\"";
        String value = "";
        int index = -1;
        if ((index = xml.indexOf(attributeName, from)) > -1) {
            value = xml.substring(index + attributeName.length(), xml.indexOf("\"", index + attributeName.length() + 1));
        }
        return value;
    }

    public String getMediaUrl() throws IOException {
        return "rtsp://" + user + ":" + md5Pass + "@" + remoteHost + ":" + rtspPort + "/stream0";
    }

    public void ptzMove(AnpvizPtzTuple moveVector) throws IOException, SensorException {
        int panSpeed = moveVector.getPanSpeed();
        int tiltSpeed = moveVector.getTiltSpeed();
        String direction = moveVector.getDirection();
        String zoom = moveVector.getZoom();
        String xmlBody;
        boolean didStop = false;

        // Cancel ptz if either pt or z need to stop
        if (direction.isEmpty() || zoom.isEmpty()) {
            xmlBody = "<xml><cmd>stop</cmd></xml>";
            sendRequest(xmlBody, AnpvizRequest.SET_PTZ_CMD_PATH);
            didStop = true;
        }

        if (!direction.isEmpty()) {
            xmlBody = "<xml><cmd>"
                    + direction
                    + "</cmd><panspeed>"
                    + panSpeed
                    + "</panspeed><tiltspeed>"
                    + tiltSpeed
                    + "</tiltspeed></xml>";
            sendRequest(xmlBody, AnpvizRequest.SET_PTZ_CMD_PATH);
        }
        if (!zoom.isEmpty()) {
            // If the stop command was sent, wait a moment so that the zoom command is processed.
            if (didStop) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
            xmlBody = "<xml><cmd>" + zoom + "</cmd></xml>";
            sendRequest(xmlBody, AnpvizRequest.SET_PTZ_CMD_PATH);
        }
    }

    // Have to do all the SOAP request generation/parsing manually; this camera does not
    // use valid SOAP (or even well-formed XML).
    private String sendRequest(String xmlBody, AnpvizRequest request) throws IOException, SensorException {
        String desUser = encryptDes(user, KEY).trim();
        String desPass = encryptDes(rawPasswd, KEY).trim();

        if (xmlBody == null) {
            xmlBody = "";
        }

        String xml = """
                <?xml version=\"1.0\"?>
                <soap:Envelope xmlns:soap=\"http://www.w3.org/2001/12/soap-envelope\">
                    <soap:Header>
                        <userid>""" + desUser + """
                        </userid>
                        <passwd>""" + desPass + "</passwd>"
                +"""
                    </soap:Header>
                    <soap:Body>""" + xmlBody + "</soap:Body>"
                +"</soap:Envelope>";


        URL url = new URL(getEndpoint() + "/" + request.requestPath);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        conn.setRequestProperty("Accept", "text/javascript, text/html, application/xml, text/xml, */*");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        conn.setRequestProperty("Connection", "keep-alive");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Host", remoteHost);
        conn.setRequestProperty("Origin", getEndpointNoPort());
        conn.setRequestProperty("Referer", getEndpointNoPort() + "/");
        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        conn.setRequestProperty("Cookie", getCookieHeader());

        logger.debug("Sending XML:\n{}", xml);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(xml.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        logger.debug("Response Code: {}", responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                logger.debug(line);
                response.append(line);
            }
        }
        return response.toString();
    }

    private String getCookieHeader() throws SensorException {
        return "DHLangCookie30=English; ipc_" + remoteHost + "_username=" + user +
                "; ipc_" + remoteHost + "_password=" + encryptMd5(rawPasswd) +
                "; ipc_" + remoteHost + "_webLanguage=en_us; ipc_" + remoteHost + "_KeepScale=0";
    }

    public String getEndpoint() {
        return getEndpointNoPort() + ":" + remotePort;
    }

    public String getEndpointNoPort() {
        return "http://" + remoteHost;
    }

    private String encryptDes(String input, String key) throws SensorException {
        try {
            byte[] keyBytes = key.substring(0, 8).getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DES");
            Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");

            byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
            int padLength = 8 - inputBytes.length % 8;
            byte[] padded = new byte[inputBytes.length + padLength];
            System.arraycopy(inputBytes, 0, padded, 0, inputBytes.length);
            /*
            byte[] padded = new byte[8];
            System.arraycopy(inputBytes, 0, padded, 0, Math.min(inputBytes.length, 8));
             */
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(padded);

            StringBuilder hex = new StringBuilder();
            for (byte b : encrypted) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (Exception e) {
            throw new SensorException("Could not generate DES", e);
        }
    }

    private String encryptMd5(String input) throws SensorException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02X", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new SensorException("Could not generate MD5", e);
        }
    }
}
