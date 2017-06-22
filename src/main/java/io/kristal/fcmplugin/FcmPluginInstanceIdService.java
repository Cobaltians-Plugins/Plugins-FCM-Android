package io.kristal.fcmplugin;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;


public class FcmPluginInstanceIdService extends Service {
    protected final static String TAG = FcmPluginInstanceIdService.class.getSimpleName();

    public FcmPluginInstanceIdService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.i(TAG, "Refreshed token: " + refreshedToken);
        FcmPlugin.getInstance().setToken(refreshedToken);
        FcmPlugin.getInstance().onTokenReceived();
        
    }

}
