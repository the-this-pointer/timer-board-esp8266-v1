package ir.paadino.scheduling.net;


import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MixerPaad {
    private static final String PING = "{\"action\": \"ping\"}";
    private static final String SET_DATA = "{\"action\": \"set_data\", \"param\":%1}"; //%1 is json
    private static final String SET_TIME = "{\"action\": \"set_time\", \"hour\":%1, \"minute\":%2, \"wday\":%3}";
    private static final String SET_SSID = "{\"action\": \"set_ssid\", \"param\":\"%1\"}";
    private static final String SET_PASS = "{\"action\": \"set_pass\", \"param\":\"%1\"}";

    private static final String DATA = "{\"action\": \"data\"}";
    private static final String PASS = "{\"action\": \"pass\"}";
    private static final String TIME = "{\"action\": \"time\"}";

    private static final String TAG = MixerPaad.class.getSimpleName();
    private ConnectTask mConnectTask;
    private MixerPaadListener mMixerPaadListener;

    private boolean mPingSent;
    private long mSendTime;
    private long mRecvTime;

    private String mBuffer;

    private String mDevicePass = "000000";

    private Handler mHandler;
    public MixerPaad(MixerPaadListener mixerPaadListener) {
        mMixerPaadListener = mixerPaadListener;
        mHandler = new Handler();
        mBuffer = "";
    }

    public void connect() {
        Log.d(TAG, "MixerPaad connect");
        if(mConnectTask != null) {
            mConnectTask.disconnect();
            mConnectTask = null;
        }
        mPingSent = false;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mConnectTask = new ConnectTask();
                mConnectTask.setOnClientConnected(new TcpClient.OnClientConnected() {
                    @Override
                    public void clientConnected() {
                        Log.d(TAG, "Socket Connected!");
                        mMixerPaadListener.onConnected();
                        
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(mPingSent) {
                                    //Ping time is big
                                    mMixerPaadListener.onError("Too much ping time passed...");
                                    disconnect();
                                }
                                sendData(PING, true);
                                if (isConnected())
                                    mHandler.postDelayed(this, 20000);
                            }
                        }, 10000);  //the time is in miliseconds
                    }

                    @Override
                    public void clientDisconnected() {
                        Log.d(TAG, "Socket Disconnected!");
                        mMixerPaadListener.onDisconnected();
                    }

                    @Override
                    public void socketError(String error) {
                        Log.d(TAG, "Socket error occured: " + error);
                        mMixerPaadListener.onError(error);
                    }

                    @Override
                    public void messageReceived(String message) {
                        Log.d(TAG, "messageReceived: "+message);
                        mBuffer += message;
                        try {
                            JSONObject jObject = new JSONObject(mBuffer);
                            String jAction = jObject.optString("action");
                            String jParam1 = null, jParam2 = null, jParam3 = null;
                            if (jObject.has("param1"))
                                jParam1 = jObject.getString("param1").trim();
                            if (jObject.has("param2"))
                                jParam2 = jObject.getString("param2").trim();
                            if (jObject.has("param3"))
                                jParam3 = jObject.getString("param3").trim();
                            switch (jAction) {
                                case "pong":
                                    if(!mPingSent) break;
                                    mRecvTime = System.currentTimeMillis();
                                    mPingSent = false;
                                    mMixerPaadListener.ping(mRecvTime - mSendTime);
                                    break;
                                case "set_data":
                                    mMixerPaadListener.onDataSaved();
                                    break;
                                case "set_time":
                                    mMixerPaadListener.onTimeSaved();
                                    break;
                                case "set_ssid":  //no need to this, nodemcu will be reset 
                                    mMixerPaadListener.onSSIDSaved();
                                    break;
                                case "set_pass":  //no need to this, nodemcu will be reset 
                                    mMixerPaadListener.onPasswordSaved();
                                    break;
                                case "dc":
                                    mConnectTask.disconnect();
                                    break;

                                case "pass":
                                    if (jParam1 != null && jParam1.length() > 0)
                                        mDevicePass = jParam1;
                                    mMixerPaadListener.onPasswordReturned(jParam1);
                                    break;
                                case "time":
                                    //TODO
                                    break;
                                default:
                                    if (jObject.has("times")) {
                                        mMixerPaadListener.onDataReceived(mBuffer);
                                    }
                                    break;
                            }
                            mBuffer = "";
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                mConnectTask.connect();
            }
        }, 100);
    }

    public void disconnect() {
        Log.d(TAG, "MixerPaad disconnect");
        if(mConnectTask != null) {
            mConnectTask.disconnect();
            mConnectTask = null;
        }
    }

    public boolean isConnected() {
        return mConnectTask != null && mConnectTask.isConnected();
    }

    private void sendData(String data, boolean ping) {
        if(!isConnected()) return;
        mConnectTask.sendData(data);
        if (ping) {
            mSendTime = System.currentTimeMillis();
            mPingSent = true;
        }
    }

    public void requestForPassword() {
        sendData(PASS, false);
    }

    public void setPassword(String password) {
        sendData(SET_PASS.replace("%1",password), false);
    }

    public String password() {
        return mDevicePass;
    }

    public void requestReadData() {
        sendData(DATA, false);
    }

    public void setData(String data) {
        sendData(SET_DATA.replace("%1",data), false);
    }

    public void requestReadTime() {
        sendData(TIME, false);
    }

    public void setTime(Integer hour, Integer minute, Integer weekDay) {
        sendData(SET_TIME.replace("%1", String.valueOf(hour)).replace("%2", String.valueOf(minute)).replace("%3", String.valueOf(weekDay)), false);
    }

    public void setSsid(String ssid) {
        sendData(SET_SSID.replace("%1", ssid), false);
    }

    public interface MixerPaadListener {
        void onDisconnected();
        void onConnected();
        void onPasswordReturned(String password);
        void onError(String error);

        void ping(long ping);
        void onDataSaved();
        void onDataReceived(String data);
        void onTimeSaved();
        void onSSIDSaved();
        void onPasswordSaved();
    }
}
