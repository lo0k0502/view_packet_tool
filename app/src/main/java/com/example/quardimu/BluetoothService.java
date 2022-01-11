package com.example.quardimu;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static com.example.quardimu.MainActivity.MESSAGE_READ;


public class BluetoothService {

    private static String TAG = "BluetoothService";

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String NAME = "BluetoothData";
    private final BluetoothAdapter mBluetoothAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState = STATE_NONE;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private String mAddress;


    public BluetoothService(Handler handler) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
    }

    public boolean isConnected(){
        return mState == STATE_CONNECTED;
    }


    private synchronized void setState(int state) {
        mState = state;
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE,state,-1);
        Bundle bundle = new Bundle();
        bundle.putString("device_MAC", mAddress);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    public synchronized void start() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    public synchronized void connect(BluetoothDevice device, String address) {

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        mAddress = address;
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket,BluetoothDevice device) {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_ADDRESS);
        Bundle bundle = new Bundle();
        bundle.putString("device_MAC", device.getAddress());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        setState(STATE_NONE);
    }

    private void connectionFailed() {
        setState(STATE_NONE);
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private void connectionLost() {
        setState(STATE_NONE);
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException ignored) {}
            mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread");
            BluetoothSocket socket;

            while (mState != STATE_CONNECTED) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }

                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException err) {
                                    err.printStackTrace();
                                    Log.e("IO","IO"+err);
                                }
                                break;
                        }
                    }
                }

            }
        }

        public void cancel() {

            try {
                mmServerSocket.close();
            }
            catch (IOException err) {
                err.printStackTrace();
                Log.e("IO","IO"+err);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            }
            catch (IOException ignored) {}
            mmSocket = tmp;
        }

        public void run() {

            setName("ConnectThread");
            mBluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            }
            catch (IOException e) {
                connectionFailed();
                try {
                    mmSocket.close();
                } catch (IOException ignored) {}

                BluetoothService.this.start();
                return;
            }

            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }
            connected(mmSocket, mmDevice);
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException err) {
                err.printStackTrace();
                Log.e("IO","IO"+err);
            }
        }

    }
    class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException ignored) {}
            mmInStream = tmpIn;
        }

        private float [] fData=new float[9];
        @SuppressLint("DefaultLocale")
        public void run() {
            byte[] buffer = new byte[200];
            byte[] packBuffer = new byte[11];
            int acceptedLen;

            boolean start = false;
            int count = 0;

            while (true) {
                try {
                    acceptedLen = mmInStream.read(buffer);
                    if (acceptedLen > 0) {
                        for (int i=0; i < acceptedLen; i++){
                            if (buffer[i] == 0x55 && !start) {
                                start = true;
                                count = 0;
                            }
                            if (start){
                                packBuffer[count] = buffer[i];
                                count++;
                            }
                            if (count > 10){
                                start = false;
                                writeMessage(packBuffer);
                            }
                        }
                    }

                } catch (IOException err) {
                    connectionLost();
                    break;
                }
                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void writeMessage(byte[] packBuffer){
            switch (packBuffer[1]) {
                case 0x51:
                    fData[0] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff)) / 32768.0f * 16;
                    fData[1] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff)) / 32768.0f * 16;
                    fData[2] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 32768.0f * 16;
                    break;
                case 0x52:
                    fData[3] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff)) / 32768.0f * 2000;
                    fData[4] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff)) / 32768.0f * 2000;
                    fData[5] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 32768.0f * 2000;
                    break;
                case 0x53:
                    fData[6] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff)) / 32768.0f * 180;
                    fData[7] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff)) / 32768.0f * 180;
                    fData[8] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 32768.0f * 180;
                    break;
            }
            Message msg = mHandler.obtainMessage(MESSAGE_READ);
            Bundle bundle = new Bundle();
            bundle.putFloatArray("Data", fData);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException err) {
                err.printStackTrace();
                Log.e("IO","IO"+err);
            }
        }
    }

}



