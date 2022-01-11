package com.example.quardimu;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.example.quardimu.ui.SectionsPagerAdapter;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.quardimu.Matrix.getRelateAngle;
import static com.example.quardimu.Matrix.getVector;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final static String TAG = "MainActivity";

    //Bluetooth
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothService mBluetoothServiceLT = null;
    private BluetoothService mBluetoothServiceLC = null;
    private BluetoothService mBluetoothServiceRT = null;
    private BluetoothService mBluetoothServiceRC = null;

    //GPS
    private LocationSettingsRequest.Builder mBuilder;
    private static Location mLocation;
    private static LocationService mLocationService;
    private boolean mBound = false;
    private LocationRequest mLocationRequest;

    //Parameters
    public static final int REQUEST_LT = 0;
    public static final int REQUEST_LC = 1;
    public static final int REQUEST_RT = 2;
    public static final int REQUEST_RC = 3;
    private static final int REQUEST_CHECK_SETTINGS = 4;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_ADDRESS = 4;
    public static final int MESSAGE_TOAST = 5;

    //UI
    private ToggleButton mToggleLT;
    private ToggleButton mToggleLC;
    private ToggleButton mToggleRT;
    private ToggleButton mToggleRC;

    //IO
    private static boolean mRecordStatus = false;
    private OutputStream mOutputStreamRaw;
    private static OutputStream mOutputStreamMap;
    private static long mStartTime;
    private static final String mOutputFileFormatRaw = "#acc_x\tacc_y\tacc_z\tw_x\tw_y\tw_z\tth_x\tth_y\tth_z\tleft_knee_max\tright_knee_max\tgps_la\tgps_lo\tgps_sp\tgps_ac\ttime\n";
    private static final String mOutputFileFormatMap = "#gps_la\tgps_lo\tgps_sp\tgps_ac\tleft_knee_max\tright_knee_max\ttime\n";
    public static final String mOutputFileDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/QuadIMU_data/";
    private static String[] mPermissionList = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    //Data
    private String[] mData = new String[4];
    private static DataViewModel mDataViewModel;
    private float mLeftKneeAngle = 0;
    private float mRightKneeAngle = 0;
    private static float mLeftKneeAngleMax = 0;
    private static float mRightKneeAngleMax = 0;

    //Preferences
    private float[] mCalZ = new float[4];
    private int mIndexVector = 1;

    //Notification
    private static long mMapUpdateTime;
    private long mBluetoothUpdateTime;
    private Timer mRecordingTimer;
    private NotificationManagerCompat mNotificationManagerCompat;
    private int REC_ID = 1;
    private NotificationCompat.Builder mNotifyBuilder;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG,"Storage location: " + mOutputFileDir);

        setContentView(R.layout.activity_main);

        //ViewPager
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        //Preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mCalZ[REQUEST_LT] = stringGetFloat(sharedPreferences.getString("calZ_LT","0"));
        mCalZ[REQUEST_LC] = stringGetFloat(sharedPreferences.getString("calZ_LC","0"));
        mCalZ[REQUEST_RT] = stringGetFloat(sharedPreferences.getString("calZ_RT","0"));
        mCalZ[REQUEST_RC] = stringGetFloat(sharedPreferences.getString("calZ_RC","0"));
        mIndexVector = Integer.parseInt(sharedPreferences.getString("mIndexVector","1"));

        //Bottom Sheet
        final LinearLayout toggle_button_layout = findViewById(R.id.toggleButton_layout);
        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setAlpha(0.5f);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (toggle_button_layout.getVisibility() == View.GONE) {
                    toggle_button_layout.setVisibility(View.VISIBLE);
                    fab.setAlpha(1f);
                } else {
                    toggle_button_layout.setVisibility(View.GONE);
                    fab.setAlpha(0.5f);
                }
            }
        });
        toggle_button_layout.setVisibility(View.GONE);
        fab.setAlpha(0.5f);
        mToggleLT = findViewById(R.id.toggleButton_left_thigh);
        mToggleLC = findViewById(R.id.toggleButton_left_calf);
        mToggleRT = findViewById(R.id.toggleButton_right_thigh);
        mToggleRC = findViewById(R.id.toggleButton_right_calf);

        //Permission
        if (checkStoragePermissions() || !checkLocationPermissions()) {
            showPermissionAlert(mPermissionList);
        }

        //Bluetooth Connection
        mDataViewModel = new ViewModelProvider(this).get(DataViewModel.class);
        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                setSnackBar("BLT unavailable");
            }
        } catch (Exception err) {
            err.printStackTrace();
            Log.e("IO", "IO" + err);
        }
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            }
        }

        mToggleLT.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    try {
                        if (!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();
                        if (mBluetoothServiceLT == null)
                            mBluetoothServiceLT = new BluetoothService(mHandlerLT);
                        Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                        startActivityForResult(serverIntent, REQUEST_LT);
                    } catch (Exception err) {
                        err.printStackTrace();
                        Log.e("IO", "IO" + err);
                    }
                } else {
                    if (mBluetoothServiceLT != null) {
                        mBluetoothServiceLT.stop();
                        setSnackBar("BLT not connected");
                    }
                    mBluetoothServiceLT = null;
                }
                if (mBluetoothServiceLT == null) mDataViewModel.setData(REQUEST_LT, null);
            }
        });

        mToggleLC.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    try {
                        if (!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();
                        if (mBluetoothServiceLC == null)
                            mBluetoothServiceLC = new BluetoothService(mHandlerLC);
                        Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                        startActivityForResult(serverIntent, REQUEST_LC);
                    } catch (Exception err) {
                        err.printStackTrace();
                        Log.e("IO", "IO" + err);
                    }
                } else {
                    if (mBluetoothServiceLC != null) {
                        mBluetoothServiceLC.stop();
                        setSnackBar("BLT not connected");
                    }
                    mBluetoothServiceLC = null;
                }
                if (mBluetoothServiceLC == null) mDataViewModel.setData(REQUEST_LC, null);
            }
        });

        mToggleRT.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    try {
                        if (!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();
                        if (mBluetoothServiceRT == null)
                            mBluetoothServiceRT = new BluetoothService(mHandlerRT);
                        Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                        startActivityForResult(serverIntent, REQUEST_RT);
                    } catch (Exception err) {
                        err.printStackTrace();
                        Log.e("IO", "IO" + err);
                    }
                } else {
                    if (mBluetoothServiceRT != null) {
                        mBluetoothServiceRT.stop();
                        setSnackBar("BLT not connected");
                    }
                    mBluetoothServiceRT = null;
                }
                if (mBluetoothServiceRT == null) mDataViewModel.setData(REQUEST_RT, null);

            }
        });

        mToggleRC.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    try {
                        if (!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();
                        if (mBluetoothServiceRC == null)
                            mBluetoothServiceRC = new BluetoothService(mHandlerRC);
                        Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                        startActivityForResult(serverIntent, REQUEST_RC);
                    } catch (Exception err) {
                        err.printStackTrace();
                        Log.e("IO", "IO" + err);
                    }
                } else {
                    if (mBluetoothServiceRC != null) {
                        mBluetoothServiceRC.stop();
                        setSnackBar("BLT not connected");
                    }
                    mBluetoothServiceRC = null;
                }
                if (mBluetoothServiceRC == null) mDataViewModel.setData(REQUEST_RC, null);
            }
        });


        mBuilder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkLocationPermissions();
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            mDataViewModel.setLocation(location);
                        }
                    }
                });

        checkLocationSetting(mBuilder);
        createNotification();
    }



    public void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        bindService(serviceIntent,connection,Context.BIND_AUTO_CREATE);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            LocationService.LocationBinder binder = (LocationService.LocationBinder) service;
            mLocationService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        stopService(serviceIntent);
    }

    public static void requestLocation(){
        mDataViewModel.setLocation(mLocationService.getLocation());
        mLocation = mLocationService.getLocation();
        if(mRecordStatus) {
            writeToFileMap();
        }
    }
    private void createNotification() {
        String CHANNEL_ID = "QuardIMU_recording";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mNotifyBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_radio_button_checked_24)
                .setContentTitle("Recording Service")
                .setContentText("Ready");
        mNotificationManagerCompat = NotificationManagerCompat.from(this);
        mNotificationManagerCompat.notify(REC_ID, mNotifyBuilder.build());
    }

    private void startRecordingNotification(){
        mNotifyBuilder.setContentText("Recording...");
        mNotificationManagerCompat.notify(REC_ID, mNotifyBuilder.build());
        mRecordingTimer = new Timer();
        mRecordingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateRecordingNotification();
            }
        },1000,1000);
    }

    private void stopRecordingNotification(){
        mRecordingTimer.cancel();
        String mesg = "Standby";
        mNotifyBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(mesg));
        mNotificationManagerCompat.notify(REC_ID,mNotifyBuilder.build());
    }

    private void updateRecordingNotification(){
        String mesg = "Up time: " + (System.currentTimeMillis() - mStartTime) / 1000 + "s\n" +
                "Last Map write: " + (System.currentTimeMillis() - mMapUpdateTime) + "ms\n" +
                "Last BTL write: " + (System.currentTimeMillis() - mBluetoothUpdateTime) + "ms\n";
        mNotifyBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(mesg));
        mNotificationManagerCompat.notify(REC_ID,mNotifyBuilder.build());
    }


    //Activity methods
    public void setSnackBar(String str) {
        Snackbar.make(findViewById(R.id.snackbarLayout), str, Snackbar.LENGTH_SHORT).show();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        stopLocationService();
        if (mBluetoothServiceLT != null)
            mBluetoothServiceLT.stop();
        if (mBluetoothServiceLC != null)
            mBluetoothServiceLC.stop();
        if (mBluetoothServiceRT != null)
            mBluetoothServiceRT.stop();
        if (mBluetoothServiceRC != null)
            mBluetoothServiceRC.stop();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.action_button,menu);
        menuInflater.inflate(R.menu.settings_menu,menu);
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mCalZ[REQUEST_LT] = stringGetFloat(sharedPreferences.getString("calZ_LT","0"));
        mCalZ[REQUEST_LC] = stringGetFloat(sharedPreferences.getString("calZ_LC","0"));
        mCalZ[REQUEST_RT] = stringGetFloat(sharedPreferences.getString("calZ_RT","0"));
        mCalZ[REQUEST_RC] = stringGetFloat(sharedPreferences.getString("calZ_RC","0"));
        mIndexVector = Integer.parseInt(sharedPreferences.getString("mIndexVector","1"));
    }

    private float stringGetFloat(String str){
        if (str.isEmpty()){
            return 0;
        }else{
            return Float.parseFloat(str);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        if (id == R.id.action_recording_icon) {

            Log.d(TAG, mOutputFileDir);
            Boolean bool = checkStoragePermissions();
            Log.d(TAG, bool.toString());

            if (checkStoragePermissions()) {
                showPermissionAlert(mPermissionList);
            } else {
                if (!mRecordStatus) {
                    String recordFileNameRaw = "RAW_" + sdf.format(new Date());
                    String recordFileNameMap = "MAP_" + sdf.format(new Date());
                    try {
                        File fileDir = new File(mOutputFileDir);
                        if (!fileDir.mkdirs()) {
                            setSnackBar("Cannot mkdir");
                        }
                        mOutputStreamRaw = new FileOutputStream(new File(fileDir, recordFileNameRaw));
                        mOutputStreamMap = new FileOutputStream(new File(fileDir, recordFileNameMap));
                        mOutputStreamRaw.write(mOutputFileFormatRaw.getBytes());
                        mOutputStreamMap.write(mOutputFileFormatMap.getBytes());
                        mStartTime = System.currentTimeMillis();
                        mBluetoothUpdateTime = System.currentTimeMillis();
                        mMapUpdateTime = System.currentTimeMillis();
                        setSnackBar("Start recording");
                        startRecordingNotification();
                        mRecordStatus = true;
                        item.setIcon(R.drawable.ic_baseline_radio_button_checked_24);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {
                    try {
                        mOutputStreamRaw.flush();
                        mOutputStreamRaw.close();
                        mOutputStreamMap.flush();
                        mOutputStreamMap.close();
                        setSnackBar("Records save to " + sdf.format(new Date()));
                        stopRecordingNotification();
                        mRecordStatus = false;
                        item.setIcon(R.drawable.ic_baseline_radio_button_unchecked_24);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return true;
        }
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                if (resultCode == RESULT_OK){
                    checkLocationSetting(mBuilder);
                }
                break;
            case REQUEST_LT:
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    String address;
                    if (data.getExtras() != null) {
                        address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
                        if (mBluetoothServiceLT != null) {
                            mBluetoothServiceLT.connect(bluetoothDevice, address);
                        }
                    }
                } else {
                    mToggleLT.setChecked(false);
                }
                break;
            case REQUEST_LC:
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    String address;
                    if (data.getExtras() != null) {
                        address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
                        if (mBluetoothServiceLC != null) {
                            mBluetoothServiceLC.connect(bluetoothDevice, address);
                        }
                    }
                } else {
                    mToggleLC.setChecked(false);
                }
                break;
            case REQUEST_RT:
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    String address;
                    if (data.getExtras() != null) {
                        address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
                        if (mBluetoothServiceRT != null) {
                            mBluetoothServiceRT.connect(bluetoothDevice, address);
                        }
                    }
                } else {
                    mToggleRT.setChecked(false);
                }
                break;
            case REQUEST_RC:
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    String address;
                    if (data.getExtras() != null) {
                        address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
                        if (mBluetoothServiceRC != null) {
                            mBluetoothServiceRC.connect(bluetoothDevice, address);
                        }
                    }
                } else {
                    mToggleRC.setChecked(false);
                }
                break;
        }
    }

    //permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                showPermissionAlert(mPermissionList);
            } else {
                if (checkLocationPermissions()) {
                    checkLocationSetting(mBuilder);
                }
            }
        }
    }
    private void showPermissionAlert(String[] permissionList) {
        if (!checkLocationPermissions()) {
            ActivityCompat.requestPermissions(MainActivity.this, permissionList, 123);
        }
    }
    private boolean checkLocationPermissions() {
        boolean temp = false;
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            temp = true;
        }
        return temp;
    }
    private boolean checkStoragePermissions() {
        return ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    //File methods
    private void writeToFileRaw(String text_IMU) {
        String msg = text_IMU + "\t" + mLeftKneeAngle + "\t"+ mRightKneeAngle + "\t" + mLocation.getLatitude() + "\t" + mLocation.getLongitude() + "\t" + mLocation.getSpeed() + "\t" + mLocation.getAccuracy() + "\t" + (System.currentTimeMillis() - mStartTime) + "\n";
        try {
            if(mOutputStreamRaw != null) {
                mOutputStreamRaw.write(msg.getBytes());
                mOutputStreamRaw.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mBluetoothUpdateTime = System.currentTimeMillis();
    }
    private static void writeToFileMap() {
        String msg = mLocation.getLatitude() + "\t" + mLocation.getLongitude() + "\t" + mLocation.getSpeed() + "\t" + mLocation.getAccuracy() + "\t" + mLeftKneeAngleMax + "\t" + mRightKneeAngleMax + "\t" + (System.currentTimeMillis() - mStartTime) + "\n";
        try {
            if(mOutputStreamMap != null) {
                mOutputStreamMap.write(msg.getBytes());
                mOutputStreamMap.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMapUpdateTime = System.currentTimeMillis();
        mLeftKneeAngleMax = 0;
        mRightKneeAngleMax = 0;
    }

    //GPS methods
    private void checkLocationSetting(LocationSettingsRequest.Builder builder) {

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                startLocationService();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull final Exception e) {
                if (e instanceof ResolvableApiException) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(getApplicationContext());
                    builder1.setTitle("Continuous Location Request");
                    builder1.setMessage("This request is essential to get location update continuously");
                    builder1.create();
                    builder1.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            try {
                                resolvable.startResolutionForResult(MainActivity.this,
                                        REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                    builder1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setSnackBar("Location update permission not granted");
                        }
                    });
                    builder1.show();
                }
            }
        });
    }


    //Bluetooth methods
    @SuppressLint("HandlerLeak")
    private final Handler mHandlerLT = new Handler() {
        @SuppressLint("DefaultLocale")
        @Override
        public void handleMessage(Message msg) {
            imuMsgHandler(msg,REQUEST_LT);
        }
    };
    //Bluetooth methods
    @SuppressLint("HandlerLeak")
    private final Handler mHandlerLC = new Handler() {
        @SuppressLint("DefaultLocale")
        @Override
        public void handleMessage(Message msg) {
            imuMsgHandler(msg,REQUEST_LC);
        }
    };
    //Bluetooth methods
    @SuppressLint("HandlerLeak")
    private final Handler mHandlerRT = new Handler() {
        @SuppressLint("DefaultLocale")
        @Override
        public void handleMessage(Message msg) {
            imuMsgHandler(msg,REQUEST_RT);
        }
    };
    //Bluetooth methods
    @SuppressLint("HandlerLeak")
    private final Handler mHandlerRC = new Handler() {
        @SuppressLint("DefaultLocale")
        @Override
        public void handleMessage(Message msg) {
            imuMsgHandler(msg,REQUEST_RC);
        }
    };


    @SuppressLint("DefaultLocale")
    private void imuMsgHandler(Message msg, int request){
        String address = null;
        switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                address = msg.getData().getString("device_MAC");
                switch (msg.arg1) {
                    case BluetoothService.STATE_CONNECTED:
                        setSnackBar(address+": Connected");
                        break;
                    case BluetoothService.STATE_CONNECTING:
                        setSnackBar(address+": Connecting");
                        break;
                    case BluetoothService.STATE_LISTEN:
                        break;
                    case BluetoothService.STATE_NONE:
                        setSnackBar(address+": Not-Connected");
                        switch (request){
                            case REQUEST_LT:
                                mToggleLT.setChecked(false);
                                mDataViewModel.setData(REQUEST_LT, null);
                                break;
                            case REQUEST_LC:
                                mToggleLC.setChecked(false);
                                mDataViewModel.setData(REQUEST_LC, null);
                                break;
                            case REQUEST_RT:
                                mToggleRT.setChecked(false);
                                mDataViewModel.setData(REQUEST_RT, null);
                                break;
                            case REQUEST_RC:
                                mToggleRC.setChecked(false);
                                mDataViewModel.setData(REQUEST_RC, null);
                                break;
                        }
                        break;
                }
                break;
            case MESSAGE_READ:
                String text;
                try {
                    float[] fData = msg.getData().getFloatArray("Data");

                    if (fData != null) {
                        text = String.format("%.2f\t", fData[0]) + String.format("%.2f\t", fData[1]) + String.format("%.2f\t", fData[2]);
                        text = text + String.format("%.2f\t", fData[3]) + String.format("%.2f\t", fData[4]) + String.format("%.2f\t", fData[5]);
                        text = text + String.format("%.2f\t", fData[6]) + String.format("%.2f\t", fData[7]) + String.format("%.2f", fData[8] - mCalZ[request]);
                        mDataViewModel.setData(request, text);

                        if (mRecordStatus) {
                            writeToFileRaw("[" + request + "]\t" + text);
                        }
                        mData[request] = text;
                    }
                    if (fData != null) {
                        switch (request) {
                            case REQUEST_LT:
                            case REQUEST_LC:
                                if (mData[REQUEST_LT] != null && mData[REQUEST_LC] != null) {
                                    mLeftKneeAngle = getRelateAngle(getVector(mData[REQUEST_LT], mIndexVector), getVector(mData[REQUEST_LC], mIndexVector));
                                    if (mLeftKneeAngle > mLeftKneeAngleMax && fData[4] > 0) {
                                        mLeftKneeAngleMax = mLeftKneeAngle;
                                    }
                                    mDataViewModel.setDataLeftKneeAngle(mLeftKneeAngle);
                                }
                                if (fData[4] > 0) {
                                    mDataViewModel.setIsTouchLeftFoot(true);
                                } else {
                                    mDataViewModel.setIsTouchLeftFoot(false);
                                }
                                break;


                            case REQUEST_RT:
                            case REQUEST_RC:
                                if (mData[REQUEST_RT] != null && mData[REQUEST_RC] != null) {
                                    mRightKneeAngle = getRelateAngle(getVector(mData[REQUEST_RT], mIndexVector), getVector(mData[REQUEST_RC], mIndexVector));
                                    if (mRightKneeAngle > mRightKneeAngleMax && fData[4] > 0) {
                                        mRightKneeAngleMax = mLeftKneeAngle;
                                    }
                                    mDataViewModel.setDataRightKneeAngle(mRightKneeAngle);
                                }
                                if (fData[4] > 0) {
                                    mDataViewModel.setIsTouchRightFoot(true);
                                } else {
                                    mDataViewModel.setIsTouchRightFoot(false);
                                }
                                break;
                        }
                    }

                } catch (Exception err) {
                    err.printStackTrace();
                    Log.e("IO","IO"+err);
                }
                break;
            case MESSAGE_DEVICE_ADDRESS:
                address = msg.getData().getString("device_MAC");
                setSnackBar( "Connected to " + address);
                break;
        }
    }
}


