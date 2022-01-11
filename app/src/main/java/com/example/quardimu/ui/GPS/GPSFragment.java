package com.example.quardimu.ui.GPS;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StableIdKeyProvider;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quardimu.DataViewModel;
import com.example.quardimu.R;
import com.example.quardimu.VerticalElement;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import static com.example.quardimu.MainActivity.mOutputFileDir;


public class GPSFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener{

    private static final String TAG = "GPSFragment";
    private TextView mLocation_text;

    @SuppressLint("StaticFieldLeak")
    private static TextView mWidthText;

    private static LocationDrawable mLocationDrawable;

    private static VerticalElement.VerticalSeekBar mSeekBar;

    @SuppressLint("StaticFieldLeak")
    private static ImageView mImageView;

    private ArrayList<ImageView> maps = new ArrayList<>();
    private Boolean mapVisibility = true;
    private ImageView mBottomOfSilentLake;
    private ImageView mSilentLake;
    private ImageView mActivityCenter;
    private ImageView mBottomOfPositiveRoad;
    private ImageView mLiteratureDepartment;
    private ImageView mFountain;
    private ImageView mAdministration;
    private ImageView mLibrary;
    private ImageView mEducation;
    private ImageView mCommonEducation;
    private ImageView mLaw;
    private ImageView mHall;

    private float mapWidth;
    private float mapHeight;
    private float rotateAngle;
    private double rotateAngleRadians;

    private static SharedPreferences mSharedPreferences;

    protected RecyclerView mMapRecycleView;
    protected MapListAdapter mMapAdapter;
    protected RecyclerView.LayoutManager mMapLayoutManager;
    protected String[] mMapSet = {"No records"};
    private static ConstraintLayout mMapListLayout;

    private static String mapFileName;

    private static Location lastLocation;

    private static double mMapLowGap = 30;
    private static double mMapHighGap = 60;



    public static GPSFragment newInstance(){
        return new GPSFragment();
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_gps, container, false);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.requireActivity());
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        mLocationDrawable = new LocationDrawable(getResources());
        mImageView = view.findViewById(R.id.imageView);

        mWidthText = view.findViewById(R.id.width_text);
        mLocation_text = view.findViewById(R.id.text_location);

        mSeekBar = view.findViewById(R.id.seekBar);
        mSeekBar.setMax(stringGetInt(mSharedPreferences.getString("widthMax","10"))-1);

        mWidthText.setText("<- " + (1 + mSeekBar.getProgress()) * 100 + "m [" + stringGetInt(mSharedPreferences.getString("gridSize","10")) + "m] ->");

        mLocationDrawable.setGridSize(stringGetFloat(mSharedPreferences.getString("gridSize","10")));
        ViewModelProvider viewModelProvider = new ViewModelProvider(requireActivity());
        DataViewModel dataViewModel = viewModelProvider.get(DataViewModel.class);
        mLocation_text.setText(dataViewModel.getLatitude()+":"+dataViewModel.getLongitude());

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                reDraw();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        ImageView zoom_out = view.findViewById(R.id.zoom_out_button);
        zoom_out.setOnClickListener(v -> {
            if(mSeekBar.getProgress() < stringGetInt(mSharedPreferences.getString("widthMax","10"))-1) {
                mSeekBar.setProgress(mSeekBar.getProgress() + 1);
                resetMaps();
            }
        });
        ImageView zoom_in = view.findViewById(R.id.zoom_in_button);
        zoom_in.setOnClickListener(v -> {
            if (mSeekBar.getProgress() > 0) {
                mSeekBar.setProgress(mSeekBar.getProgress() - 1 );
                resetMaps();
            }
        });
        for (int i = 0; i < 5; i++) {
            mSeekBar.setProgress(mSeekBar.getProgress() + 1);
        }

        ImageView mapView = view.findViewById(R.id.map);
        mMapListLayout = view.findViewById(R.id.map_list_layout);

        mMapRecycleView =  view.findViewById(R.id.map_recycle_view);
        mMapLayoutManager = new LinearLayoutManager(getActivity());
        mMapRecycleView.setLayoutManager(mMapLayoutManager);
        mMapAdapter = new MapListAdapter(mMapSet);
        mMapRecycleView.setAdapter(mMapAdapter);

        mapView.setOnClickListener(v -> {
            if(mMapListLayout.getVisibility() != View.VISIBLE) {
                mMapSet = getMapFileName();
                mMapAdapter = new MapListAdapter(mMapSet);
                mMapRecycleView.setAdapter(mMapAdapter);
                mMapListLayout.setVisibility(View.VISIBLE);
            } else {
                mMapListLayout.setVisibility(View.GONE);
            }
        });

        ImageView mark = view.findViewById(R.id.mark);
        Log.d(TAG, "markHeight: " + mark.getHeight());

        ImageView colors = view.findViewById((R.id.colors));
        colors.getLayoutParams().height = 300;
        colors.getLayoutParams().width = 300 * 606 / 869;
        colors.setY(100);
        colors.requestLayout();

        rotateAngle = 128;
        rotateAngleRadians = Math.toRadians(rotateAngle);

        mBottomOfSilentLake = view.findViewById(R.id.silent_lake_to_positive_road);
        maps.add(mBottomOfSilentLake);
        mSilentLake = view.findViewById(R.id.silent_lake);
        maps.add(mSilentLake);
        mActivityCenter = view.findViewById(R.id.activity_center);
        maps.add(mActivityCenter);
        mBottomOfPositiveRoad = view.findViewById(R.id.bottom_of_positive_road);
        maps.add(mBottomOfPositiveRoad);
        mLiteratureDepartment = view.findViewById(R.id.literature_department);
        maps.add(mLiteratureDepartment);
        mFountain = view.findViewById(R.id.fountain);
        maps.add(mFountain);
        mAdministration = view.findViewById(R.id.administration);
        maps.add(mAdministration);
        mLibrary = view.findViewById(R.id.library);
        maps.add(mLibrary);
        mEducation = view.findViewById(R.id.education);
        maps.add(mEducation);
        mCommonEducation = view.findViewById(R.id.common_education);
        maps.add(mCommonEducation);
        mLaw = view.findViewById(R.id.law);
        maps.add(mLaw);
        mHall = view.findViewById(R.id.hall);mLaw.setVisibility(View.INVISIBLE);
        maps.add(mHall);

        resetMaps();

        view.findViewById(R.id.draw).setOnClickListener(v -> {
            ArrayList<String> fileNames = new ArrayList<String>();
            File folder = new File(mOutputFileDir);
            File[] files = folder.listFiles();

            assert files != null;
            for (File file : files) {
                if (file.isFile()) {
                    if (file.getName().matches("MAP.*")) {
                        fileNames.add(file.getName());
                    }
                }
            }

            drawMultiple(fileNames);
        });

        view.findViewById(R.id.toggleMap).setOnClickListener(v -> {
            mapVisibility = !mapVisibility;
            resetMaps();
        });

        Button removeButton = view.findViewById(R.id.remove_map);
        removeButton.setOnClickListener(v -> {
            mapFileName = null;
            mLocationDrawable.setNullMap();
            mMapListLayout.setVisibility(View.GONE);
            reDraw();
        });

        mImageView.setImageDrawable(mLocationDrawable);

        return view;
    }

    private void resetMaps() {
        // set visibility
        maps.forEach(m -> m.setVisibility(mapVisibility ? View.VISIBLE : View.INVISIBLE));

        // set size
        mapWidth = 200 + ((9 - mSeekBar.getProgress()) * 100);
        mapHeight = (float) (mapWidth * 0.6665);
        maps.forEach(m -> {
            m.getLayoutParams().width = (int) mapWidth;
            m.getLayoutParams().height = (int) mapHeight;
            m.requestLayout();
        });

        // rotate maps
        maps.forEach(m -> m.setRotation(rotateAngle));

        // set position
        mBottomOfSilentLake.setX(350);
        mBottomOfSilentLake.setY(1300);

        setRelativeMapsPosition(mSilentLake, mBottomOfSilentLake, "right");
        setRelativeMapsPosition(mActivityCenter, mBottomOfSilentLake, "down");
        setRelativeMapsPosition(mBottomOfPositiveRoad, mBottomOfSilentLake, "left");
        setRelativeMapsPosition(mLiteratureDepartment, mBottomOfPositiveRoad, "down");
        setRelativeMapsPosition(mFountain, mBottomOfPositiveRoad, "left");
        setRelativeMapsPosition(mAdministration, mFountain, "up");
        setRelativeMapsPosition(mLibrary, mFountain, "down");
        setRelativeMapsPosition(mEducation, mFountain, "left");
        setRelativeMapsPosition(mCommonEducation, mEducation, "up");
        setRelativeMapsPosition(mLaw, mEducation, "left");
        setRelativeMapsPosition(mHall, mAdministration, "up");
    }

    private void setRelativeMapsPosition(ImageView targetMap, ImageView centerMap, String direction) {
        float xDeviation, yDeviation;

        switch (direction) {
            case "up": {
                xDeviation = (float) (mapHeight * Math.sin(rotateAngleRadians));
                yDeviation = (float) (-(mapHeight * Math.cos(rotateAngleRadians)));
                break;
            }
            case "down": {
                xDeviation = (float) (-(mapHeight * Math.sin(rotateAngleRadians)));
                yDeviation = (float) (mapHeight * Math.cos(rotateAngleRadians));
                break;
            }
            case "left": {
                xDeviation = (float) (-(mapWidth * Math.cos(rotateAngleRadians)));
                yDeviation = (float) (-(mapWidth * Math.sin(rotateAngleRadians)));
                break;
            }
            case "right": {
                xDeviation = (float) (mapWidth * Math.cos(rotateAngleRadians));
                yDeviation = (float) (mapWidth * Math.sin(rotateAngleRadians));
                break;
            }
            default: {
                Log.d(TAG, "Wrong direction string!");
                return;
            }
        }

        targetMap.setX(centerMap.getX() + xDeviation);
        targetMap.setY(centerMap.getY() + yDeviation);
    }

    @SuppressLint("SetTextI18n")
    private static void reDraw() {
        mLocationDrawable.setWidthSize((mSeekBar.getProgress() + 1) * 100);
        mWidthText.setText("<- " + (1 + mSeekBar.getProgress()) * 100 + "m [" + stringGetInt(mSharedPreferences.getString("gridSize","10")) + "m] ->");
        if (mapFileName != null) {
            mLocationDrawable.setMap(mLocationDrawable.readMap(mapFileName, mMapLowGap, 0), lastLocation, LocationDrawable.TYPE_MAP);
            mLocationDrawable.setMap(mLocationDrawable.readMap(mapFileName, mMapHighGap, mMapLowGap), lastLocation, LocationDrawable.TYPE_MAP_LOW);
            mLocationDrawable.setMap(mLocationDrawable.readMap(mapFileName, 120, mMapHighGap), lastLocation, LocationDrawable.TYPE_MAP_HIGH);
            mLocationDrawable.setNoteLocation(mLocationDrawable.readNoteLocation(mapFileName), lastLocation);
        }
        mImageView.invalidateDrawable(mLocationDrawable);
    }

    @SuppressLint("SetTextI18n")
    private static void drawMultiple(ArrayList<String> fileNames) {
        mLocationDrawable.setWidthSize((mSeekBar.getProgress() + 1) * 100);
        mWidthText.setText("<- " + (1 + mSeekBar.getProgress()) * 100 + "m [" + stringGetInt(mSharedPreferences.getString("gridSize","10")) + "m] ->");

        ArrayList<Double> list;

        list = mLocationDrawable.readMap(fileNames.get(0), mMapLowGap, 0);
        for (int i = 1; i < fileNames.size(); i++) {
            list.addAll(mLocationDrawable.readMap(fileNames.get(i), mMapLowGap, 0));
        }
        mLocationDrawable.setMap(list, lastLocation, LocationDrawable.TYPE_MAP);

        list = mLocationDrawable.readMap(fileNames.get(0), mMapHighGap, mMapLowGap);
        for (int i = 1; i < fileNames.size(); i++) {
            list.addAll(mLocationDrawable.readMap(fileNames.get(i), mMapHighGap, mMapLowGap));
        }
        mLocationDrawable.setMap(list, lastLocation, LocationDrawable.TYPE_MAP_LOW);

        list = mLocationDrawable.readMap(fileNames.get(0), 120, mMapHighGap);
        for (int i = 1; i < fileNames.size(); i++) {
            list.addAll(mLocationDrawable.readMap(fileNames.get(i), 120, mMapHighGap));
        }
        mLocationDrawable.setMap(list, lastLocation, LocationDrawable.TYPE_MAP_HIGH);

        list = mLocationDrawable.readNoteLocation(fileNames.get(0));
        for (int i = 1; i < fileNames.size(); i++) {
            list.addAll(mLocationDrawable.readNoteLocation(fileNames.get(i)));
        }
        mLocationDrawable.setNoteLocation(list, lastLocation);

        mImageView.invalidateDrawable(mLocationDrawable);
    }


    @SuppressLint("SetTextI18n")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() != null) {
            new ViewModelProvider(getActivity()).get(DataViewModel.class).getLocation().observe(getViewLifecycleOwner(), s -> {
                assert s != null;
                lastLocation = s;
                mLocation_text.setText(s.getLatitude() + ":" + s.getLongitude());
                if (mapFileName != null) {
                    mLocationDrawable.setMap(mLocationDrawable.readMap(mapFileName, mMapLowGap, 0), lastLocation, LocationDrawable.TYPE_MAP);
                    mLocationDrawable.setMap(mLocationDrawable.readMap(mapFileName, mMapHighGap, mMapLowGap), lastLocation, LocationDrawable.TYPE_MAP_LOW);
                    mLocationDrawable.setMap(mLocationDrawable.readMap(mapFileName, 120, mMapHighGap), lastLocation, LocationDrawable.TYPE_MAP_HIGH);
                    mLocationDrawable.setNoteLocation(mLocationDrawable.readNoteLocation(mapFileName), lastLocation);
                    mImageView.invalidateDrawable(mLocationDrawable);
                }
            });
        }
    }

    private float stringGetFloat(String str){
        if (str.isEmpty()){
            return 0;
        }else{
            return Float.parseFloat(str);
        }
    }
    private static int stringGetInt(String str){
        if (str.isEmpty()){
            return 0;
        }else{
            return Integer.parseInt(str);
        }
    }
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mMapLowGap = Double.parseDouble(sharedPreferences.getString("angleLowGap","30"));
        mMapHighGap = Double.parseDouble(sharedPreferences.getString("angleHighGap","60"));
        mLocationDrawable.setGridSize(stringGetFloat(sharedPreferences.getString("gridSize","10")));
        mSeekBar.setMax(stringGetInt(sharedPreferences.getString("widthMax","10"))-1);
        reDraw();
    }

    private String[] getMapFileName(){
        String[] mapList = {"No records"};
        File dir = new File(mOutputFileDir);
        if (dir.exists()){
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                int num=0;
                for (File file : files) {
                    if (file.getName().matches("MAP(.*)")){
                        num++;
                    }
                }
                mapList = new String[num];
                num=0;
                for (File file : files) {
                    if (file.getName().matches("MAP(.*)")) {
                        mapList[num] = file.getName();
                        num++;
                    }
                }
            }
        }
        return mapList;
    }

    public static class MapListAdapter extends RecyclerView.Adapter<MapListAdapter.ViewHolder> {
        private static final String TAG = "MapListAdapter";

        private static String[] mDataSet;

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView textView;
            public ViewHolder(View v) {
                super(v);
                v.setOnClickListener(v1 -> {
                    mapFileName = mDataSet[getAdapterPosition()];
                    Log.d(TAG, "Element " + getAdapterPosition() + " clicked.");
                    mMapListLayout.setVisibility(View.GONE);
                    reDraw();
                });
                textView = (TextView) v.findViewById(R.id.map_textView);
            }
            public TextView getTextView() {
                return textView;
            }
        }

        public MapListAdapter(String[] dataSet) {
            mDataSet = dataSet;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            // Create a new view.
            View v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.map_item, viewGroup, false);

            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {
            Log.d(TAG, "Element " + position + " set.");
            viewHolder.getTextView().setText(mDataSet[position]);
        }

        @Override
        public int getItemCount() {
            return mDataSet.length;
        }
    }

}

