package ir.paadino.scheduling.net;

/**
 * Created by Reza Kiani on 25/11/2017.
 */

import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import static ir.paadino.scheduling.storage.Constants.SERVER_IP;
import static ir.paadino.scheduling.storage.Constants.SERVER_PORT;

public class TcpClient {

    private static final String TAG = TcpClient.class.getSimpleName();
    // message to send to the server
    private String mServerMessage;
    // sends message received notifications
    private OnClientConnected mClientConnected = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages
    private PrintWriter mBufferOut;
    // used to read messages from the server
    private BufferedReader mBufferIn;
    private Socket socket;
    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TcpClient() {
        mServerMessage = "";
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(String message) {
        if (mBufferOut != null && !mBufferOut.checkError()) {
            mBufferOut.println(message);
            mBufferOut.flush();
        }
    }

    public boolean isConnected()
    {
        if(socket != null && socket.isConnected()) return true;
        return false;
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {
        Log.d("TcpClient", "stopping...");
        mRun = false;
    }

    public void run() {
        mRun = true;
        char c = 0;

        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

            Log.e("TCP Client", "C: Connecting...");

            try {
                //create a socket to make the connection with the server
                socket = new Socket(serverAddr, SERVER_PORT);

                //sends the message to the server
                mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                //receives the message which the server sends back
                mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                Log.d("TCP Client", "C: Connected.");
                if(mClientConnected != null) mClientConnected.clientConnected();

                while (mRun) {
                    while(mBufferIn.ready() && c != 10 && c != 13)
                    {
                        c = (char)mBufferIn.read();
                        mServerMessage = mServerMessage.concat(String.valueOf(c));
                    }

                    if (mServerMessage != null && mServerMessage.trim().length() > 0 && mClientConnected != null) {
                        //call the method messageReceived from MyActivity class
                        mClientConnected.messageReceived(mServerMessage);
                    }
                    mServerMessage = "";
                    c = 0;
                }
            } catch (Exception e) {

                Log.e("TCP", "S: Error", e);
                if(mClientConnected != null) mClientConnected.socketError(e.getMessage());
            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                socket.close();
                Log.d("TcpClient", "close...");

                if(mClientConnected != null) mClientConnected.clientDisconnected();

                if (mBufferOut != null) {
                    mBufferOut.flush();
                    mBufferOut.close();
                }

                mBufferIn = null;
                mBufferOut = null;
                mServerMessage = null;
                socket = null;
            }

        } catch (Exception e) {

            Log.e("TCP", "C: Error", e);

        }

    }

    //TODO combine these functions to one...

    public void setOnClientConnected(OnClientConnected onClientConnected) {
        this.mClientConnected = onClientConnected;
    }

    public interface OnClientConnected {
        void clientConnected();
        void clientDisconnected();
        void socketError(String error);
        void messageReceived(String message);
    }
}
