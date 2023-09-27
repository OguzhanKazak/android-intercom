package com.example.murtaza.walkietalkie;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int SEPERATION_DIST_THRESHOLD = 50;

    private static int device_count = 0;
    ImageView centerDeviceIcon;

    ListView peerListView;
    ArrayAdapter<CustomDevice> peerListViewAdapter;

    ArrayList<Point> device_points = new ArrayList<>();

    TextView connectionStatus;
    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    public static final int PORT_USED = 9584;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    ArrayList<CustomDevice> customPeers = new ArrayList<>();

    ServerClass serverClass;
    ClientClass clientClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissionsIfRequired();
        initialSetup();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    @SuppressLint("MissingPermission") //already asking for permission in requestPermissionsIfRequired method
    @Override
    public void onClick(View v) {

        if (checkIfClickedToPair(v.getId())) {
            int idx = getIndexFromIdPeerList(v.getId());
            final WifiP2pDevice device = customPeers.get(idx).device;
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;

            requestPermissionsIfRequired();
            mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getApplicationContext(), "Connected to " + device.deviceName, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reason) {
                    Toast.makeText(getApplicationContext(), "Error in connecting to " + device.deviceName, Toast.LENGTH_SHORT).show();
                }
            });
        }
        else {
            startDiscovery(v);
        }
    }

    WifiP2pManager.PeerListListener peerListListener = peersList -> {
        Log.d("PEER_DISCOVERY", "Listener called, available device count: "+peersList.getDeviceList().size());
        if(peersList.getDeviceList().size() != 0){

            // first make a list of all devices already present
            ArrayList<CustomDevice> deviceAlreadyPresent = new ArrayList<>();

            for(WifiP2pDevice device : peersList.getDeviceList()){
                Log.d("PEER_DISCOVERY", "available device: "+device.deviceName);
                int idx = checkPeerListByName(device.deviceName);
                if(idx != -1){
                    // device already in list
                    deviceAlreadyPresent.add(customPeers.get(idx));
                }
            }

            if(deviceAlreadyPresent.size() == peersList.getDeviceList().size()){
                // all discovered devices already present
                return;
            }

            // this will remove all devices no longer in range
            clearPeerList();
            // add all devices in range
            deviceAlreadyPresent.forEach(this::addToPeerList);


            for(WifiP2pDevice device : peersList.getDeviceList()) {
                if (checkPeerListByName(device.deviceName) == -1) {
                    // device not already present
                    View newDevice = createNewDevice(device.deviceName);
                    CustomDevice customDevice = new CustomDevice();
                    customDevice.deviceName = device.deviceName;
                    customDevice.id = newDevice.getId();
                    customDevice.device = device;
                    customDevice.icon_view = newDevice;

                    addToPeerList(customDevice);
                }
            }
        }

        if(peersList.getDeviceList().size() == 0){
            Toast.makeText(getApplicationContext(), "No Peers Found", Toast.LENGTH_SHORT).show();
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            final InetAddress groupOwnerAddress = info.groupOwnerAddress;

            if(info.groupFormed && info.isGroupOwner){
                connectionStatus.setText("HOST");
                serverClass = new ServerClass();
                serverClass.start();
            }else if(info.groupFormed){
                connectionStatus.setText("CLIENT");
                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();
            }
        }
    };

    private void initialSetup() {
        // layout files

        connectionStatus = findViewById(R.id.connectionStatus);
        centerDeviceIcon = findViewById(R.id.myImageView);
        peerListView = findViewById(R.id.dynamic_peer_list_view);

        peerListViewAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1,
                customPeers);

        peerListView.setAdapter(peerListViewAdapter);
        peerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onClick(view);
            }
        });
        // add onClick Listeners
        centerDeviceIcon.setOnClickListener(this);

        // center button position
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        device_points.add(new Point(size.x / 2, size.y / 2));

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    public class ServerClass extends Thread {
        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(PORT_USED);
                socket = serverSocket.accept();

                SocketHandler.setSocket(socket);

                startActivity(new Intent(getApplicationContext(), ChatWindow.class));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ClientClass extends Thread {
        Socket socket;
        String hostAddress;

        ClientClass(InetAddress address) {
            this.socket = new Socket();
            this.hostAddress = address.getHostAddress();
        }

        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAddress, PORT_USED), 500);

                SocketHandler.setSocket(socket);

                startActivity(new Intent(getApplicationContext(), ChatWindow.class));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startDiscovery(View v) {
        if (v.getId() == R.id.myImageView) {
            checkLocationEnabled();
            discoverDevices();
        }
    }

    private void checkLocationEnabled() {
        LocationManager lm = (LocationManager) MainActivity.this.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }

        if (!gps_enabled && !network_enabled) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.gps_network_not_enabled_title)
                    .setMessage(R.string.gps_network_not_enabled)
                    .setPositiveButton(R.string.open_location_settings, (paramDialogInterface, paramInt) -> MainActivity.this.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .show();
        }
    }

    private boolean checkIfClickedToPair(int id) {
        return getIndexFromIdPeerList(id) != -1;
    }

    private void addToPeerList(CustomDevice device) {
        customPeers.add(device);
        peerListViewAdapter.notifyDataSetChanged();
    }

    private void clearPeerList() {
        customPeers.clear();
        peerListViewAdapter.notifyDataSetChanged();
    }

    private int getIndexFromIdPeerList(int id) {
        for (CustomDevice d : customPeers) {
            if (d.id == id) {
                return customPeers.indexOf(d);
            }
        }
        return -1;
    }

    private int checkPeerListByName(String deviceName) {
        for (CustomDevice d : customPeers) {
            if (d.deviceName.equals(deviceName)) {
                return customPeers.indexOf(d);
            }
        }
        return -1;
    }

    @SuppressLint("MissingPermission")
    private void discoverDevices() {
        requestPermissionsIfRequired();
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                connectionStatus.setText(getString(R.string.discovery_started));
            }

            @Override
            public void onFailure(int reason) {
                connectionStatus.setText(getString(R.string.discovery_failed));
            }
        });
    }

    boolean checkPositionOverlap(Point new_p){
    //  if overlap, then return true, else return false
        if(!device_points.isEmpty()){
            for(Point p:device_points){
                int distance = (int)Math.sqrt(Math.pow(new_p.x - p.x, 2) + Math.pow(new_p.y - p.y, 2));
                Log.d(TAG, distance+"");
                if(distance < SEPERATION_DIST_THRESHOLD){
                    return true;
                }
            }
        }
        return false;
    }

    public View createNewDevice(String device_name){
        View device1 = LayoutInflater.from(this).inflate(R.layout.device_icon, null);

        TextView txt_device1 = device1.findViewById(R.id.myImageViewText);
        int device_id = (int)System.currentTimeMillis() + device_count++;
        txt_device1.setText(device_name);
        device1.setId(device_id);
        device1.setOnClickListener(this);

        device1.setVisibility(View.INVISIBLE);
        return device1;
    }

    private void requestPermissionsIfRequired() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED)) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.NEARBY_WIFI_DEVICES}, 1);
        }
    }

}

class CustomDevice{
    int id;
    String deviceName;
    WifiP2pDevice device;
    View icon_view;
    CustomDevice(){

    }

    @Override
    public String toString() {
        return deviceName;
    }
}