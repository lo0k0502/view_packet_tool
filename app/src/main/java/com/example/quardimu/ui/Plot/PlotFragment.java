package com.example.quardimu.ui.Plot;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.example.quardimu.DataViewModel;
import com.example.quardimu.R;
import com.example.quardimu.ui.GPS.LocationDrawable;

import static com.example.quardimu.MainActivity.REQUEST_LC;
import static com.example.quardimu.MainActivity.REQUEST_LT;
import static com.example.quardimu.MainActivity.REQUEST_RT;

public class PlotFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener{

    private final static String TAG = "PlotFragment";
    private PlotDrawable mPlotDrawable;
    private static ImageView mImageView;

    private int mGridNumberX = 10;
    private int mGridNumberY = 6;
    private float mDataHeightPixel = 500;
    private float[] mDataLT = new float[9];
    private float[] mDataLC = new float[9];
    private float[] mDataRT = new float[9];
    private float[] mDataRC = new float[9];
    private float[] mDataLeftKneeAngle;
    private float[] mDataRightKneeAngle;

    public static PlotFragment newInstance() {
        return new PlotFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_plot, container, false);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.requireActivity());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mPlotDrawable = new PlotDrawable();
        mImageView = view.findViewById(R.id.imageView);
        ViewModelProvider viewModelProvider = new ViewModelProvider(requireActivity());
        DataViewModel dataViewModel = viewModelProvider.get(DataViewModel.class);

        mGridNumberX = Integer.parseInt(sharedPreferences.getString("grid_num_x","10"));
        mGridNumberY = Integer.parseInt(sharedPreferences.getString("grid_num_y","6"));
        mDataHeightPixel = Float.parseFloat(sharedPreferences.getString("plot_height","500"));
        mDataLeftKneeAngle = new float[mGridNumberX*2];
        mDataRightKneeAngle = new float[mGridNumberX*2];
        mPlotDrawable.initData();
        mPlotDrawable.addData(mDataLeftKneeAngle);
        mPlotDrawable.addData(mDataRightKneeAngle);
        mPlotDrawable.setDataHeightPixel(mDataHeightPixel);
        mPlotDrawable.setDataGridNumberX(mGridNumberX);
        mPlotDrawable.setDataGridNumberY(mGridNumberY);
        mImageView.getLayoutParams().height = (int)(mPlotDrawable.getDataNumber()* mDataHeightPixel);
        mImageView.setImageDrawable(mPlotDrawable);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() != null) {
            new ViewModelProvider(getActivity()).get(DataViewModel.class).getData(REQUEST_LT).observe(getViewLifecycleOwner(), new Observer<String>() {
                @SuppressLint("DefaultLocale")
                @Override
                public void onChanged(@Nullable String s) {
                    if (s != null) {
                        String[] data = s.split("\t");
                        for (int i = 0; i < 9; i++) {
                            mDataLT[i] = Float.parseFloat(data[i]);
                        }
                    }
                }
            });
            new ViewModelProvider(getActivity()).get(DataViewModel.class).getData(REQUEST_LC).observe(getViewLifecycleOwner(), new Observer<String>() {
                @SuppressLint("DefaultLocale")
                @Override
                public void onChanged(@Nullable String s) {
                    if (s!=null) {
                        String[] data = s.split("\t");
                        for (int i = 0; i < 9; i++) {
                            mDataLC[i] = Float.parseFloat(data[i]);
                        }
                    }
                }
            });

            new ViewModelProvider(getActivity()).get(DataViewModel.class).getData(REQUEST_RT).observe(getViewLifecycleOwner(), new Observer<String>() {
                @SuppressLint("DefaultLocale")
                @Override
                public void onChanged(@Nullable String s) {
                    if (s != null) {
                        String[] data = s.split("\t");
                        for (int i = 0; i < 9; i++) {
                            mDataRT[i] = Float.parseFloat(data[i]);
                        }
                    }
                }
            });

            new ViewModelProvider(getActivity()).get(DataViewModel.class).getData(REQUEST_RT).observe(getViewLifecycleOwner(), new Observer<String>() {
                @SuppressLint("DefaultLocale")
                @Override
                public void onChanged(@Nullable String s) {
                    if (s != null) {
                        String[] data = s.split("\t");
                        for (int i = 0; i < 9; i++) {
                            mDataRC[i] = Float.parseFloat(data[i]);
                        }
                    }
                }
            });

            new ViewModelProvider(getActivity()).get(DataViewModel.class).getLeftKneeAngle().observe(getViewLifecycleOwner(), new Observer<Float>() {
                @Override
                public void onChanged(@Nullable Float s) {
                    if (s != null) {

                        reDraw();
                    }
                }
            });
            new ViewModelProvider(getActivity()).get(DataViewModel.class).getRightKneeAngle().observe(getViewLifecycleOwner(), new Observer<Float>() {
                @Override
                public void onChanged(@Nullable Float s) {
                    if (s != null) {
                        reDraw();
                    }
                }
            });
        }
    }

    private void reDraw(){
        mPlotDrawable.initData();
        mPlotDrawable.addData(mDataLeftKneeAngle);
        mPlotDrawable.addData(mDataRightKneeAngle);
        mImageView.getLayoutParams().height = (int)(mPlotDrawable.getDataNumber() * mDataHeightPixel);
        mPlotDrawable.setDataHeightPixel(mDataHeightPixel);
        mPlotDrawable.setDataGridNumberX(mGridNumberX);
        mPlotDrawable.setDataGridNumberY(mGridNumberY);
        mImageView.invalidateDrawable(mPlotDrawable);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mGridNumberX = Integer.parseInt(sharedPreferences.getString("grid_num_x","10"));
        mGridNumberY = Integer.parseInt(sharedPreferences.getString("grid_num_y","6"));
        mDataHeightPixel = Float.parseFloat(sharedPreferences.getString("plot_height","500"));
        mDataLeftKneeAngle = new float[mGridNumberX];
        mDataRightKneeAngle = new float[mGridNumberX];
        reDraw();
    }
}
