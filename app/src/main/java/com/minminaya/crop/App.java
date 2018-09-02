package com.minminaya.crop;

import android.app.Application;

public class App extends Application {
    private static App sInstance;

    public static App getsInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }
}
