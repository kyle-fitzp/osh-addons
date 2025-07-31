package org.sensorhub.impl.sensor.anpviz;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.xml.*;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class AnpvizDevice {
    private enum AnpvizRequest {
        LOGIN_PATH("ipcLogin"),
        USER_CONFIG_PATH("getUserConfigPwdEntrypt"),
        MEDIA_CONFIG_PATH("getMediaVideoConfig"),
        SYSTEM_VERSION_PATH("getSystemVersionInfo"),
        PTZ_CONFIG_PATH("getPtzConfig"),
        PRESET_LIST_PATH("getPresetList"),
        TIME_CONFIG_PATH("getTimeConfig"),
        MEDIA_STREAM_CONFIG_PATH("getMediaStreamConfig");

        private AnpvizRequest(String requestPath) {
            this.requestPath = requestPath;
        };
        public final String requestPath;
    }

    private static final Logger logger = LoggerFactory.getLogger(AnpvizDevice.class);
    private static final String KEY = "WebLogin"; // Encryption key was stored as plain text in the JS lol

    String remoteHost;
    int remotePort;
    String user;
    String rawPasswd;
    String userGroup;

    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

    public AnpvizDevice(String remoteHost, int remotePort, String user, String passwd) throws IOException, ParserConfigurationException, SAXException {
        this.remoteHost = remoteHost;
        this.user = user;
        this.rawPasswd = passwd;
        this.remotePort = remotePort;
        login();
        getUserConfig();
    }

    public void login() throws IOException {
        sendRequest("", AnpvizRequest.LOGIN_PATH);
    }

    public void getUserConfig() throws IOException, ParserConfigurationException, SAXException {
        String response = sendRequest("",  AnpvizRequest.USER_CONFIG_PATH);
        String groupAtt = "Group=\"";
        String group = "";
        int index = -1;
        if ((index = response.indexOf(groupAtt, response.indexOf(user))) > -1) {
            group = response.substring(index + groupAtt.length(), response.indexOf("\"", index + groupAtt.length() + 1));
        }
        this.userGroup = group;
    }

    // Have to do all the SOAP request generation/parsing manually; this camera does not
    // use valid SOAP (or even well-formed XML).
    private String sendRequest(String xmlBody, AnpvizRequest request) throws IOException {
        String desUser = encryptDesHex(user, KEY).trim();
        String desPass = encryptDesHex(rawPasswd, KEY).trim();

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

    private String getCookieHeader() {
        return "DHLangCookie30=English; ipc_" + remoteHost + "_username=" + user +
                "; ipc_" + remoteHost + "_password=" + encryptMd5Hex(rawPasswd) +
                "; ipc_" + remoteHost + "_webLanguage=en_us; ipc_" + remoteHost + "_KeepScale=0";
    }

    public String getEndpoint() {
        return getEndpointNoPort() + ":" + remotePort;
    }

    public String getEndpointNoPort() {
        return "http://" + remoteHost;
    }

    private String encryptDesHex(String input, String key) {
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
            logger.error("Could not generate DES", e);
        }
        return null;
    }

    private String encryptMd5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02X", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.error("Could not generate MD5", e);
        }
        return null;
    }
}
