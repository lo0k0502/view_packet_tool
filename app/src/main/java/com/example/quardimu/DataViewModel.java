package com.example.quardimu;

import android.location.Location;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import static com.example.quardimu.MainActivity.REQUEST_LC;
import static com.example.quardimu.MainActivity.REQUEST_LT;
import static com.example.quardimu.MainActivity.REQUEST_RC;
import static com.example.quardimu.MainActivity.REQUEST_RT;


public class DataViewModel extends ViewModel {

    private MutableLiveData<String> dataLT;
    private MutableLiveData<String> dataLC;
    private MutableLiveData<String> dataRT;
    private MutableLiveData<String> dataRC;

    private MutableLiveData<Double> dataLatitude;
    private MutableLiveData<Double> dataLongitude;
    private MutableLiveData<Float> dataLeftKneeAngle;
    private MutableLiveData<Float> dataRightKneeAngle;
    private MutableLiveData<Location> dataLocation;

    private MutableLiveData<Boolean> isTouchLeftFoot;
    private MutableLiveData<Boolean> isTouchRightFoot;

    public DataViewModel() {
        dataLT = new MutableLiveData<>();
        dataLC = new MutableLiveData<>();
        dataRT = new MutableLiveData<>();
        dataRC = new MutableLiveData<>();

        dataLatitude = new MutableLiveData<>();
        dataLongitude = new MutableLiveData<>();
        dataRightKneeAngle = new MutableLiveData<>();
        dataLeftKneeAngle = new MutableLiveData<>();
        dataLocation = new MutableLiveData<>();

        isTouchLeftFoot = new MutableLiveData<>();
        isTouchRightFoot = new MutableLiveData<>();
    }

    public void setIsTouchLeftFoot(Boolean b){
        isTouchLeftFoot.setValue(b);
    }

    public void setIsTouchRightFoot(Boolean b){
        isTouchRightFoot.setValue(b);
    }

    public MutableLiveData<Boolean> getIsTouchLeftFoot(){return isTouchLeftFoot;}
    public MutableLiveData<Boolean> getIsTouchRightFoot(){return isTouchRightFoot;}


    public void setData(int tag, String text) {
        switch (tag){
            case REQUEST_LT:
                dataLT.setValue(text);
                break;
            case REQUEST_LC:
                dataLC.setValue(text);
                break;
            case REQUEST_RT:
                dataRT.setValue(text);
                break;
            case REQUEST_RC:
                dataRC.setValue(text);
                break;
        }
    }

    public void setDataRightKneeAngle(float data){
        dataRightKneeAngle.setValue(data);
    }
    public void setDataLeftKneeAngle(float data){
        dataLeftKneeAngle.setValue(data);
    }

    public void setLocation(Location location) {
        dataLatitude.setValue(location.getLatitude());
        dataLongitude.setValue(location.getLongitude());
        dataLocation.setValue(location);

    }

    public MutableLiveData<String> getData(int tag) {
        MutableLiveData<String> tmp = new MutableLiveData<>();
        switch (tag){
            case REQUEST_LT:
                tmp = dataLT;
                break;
            case REQUEST_LC:
                tmp = dataLC;
                break;
            case REQUEST_RT:
                tmp = dataRT;
                break;
            case REQUEST_RC:
                tmp = dataRC;
                break;
        }
        return tmp;
    }
    public LiveData<Float> getLeftKneeAngle(){return dataLeftKneeAngle;}
    public LiveData<Float> getRightKneeAngle(){return dataRightKneeAngle;}

    public LiveData<Location> getLocation(){
        return dataLocation;
    }
    public LiveData<Double> getLatitude(){
        return dataLatitude;
    }
    public LiveData<Double> getLongitude(){
        return dataLongitude;
    }


}

