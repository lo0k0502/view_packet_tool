package com.example.quardimu;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Set;


public class DeviceListActivity extends AppCompatActivity {


    private static final String TAG = "DeviceListActivity";
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private static final boolean D = true;

    private BluetoothAdapter mBA;
    private ArrayAdapter<String> mNewDevicesAA;

    private AdapterView.OnItemClickListener mDeviceClickListener;

    public DeviceListActivity() {
        mDeviceClickListener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
                mBA.cancelDiscovery();
                String info = ((TextView) v).getText().toString();
                String address = info.substring(info.length() - 17);
                Intent intent = new Intent();
                intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        };
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);
        setResult(Activity.RESULT_CANCELED);
        Button scanButton = findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });
        ArrayAdapter<String> mPairedDevicesAA = new ArrayAdapter<>(this, R.layout.device_name);
        mNewDevicesAA = new ArrayAdapter<>(this, R.layout.device_name);

        ListView pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesAA);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        ListView newDevicesListView = findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesAA);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mBA = BluetoothAdapter.getDefaultAdapter();


            Set<BluetoothDevice> pairedDevices = mBA.getBondedDevices();

            if (pairedDevices.size() > 0) {
                findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
                for (BluetoothDevice device : pairedDevices) {
                    mPairedDevicesAA.add(device.getName() + "\n" + device.getAddress());
                }
            } else {
                String noDevices = getResources().getText(R.string.none_paired).toString();
                mPairedDevicesAA.add(noDevices);
            }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBA != null) {
            mBA.cancelDiscovery();
        }

        this.unregisterReceiver(mReceiver);
    }

    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");

        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        if (mBA.isDiscovering()) {
            mBA.cancelDiscovery();
        }

        mBA.startDiscovery();
    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                assert device != null;
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesAA.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (mNewDevicesAA.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesAA.add(noDevices);
                }
            }
        }
    };



}
