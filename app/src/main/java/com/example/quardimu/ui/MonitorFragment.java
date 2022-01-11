package com.example.quardimu.ui;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.example.quardimu.DataViewModel;
import com.example.quardimu.R;
import com.google.android.material.card.MaterialCardView;

import static com.example.quardimu.MainActivity.REQUEST_LC;
import static com.example.quardimu.MainActivity.REQUEST_LT;
import static com.example.quardimu.MainActivity.REQUEST_RC;
import static com.example.quardimu.MainActivity.REQUEST_RT;


public class MonitorFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener{

    private TextView textLT;
    private TextView textLC;
    private TextView textRT;
    private TextView textRC;
    private TextView textLK;
    private TextView textRK;
    private TextView textLocation;
    private TextView textLocationTime;

    private ImageView leftFootprint;
    private ImageView rightFootprint;

    private MaterialCardView cardLT;
    private MaterialCardView cardLC;
    private MaterialCardView cardRT;
    private MaterialCardView cardRC;
    private MaterialCardView cardLK;
    private MaterialCardView cardRK;
    private MaterialCardView cardLocation;


    private long startTime;

    private float mCautionAngle;

    private boolean touchLC = false;
    private boolean touchRC = false;

    public static MonitorFragment newInstance() {
        return new MonitorFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_monitor, container, false);
        textLT = view.findViewById(R.id.textCardLT);
        textLC = view.findViewById(R.id.textCardLC);
        textRT = view.findViewById(R.id.textCardRT);
        textRC = view.findViewById(R.id.textCardRC);
        textLK = view.findViewById(R.id.textCardLK);
        textRK = view.findViewById(R.id.textCardRK);
        textLocation = view.findViewById(R.id.textCardLocation);
        textLocationTime = view.findViewById(R.id.textCardLocationTime);

        leftFootprint = view.findViewById(R.id.left_footprint);
        rightFootprint = view.findViewById(R.id.right_footprint);


        cardLT = view.findViewById(R.id.cardLT);
        cardLC = view.findViewById(R.id.cardLC);
        cardRT = view.findViewById(R.id.cardRT);
        cardRC = view.findViewById(R.id.cardRC);
        cardLK = view.findViewById(R.id.cardLK);
        cardRK = view.findViewById(R.id.cardRK);
        cardLocation = view.findViewById(R.id.cardLocation);

        startTime = System.currentTimeMillis();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.requireActivity());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        mCautionAngle = Float.parseFloat(sharedPreferences.getString("caution_angle","50"));


        return view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(getActivity() != null) {
            new ViewModelProvider(getActivity()).get(DataViewModel.class).getLocation().observe(getViewLifecycleOwner(), new Observer<Location>() {
                @SuppressLint("DefaultLocale")
                @Override
                public void onChanged(@Nullable Location s) {
                    if (s == null) {
                        cardLocation.setVisibility(View.GONE);
                    } else {
                        cardLocation.setVisibility(View.VISIBLE);
                    }
                    textLocation.setText("La: " + s.getLatitude() + "\nLo: " + s.getLongitude());
                    float timeStep = System.currentTimeMillis() - startTime;
                    textLocationTime.setText(String.format("[Update]\n%.2f secs", timeStep / 1000));
                    startTime = System.currentTimeMillis();
                }
            });


            new ViewModelProvider(getActivity()).get(DataViewModel.class).getData(REQUEST_LT).observe(getViewLifecycleOwner(), new Observer<String>() {
                @Override
                public void onChanged(@Nullable String s) {
                    if (s == null) {
                        cardLT.setVisibility(View.GONE);
                    } else {
                        cardLT.setVisibility(View.VISIBLE);
                        String[] data = s.split("\t");
                        textLT.setText(MonitorDataFormat(data));
                    }
                }
            });

            new ViewModelProvider(getActivity()).get(DataViewModel.class).getData(REQUEST_LC).observe(getViewLifecycleOwner(), new Observer<String>() {
                @Override
                public void onChanged(@Nullable String s) {
                    if (s == null) {
                        cardLC.setVisibility(View.GONE);
                    } else {
                        cardLC.setVisibility(View.VISIBLE);
                        String[] data = s.split("\t");
                        textLC.setText(MonitorDataFormat(data));
                    }
                }
            });

            new ViewModelProvider(getActivity()).get(DataViewModel.class).getData(REQUEST_RT).observe(getViewLifecycleOwner(), new Observer<String>() {
                @Override
                public void onChanged(@Nullable String s) {
                    if (s == null) {
                        cardRT.setVisibility(View.GONE);
                    } else {
                        cardRT.setVisibility(View.VISIBLE);
                        String[] data = s.split("\t");
                        textRT.setText(MonitorDataFormat(data));
                    }
                }
            });

            new ViewModelProvider(getActivity()).get(DataViewModel.class).getData(REQUEST_RC).observe(getViewLifecycleOwner(), new Observer<String>() {
                @Override
                public void onChanged(@Nullable String s) {
                    if (s == null) {
                        cardRC.setVisibility(View.GONE);
                    } else {
                        cardRC.setVisibility(View.VISIBLE);
                        String[] data = s.split("\t");
                        textRC.setText(MonitorDataFormat(data));
                    }
                }
            });
            new ViewModelProvider(getActivity()).get(DataViewModel.class).getLeftKneeAngle().observe(getViewLifecycleOwner(), new Observer<Float>() {
                @Override
                public void onChanged(@Nullable Float s) {
                    if (cardLT.getVisibility() == View.GONE || cardLC.getVisibility() == View.GONE) {
                        cardLK.setVisibility(View.GONE);
                    } else {
                        cardLK.setVisibility(View.VISIBLE);
                        textLK.setText(s + "°");
                        if (s > mCautionAngle && touchLC) {
                            cardLK.setCardBackgroundColor(getResources().getColor(R.color.card_LK_warn));
                        } else {
                            cardLK.setCardBackgroundColor(getResources().getColor(R.color.card_LK));
                        }
                    }
                }
            });
            new ViewModelProvider(getActivity()).get(DataViewModel.class).getRightKneeAngle().observe(getViewLifecycleOwner(), new Observer<Float>() {
                @Override
                public void onChanged(@Nullable Float s) {
                    if (cardRT.getVisibility() == View.GONE || cardRC.getVisibility() == View.GONE) {
                        cardRK.setVisibility(View.GONE);
                    } else {
                        cardRK.setVisibility(View.VISIBLE);
                        textRK.setText(s + "°");
                        if (s > mCautionAngle && touchRC) {
                            cardRK.setCardBackgroundColor(getResources().getColor(R.color.card_RK_warn));
                        } else {
                            cardRK.setCardBackgroundColor(getResources().getColor(R.color.card_RK));
                        }
                    }
                }
            });
            new ViewModelProvider(getActivity()).get(DataViewModel.class).getIsTouchLeftFoot().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
                @Override
                public void onChanged(@Nullable Boolean b) {
                    if(b){
                        leftFootprint.setVisibility(View.VISIBLE);
                        touchLC = true;
                    } else {
                        leftFootprint.setVisibility(View.GONE);
                        touchLC = false;
                    }
                }
            });
            new ViewModelProvider(getActivity()).get(DataViewModel.class).getIsTouchRightFoot().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
                @Override
                public void onChanged(@Nullable Boolean b) {
                    if(b){
                        rightFootprint.setVisibility(View.VISIBLE);
                        touchRC = true;
                    } else {
                        rightFootprint.setVisibility(View.GONE);
                        touchRC = false;
                    }
                }
            });


        }

    }

    private String MonitorDataFormat(String[] data){
        String text;
        text = String.format("X:%11sg%11s°/s%11s°\n", data[0], data[3], data[6]);
        text = text + String.format("Y:%11sg%11s°/s%11s°\n", data[1], data[4], data[7]);
        text = text + String.format("Z:%11sg%11s°/s%11s°", data[2], data[5], data[8]);
        return text;
    }
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mCautionAngle = Float.parseFloat(sharedPreferences.getString("caution_angle","50"));
    }


}


