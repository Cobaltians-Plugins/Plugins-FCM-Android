package io.kristal.fcmplugin;

import android.content.Context;
import android.util.Log;
import android.app.Notification;


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
    protected void setToken(String token) { this.token = token;}
    protected String getToken(){ return this.token;}

    protected Context getContext() { return this.context; }
    public void setContext(Context context){this.context = context;}

    void setFragment(CobaltFragment fragment){ this.fragment = new WeakReference<CobaltFragment>(fragment); }
    CobaltFragment getFragment(){ return this.fragment.get(); }

    void setCallback(String callback) { this.callback = callback; }
    String getCallback(){ return this.callback; }


    /*******************************************************************************************************
     * SINGLETON
     *******************************************************************************************************/
    protected static FcmPlugin sInstance;

    public static FcmPlugin getInstance(){
        if (sInstance == null){
            sInstance = new FcmPlugin();
        }
        return sInstance;
    }

    // getInstance used by the CobaltPluginManager
    public static FcmPlugin getInstance(CobaltPluginWebContainer webContainer){
        if (sInstance == null){
            sInstance = new FcmPlugin(webContainer);
        }
        return sInstance;
    }

    private FcmPlugin(){
        this.token = FirebaseInstanceId.getInstance().getToken(); //Null if token not yet generated, device token else
        this.callback = null;
        this.fragment = new WeakReference<CobaltFragment>(null);
        this.context = null;
    }

    private FcmPlugin(CobaltPluginWebContainer webContainer){
        this.token = FirebaseInstanceId.getInstance().getToken(); //Null if token not yet generated, device token else
        this.callback = null;
        this.fragment = new WeakReference<CobaltFragment>(null);
        this.context = webContainer.getActivity();
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

            if (EVENT_GETTOKEN.equals(action)){
                FcmPlugin.getInstance().setFragment(fragment);
                FcmPlugin.getInstance().setCallback(callback);
                FcmPlugin.getInstance().onTokenReceived();
            }

            else if (EVENT_SUBSCRIBE.equals(action)){
                JSONObject data = message.getJSONObject(Cobalt.kJSData);
                String topic = data.getString(KEY_TOPIC);

                FirebaseMessaging.getInstance().subscribeToTopic(topic);
                if (callback != null) {
                    fragment.sendCallback(callback, null);
                }
            }

            else if (EVENT_UNSUBSCRIBE.equals(action)){
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
        CobaltFragment frag = FcmPlugin.getInstance().getFragment();
        if (FcmPlugin.getInstance().getToken() != null &&
                FcmPlugin.getInstance().getCallback() != null &&
                frag != null){

            JSONObject data = new JSONObject();
            try{
                data.put(KEY_TOKEN, FcmPlugin.sInstance.token);
                frag.sendCallback(FcmPlugin.getInstance().getCallback(), data);
                FcmPlugin.getInstance().setCallback(null);
                FcmPlugin.getInstance().setFragment(null);
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
        if (FcmPlugin.getInstance().getContext() != null){
            FcmPluginMessagingService.onMessageReceived(remoteMessage, FcmPlugin.getInstance().getContext());
        }
    }

    public static void onMessageReceived(Notification notif) {
        if (FcmPlugin.getInstance().getContext() != null){
            FcmPluginMessagingService.onMessageReceived(notif, FcmPlugin.getInstance().getContext());
        }
    }

    public static void onTokenRefresh(){
        FcmPluginInstanceIdService.onTokenRefresh();
    }
}
