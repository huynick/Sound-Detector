package com.research.uw.sounddetector;

/**
 * Created by Nicholas on 4/8/2015.
 */
public class SoundType {
    private String name;
    private boolean inUse;

    public SoundType(String name, boolean inUse) {
        this.name = name;
        this.inUse = inUse;
    }


    public String getName() {
        return name;
    }

    public boolean getInUse() {
        return inUse;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
}
