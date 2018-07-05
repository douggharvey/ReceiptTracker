package application;

import android.app.Application;

import com.facebook.stetho.Stetho;

import timber.log.Timber;

public class ReceiptTrackerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
        Stetho.initializeWithDefaults(this);
    }
}
