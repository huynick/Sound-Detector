package com.research.uw.sounddetector;

public class Recording {
    private String name;
    private String fileName;
    private String soundType;

    public Recording(String name, String fileName, String soundType) {
        this.name = name;
        this.fileName = fileName;
        this.soundType = soundType;
    }

    public String getSoundType() {
        return soundType;
    }

    public void setSoundType(String soundType) {
        this.soundType = soundType;
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setName(String name) {
        this.name = name;
    }
}
