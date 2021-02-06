package com.ynov.vernet.to_dolist;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.Objects;

public class Internet extends Activity {
    Activity activity;
    Context context;

    private static final String TAG = "Internet";

    Internet(Activity activity, Context context) {
        this.activity = activity;
        this.context = context;
    }

    public boolean internet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Objects.requireNonNull(Objects.requireNonNull(connectivityManager).getNetworkInfo(ConnectivityManager.TYPE_MOBILE)).getState() == NetworkInfo.State.CONNECTED || Objects.requireNonNull(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)).getState() == NetworkInfo.State.CONNECTED) {
            Log.d(TAG, "onCreate: Internet enable");

            return true;
        } else {
            Log.d(TAG, "onCreate: Internet disabled");
            return false;
        }
    }
}
