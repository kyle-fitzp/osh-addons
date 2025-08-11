package org.sensorhub.impl.sensor.anpviz.ptz;

public class AnpvizPtzTuple {
    int panSpeed;
    int tiltSpeed;
    int zoomSpeed;

    public AnpvizPtzTuple() {
        this.panSpeed = 0;
        this.tiltSpeed = 0;
    }

    public AnpvizPtzTuple(int panSpeed, int tiltSpeed) {
        this.panSpeed = panSpeed;
        this.tiltSpeed = tiltSpeed;
    }

    public AnpvizPtzTuple(int panSpeed, int tiltSpeed, int zoomSpeed) {
        this.panSpeed = panSpeed;
        this.tiltSpeed = tiltSpeed;
        this.zoomSpeed = zoomSpeed;
    }

    public void setPanSpeed(int panSpeed) {
        this.panSpeed = panSpeed;
    }

    public void setTiltSpeed(int tiltSpeed) {
        this.tiltSpeed = tiltSpeed;
    }

    public int getPanSpeed() {
        return panSpeed;
    }

    public int getTiltSpeed() {
        return tiltSpeed;
    }

    public int getCombinedSpeed() {
        return (int)(Math.round(Math.sqrt(this.panSpeed * this.panSpeed + this.tiltSpeed * this.tiltSpeed)));
    }

    public String getZoom() {

        if (zoomSpeed > 0) {
            return "zoomtele";
        } else if (zoomSpeed < 0) {
            return "zoomwide";
        } else {
            return "";
        }
    }

    public String getDirection() {
        String result = "";
        // Diagonals are inverted for some reason

        // Pan right
        if (panSpeed < 0) {
            if (tiltSpeed < 0) {
                result = "left_up"; // Pan right down
            } else if (tiltSpeed > 0) {
                result = "left_down"; // Pan right up
            } else {
                result = "right";
            }

        // Pan left
        } else if (panSpeed > 0) {
            if (tiltSpeed < 0) {
                result = "right_up"; // Pan left down
            } else if (tiltSpeed > 0) {
                result = "right_down"; // Pan left up
            } else {
                result = "left";
            }
        } else {
            if (tiltSpeed < 0) {
                result = "down";
            }
            else if (tiltSpeed > 0) {
                result = "up";
            }
        }

        return result;
    }
}
