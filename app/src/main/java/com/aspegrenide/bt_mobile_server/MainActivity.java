package com.aspegrenide.bt_mobile_server;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/*
This server runs on the mobile phone. It allows a Google Glass
to connect and access services otherwise not available to the Glqss
1) start server
2) wait for the glass to connect
3) set up a server and wait for requests
*/

public class MainActivity extends AppCompatActivity {

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final int REQUEST_BT_CONNECT_PERMISSION = 3;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "MAIN_ACT";
    private static final int DEACTIVE = 0;
    private static final int ACTIVE = 1;
    private static final int CONNECTED = 2;

    GoogleGlasses googleGlasses = new GoogleGlasses(false, null);
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    ConnectedThread mConnectedThread;

    AcceptThread accept;

    ImageView imgBtStatus;
    TextView tvBtState;
    ImageView imgServerState;
    TextView tvServerState;
    TextView tvGlassState;
    TextView tvGlassSerial;
    ImageView imgVideo;
    TextView tvVideoToken;

    // firebase
    private DatabaseReference mDatabase;
    VideoCallMeta vcm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgBtStatus = findViewById(R.id.imgBtStatus);
        tvBtState = findViewById(R.id.tvBtStatus);
        imgServerState = findViewById(R.id.imgServerState);
        tvServerState = findViewById(R.id.tvServerState);
        tvGlassState = findViewById(R.id.tvGlassStatus);
        tvGlassSerial = findViewById(R.id.tvGlassSerial);
        imgVideo = findViewById(R.id.imgVideo);
        tvVideoToken = findViewById(R.id.tvVideoTokenFound);

        // firebase
        mDatabase = FirebaseDatabase.getInstance().getReference("videocall");

        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                vcm = dataSnapshot.getValue(VideoCallMeta.class);
                Log.d(TAG, "vcm = " + vcm.toString());
                if(vcm.getCurrentToken() != null){
                    imgVideo.setImageResource(R.drawable.vidblue);
                    tvVideoToken.setText("Active token for video channel located");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };
        mDatabase.addValueEventListener(postListener);

        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        // update the icon for server
        setServerState(DEACTIVE, null);

        // update the icon for glass
        setGlassState(googleGlasses);


        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermission(Manifest.permission.BLUETOOTH_CONNECT, REQUEST_BT_CONNECT_PERMISSION);
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if (bluetoothAdapter.isEnabled()) {
            imgBtStatus.setImageResource(R.drawable.bton);
            tvBtState.setText("Active");
        }
        startServer();
    }


    public boolean sendMessage(String message) {
        // if glass is connected
        if(googleGlasses.isConnected()) {
            Log.d(TAG, "send message to glasses:" + message);
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            mConnectedThread.write(bytes);

            return true;
        }
        Log.d(TAG, "send message to glasses FAILED:" + message);
        return false;
    }

    private void setServerState(int state, String deviceName) {
        switch (state) {
            case DEACTIVE:
                tvServerState.setText("Not active");
                imgServerState.setImageResource(R.drawable.shh);
                break;
            case ACTIVE:
                tvServerState.setText("Active");
                imgServerState.setImageResource(R.drawable.ok);
                break;
            case CONNECTED:
                imgServerState.setImageResource(R.drawable.handshake);
                tvServerState.setText("Connected to " + deviceName);
                break;
        }
    }

    private void setGlassState(GoogleGlasses gg) {
        if (gg.isConnected()) {
            tvGlassState.setText("Serial: ");
            tvGlassSerial.setText(gg.getUid());
        } else {
            tvGlassState.setText("Serial: ");
            tvGlassSerial.setText("na");
        }
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        tvBtState.setText("Deactive");
                        imgBtStatus.setImageResource(R.drawable.btoff);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        tvBtState.setText("Deactivating ...");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        tvBtState.setText("Active");
                        imgBtStatus.setImageResource(R.drawable.bton);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        tvBtState.setText("Activating ...");
                        break;
                }
            }
        }
    };

    public void restartServer(View view) {
        setServerState(DEACTIVE, null);
        setGlassState(googleGlasses);
        accept.cancel();
        startServer();
    }

    public void startServer() {
        accept = new AcceptThread();
        accept.start();
    }

    private void connected(BluetoothSocket mmSocket) {
        Log.d(TAG, "connected: Starting.");
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.BLUETOOTH_CONNECT, REQUEST_BT_CONNECT_PERMISSION);
        }
        String deviceName = mmSocket.getRemoteDevice().getName();
        String serial = mmSocket.getRemoteDevice().toString();
        Log.d(TAG, "remotedevice to string:" + serial);
        // looking for , deviceName

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setServerState(CONNECTED, deviceName);
                googleGlasses.setConnected(true);
                googleGlasses.setUid(deviceName);
                setGlassState(googleGlasses);
            }
        });

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    public void imgVideoOnClick(View view) {
        String prefix = "TOKEN";
        String payload = vcm.getCurrentToken();
        Log.d(TAG, "imgVideoOnClick token" + payload);
        sendMessage(prefix + payload);
    }


    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null ;
            // Create a new listening server socket
            try{
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    requestPermission(Manifest.permission.BLUETOOTH_CONNECT, REQUEST_BT_CONNECT_PERMISSION);
                }
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("appname", MY_UUID_INSECURE);

                Log.d(TAG, "AcceptThread: Setting up Server using: " + MY_UUID_INSECURE);
            }catch (IOException e){
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
            }
            mmServerSocket = tmp;
        }

        public void run(){
            Log.d(TAG, "run: AcceptThread Running.");
            BluetoothSocket socket = null;

            try{
                // This is a blocking call and will only return on a
                // successful connection or an exception
                Log.d(TAG, "run: RFCOM server socket start.....");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setServerState(ACTIVE, null);
                    }
                });
                socket = mmServerSocket.accept();
                Log.d(TAG, "run: RFCOM server socket accepted connection.");

            }catch (IOException e){
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
            }

            //talk about this is in the 3rd
            if(socket != null){
                connected(socket);
            }
            Log.i(TAG, "END mAcceptThread ");
        }

        public void cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread.");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage() );
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                try {
                    bytes = mmInStream.read(buffer);
                    final String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // view_data.setText(incomingMessage);
                            Toast.makeText(MainActivity.this, "incomingMessage: " + incomingMessage, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "incomingMessage: " + incomingMessage);
                        }
                    });
                } catch (IOException e) {
                    Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage());
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage());
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
}