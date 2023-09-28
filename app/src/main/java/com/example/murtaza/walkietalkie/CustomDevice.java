package com.example.murtaza.walkietalkie;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.View;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomDevice {
    private int id;
    private String deviceName;
    private WifiP2pDevice device;
    private View iconView;

    @Override
    public String toString() {
        return deviceName;
    }
}