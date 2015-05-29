package com.research.uw.sounddetector;

import android.app.Application;
import android.util.Log;

import com.parse.Parse;

public class ParseApplication extends Application {

    @Override
    public void onCreate() {
        Parse.initialize(this.getBaseContext(), "8Dnotr5GlTj7YebZstzzrxcbCSzmHcF1sOVGredV", "yqljM1Je0Bg42YZwiqQeRaLlbHzt5uTlnYXECuI4");
    }
}
