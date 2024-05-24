package ir.paadino.scheduling.net;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by Reza Kiani on 07/02/2018.
 */

public class ConnectTask extends AsyncTask<String, String, TcpClient> {

    private static final String TAG = ConnectTask.class.getSimpleName();
    private TcpClient mTcpClient;
    private TcpClient.OnClientConnected onClientConnected;
    @Override
    protected TcpClient doInBackground(String... message) {


        mTcpClient = new TcpClient();
        mTcpClient.setOnClientConnected(onClientConnected);
        mTcpClient.run();

        Log.d(TAG, "doInBackground ending...");
        return null;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }

    public boolean sendData(String data)
    {
        if(!this.isConnected()) return false;
        if (mTcpClient != null) {
            mTcpClient.sendMessage(data);
            return true;
        }
        return false;
    }

    public void disconnect()
    {
        // disconnect
        if (mTcpClient != null) {
            mTcpClient.stopClient();
            mTcpClient = null;
        }
    }

    public void setOnClientConnected(TcpClient.OnClientConnected onClientConnected) {
        this.onClientConnected = onClientConnected;
    }

    public void connect()
    {
        if (mTcpClient == null) {
           this.execute("");
        }
    }

    public boolean isConnected()
    {
        if (mTcpClient == null) return false;
        return mTcpClient.isConnected();
    }
}