package org.sensorhub.impl.sensor.anpviz;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;

import jakarta.xml.soap.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

public class AnpvizDevice {

    private static final Logger logger = LoggerFactory.getLogger(AnpvizDevice.class);
    private static final String KEY = "WebLogin";
    private String SOAP_URI;
    private String SOAP_PREFIX;
    private static final String LOGIN_PATH = "ipcLogin";
    private static final String USER_CONFIG_PATH = "getUserConfigPwdEntrypt"; // Yes, the "typo" is correct
    private static final String MEDIA_CONFIG_PATH = "getMediaVideoConfig";
    private static final String SYSTEM_VERSION_PATH = "getSystemVersionInfo";
    private static final String PTZ_CONFIG_PATH = "getPtzConfig";
    private static final String PRESET_LIST_PATH = "getPresetList";
    private static final String TIME_CONFIG_PATH = "getTimeConfig";
    private static final String MEDIA_STREAM_CONFIG_PATH = "getMediaStreamConfig";

    String remoteHost;
    int remotePort;
    String user;
    String rawPasswd;

    public AnpvizDevice(String remoteHost, int remotePort, String user, String passwd) throws SOAPException, MalformedURLException {
        this.remoteHost = remoteHost;
        this.user = user;
        this.rawPasswd = passwd;
        this.remotePort = remotePort;

        SOAPConnectionFactory soapFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection soapConnection = soapFactory.createConnection();

        SOAPMessage request = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage();
        SOAPEnvelope envelope = request.getSOAPPart().getEnvelope();
        SOAP_URI = envelope.getNamespaceURI();
        SOAP_PREFIX = envelope.getPrefix();
        envelope.addNamespaceDeclaration(SOAP_PREFIX, SOAP_URI);
        SOAPHeader header = request.getSOAPHeader();
        SOAPBody body = request.getSOAPBody();
        header.setPrefix(SOAP_PREFIX);

        addMimeHeaders(request.getMimeHeaders());
        SOAPElement userId = header.addHeaderElement(new QName(SOAP_URI, "userid", SOAP_PREFIX));
        userId.addTextNode(encryptDesHex(user, KEY));
        SOAPElement passWd = header.addHeaderElement(new QName(SOAP_URI, "passwd", SOAP_PREFIX));
        passWd.addTextNode(encryptDesHex(passwd, KEY));

        SOAPMessage response = soapConnection.call(request, getEndpoint() + "/" + LOGIN_PATH);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            request.writeTo(out);
            String strMsg = new String(out.toByteArray());
            logger.debug("Request: {}", strMsg);
        } catch (Exception ignored) {}

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.writeTo(out);
            String strMsg = new String(out.toByteArray());
            logger.debug("Response: {}", strMsg);
        } catch (Exception ignored) {}
    }

    private void addMimeHeaders(MimeHeaders headers) {
        headers.setHeader("Accept", "text/javascript, text/html, application/xml, text/xml, */*");
        headers.setHeader("Accept-Language", "en-US,en;q=0.9");
        headers.setHeader("Connection", "keep-alive");
        headers.setHeader("Content-type", "application/x-www-form-urlencoded");
        headers.setHeader("Host", remoteHost);
        headers.setHeader("Origin", getEndpointNoPort());
        headers.setHeader("Referer", getEndpointNoPort() + "/");
        headers.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
        headers.setHeader("X-Requested-With", "XMLHttpRequest");

        // Add cookies (use "Cookie" header)
        String cookieValue = "DHLangCookie30=English; ipc_" + remoteHost + "_username=" + user + "; ipc_" + remoteHost + "_password=" + encryptMd5Hex(rawPasswd) + "; ipc_" + remoteHost + "_webLanguage=en_us; ipc_" + remoteHost + "_KeepScale=0";
        headers.addHeader("Cookie", cookieValue);
    }

    public String getEndpoint() {
        return getEndpointNoPort() + ":" + remotePort;
    }

    public String getEndpointNoPort() {
        return "http://" + remoteHost;
    }

    private String encryptDesHex(String input, String key) {
        try {
            byte[] keyBytes = key.substring(0, 8).getBytes("UTF-8");
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DES");

            Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");

            byte[] inputBytes = input.getBytes("UTF-8");
            byte[] padded = new byte[8];
            System.arraycopy(inputBytes, 0, padded, 0, Math.min(inputBytes.length, 8));

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

    // TODO
    private String encryptMd5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));

            // Convert to uppercase hex
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
