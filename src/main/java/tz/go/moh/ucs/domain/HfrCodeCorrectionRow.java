package tz.go.moh.ucs.domain;

public class HfrCodeCorrectionRow {
    private String locationName;
    private String locationType;
    private String incorrectHfrCode;
    private String correctHfrCode;
    private String uid;

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getIncorrectHfrCode() {
        return incorrectHfrCode;
    }

    public void setIncorrectHfrCode(String incorrectHfrCode) {
        this.incorrectHfrCode = incorrectHfrCode;
    }

    public String getCorrectHfrCode() {
        return correctHfrCode;
    }

    public void setCorrectHfrCode(String correctHfrCode) {
        this.correctHfrCode = correctHfrCode;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
