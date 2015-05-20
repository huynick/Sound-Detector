package com.research.uw.sounddetector;

public class Recording {
    private String name;
    private String fileName;
    private String soundType;
    private double start;
    private double end;

    public Recording(String name, String fileName, String soundType, double start, double end) {
        this.name = name;
        this.fileName = fileName;
        this.soundType = soundType;
        this.start = start;
        this.end = end;
    }

    public double getStart() {
        return start;
    }

    public double getEnd() {
        return end;
    }

    public void setStart(double start) {
        this.start = start;
    }

    public void setEnd(double end) {
        this.end = end;
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
