package io.kristal.fcmplugin;

import android.content.Context;
import android.util.Log;

import org.cobaltians.cobalt.Cobalt;
import org.cobaltians.cobalt.fragments.CobaltFragment;
import org.cobaltians.cobalt.plugin.CobaltAbstractPlugin;
import org.cobaltians.cobalt.plugin.CobaltPluginWebContainer;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.lang.ref.WeakReference;


/**
 * Created by antoine on 07/02/2017.
 */

public class FcmPlugin extends CobaltAbstractPlugin {

    /*******************************************************************************************************
     * CONSTANTS
     *******************************************************************************************************/
    protected final static String TAG = FcmPlugin.class.getSimpleName();

    protected final static String KEY_TOKEN = "token";
    protected final static String KEY_TOPIC = "topic";

    protected final static String EVENT_GETTOKEN = "getToken";
    protected final static String EVENT_SUBSCRIBE = "subscribeToTopic";
    protected final static String EVENT_UNSUBSCRIBE = "unsubscribeFromTopic";

    /*******************************************************************************************************
     * MEMBERS
     *******************************************************************************************************/
    private String token;
    private WeakReference<CobaltFragment> fragment;
    private String callback;
    private Context context;

    /*******************************************************************************************************
     * MEMBERS ACCESSIBILITY
     *******************************************************************************************************/
    protected void setToken(String token){
        this.token = token;
    }
    protected Context getContext() { return this.context; }

    /*******************************************************************************************************
     * SINGLETON
     *******************************************************************************************************/
    protected static FcmPlugin sInstance;

    public static CobaltAbstractPlugin getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new FcmPlugin(context);
        }

        return sInstance;
    }
    private FcmPlugin(Context context){
        // Token is null at start
        this.token = FirebaseInstanceId.getInstance().getToken(); //Null if token not yet generated, device token else
        this.callback = null;
        this.fragment = new WeakReference<CobaltFragment>(null);
        this.context = context;
    }


    /*******************************************************************************************************
     * PLUGIN'S ACTIONS MANAGEMENT
     *******************************************************************************************************/
    @Override
    public void onMessage(CobaltPluginWebContainer webContainer, JSONObject message) {
        try{
            String action = message.getString(Cobalt.kJSAction);
            CobaltFragment fragment = webContainer.getFragment();
            String callback = message.getString(Cobalt.kJSCallback);

            if (action.equals(EVENT_GETTOKEN)){
                FcmPlugin.sInstance.fragment = new WeakReference<CobaltFragment>(fragment);
                FcmPlugin.sInstance.callback = callback;
                FcmPlugin.sInstance.onTokenReceived();
            }

            else if (action.equals(EVENT_SUBSCRIBE)){
                JSONObject data = message.getJSONObject(Cobalt.kJSData);
                String topic = data.getString(KEY_TOPIC);

                FirebaseMessaging.getInstance().subscribeToTopic(topic);
                if (callback != null) {
                    fragment.sendCallback(callback, null);
                }
            }

            else if (action.equals(EVENT_UNSUBSCRIBE)){
                JSONObject data = message.getJSONObject(Cobalt.kJSData);
                String topic = data.getString(KEY_TOPIC);

                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic);
                if (callback != null) {
                    fragment.sendCallback(callback, null);
                }
            }
        }
        catch (JSONException e){
            if (Cobalt.DEBUG){
                Log.e(TAG, "Missing action key in message : " + message.toString());
                e.printStackTrace();
            }
        }
    }


    /*******************************************************************************************************
     * TOKEN MANAGEMENT
     *******************************************************************************************************/
    protected void onTokenReceived(){
        if (FcmPlugin.sInstance.token != null && FcmPlugin.sInstance.callback != null && FcmPlugin.sInstance.fragment.get() != null){ //An event getToken was caught

            JSONObject data = new JSONObject();
            try{
                data.put(KEY_TOKEN, FcmPlugin.sInstance.token);
                FcmPlugin.sInstance.fragment.get().sendCallback(FcmPlugin.sInstance.callback, data);
                FcmPlugin.sInstance.callback = null;
                FcmPlugin.sInstance.fragment = null;
            }
            catch (JSONException e){
                if (Cobalt.DEBUG){
                    Log.e(TAG, "An unexpected error has occurs while creating json's object data for token");
                    e.printStackTrace();
                }
            }
        }
    }


    /*******************************************************************************************************
     * INTERFACE BETWEEN APP AND FCM
     *******************************************************************************************************/
    public static void onMessageReceived(RemoteMessage remoteMessage){
        FcmPluginMessagingService.onMessageReceived(remoteMessage, FcmPlugin.sInstance.context);
    }

    public static void onTokenRefresh(){
        FcmPluginInstanceIdService.onTokenRefresh();
    }
}
