package com.example.quardimu.ui.GPS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.RequiresApi;

import com.example.quardimu.MainActivity;
import com.example.quardimu.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.util.ArrayList;

import static com.example.quardimu.MainActivity.mOutputFileDir;

public class LocationDrawable extends Drawable {
    private final static String TAG = "LocationDrawable";

    private Resources res;
    private Paint borderPaint, gridPaint, currentLocationPaint, mapPaint, mapLowPaint, mapHighPaint, noteLocationPaint;

    private float gridSize = 10; //10m
    private float widthSize = 500; //500 m
    private float margin = 20;
    private float[] map = null;
    private float[] mapLow = null;
    private float[] mapHigh = null;
    private float[] noteLocation = null;
    private ArrayList<String> noteLocationName = null;

    public static final int TYPE_MAP =0;
    public static final int TYPE_MAP_LOW =1;
    public static final int TYPE_MAP_HIGH =2;



    public LocationDrawable(Resources res) {

        this.res = res;

        borderPaint = new Paint();
        borderPaint.setARGB(255,128,128,128);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(5);

        gridPaint = new Paint();
        gridPaint.setARGB(100,128,128,128);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2);

        currentLocationPaint = new Paint();
        currentLocationPaint.setARGB(255,0,172,181);
        currentLocationPaint.setStyle(Paint.Style.STROKE);
        currentLocationPaint.setStrokeWidth(5);

        mapPaint = new Paint();
        mapPaint.setARGB(238,238,238,238);
        mapPaint.setStyle(Paint.Style.STROKE);
        mapPaint.setStrokeWidth(5);

        mapLowPaint = new Paint();
        mapLowPaint.setARGB(238,3,53,252);
        mapLowPaint.setStyle(Paint.Style.STROKE);
        mapLowPaint.setStrokeWidth(5);

        mapHighPaint = new Paint();
        mapHighPaint.setARGB(238,245,87,66);
        mapHighPaint.setStyle(Paint.Style.STROKE);
        mapHighPaint.setStrokeWidth(5);

        noteLocationPaint = new Paint();
        noteLocationPaint.setARGB(200,202,232,232);
        noteLocationPaint.setTextSize(35);

    }

    @Override
    public void draw(Canvas canvas) {
//        drawGrid(canvas);
        drawCurrentLocation(canvas);
        if(map != null) {
            canvas.drawPoints(map, mapPaint);
        }
        if(mapLow != null) {
            canvas.drawPoints(mapLow, mapLowPaint);
        }
        if(mapHigh != null) {
            canvas.drawPoints(mapHigh, mapHighPaint);
        }
        if(noteLocation != null) {
            drawNoteLocation(canvas);
        }
    }

    private void drawGrid(Canvas canvas) {
        float widthPixel = getBounds().width();
        float heightPixel = getBounds().height();
        float gridPixel = (widthPixel-2*margin)*gridSize/widthSize;
        for(int i=0;i<widthSize/gridSize;i++){
            canvas.drawLine(margin + i*gridPixel , margin , margin + i*gridPixel,heightPixel-margin,gridPaint);
        }
        canvas.drawLine(margin, heightPixel/2, widthPixel - margin, heightPixel/2 ,gridPaint);
        for(int i=1;i<((heightPixel - 2*margin)/gridPixel)/2;i++){
            canvas.drawLine(margin, heightPixel/2 + i*gridPixel , widthPixel - margin, heightPixel/2 + i*gridPixel ,gridPaint);
            canvas.drawLine(margin, heightPixel/2 - i*gridPixel , widthPixel - margin, heightPixel/2 - i*gridPixel ,gridPaint);
        }
        canvas.drawRect(margin,margin,widthPixel-margin,heightPixel-margin,borderPaint);
    }

    private void drawCurrentLocation(Canvas canvas) {
        float widthPixel = getBounds().width();
        float heightPixel = getBounds().height();
        canvas.drawCircle(widthPixel/2, heightPixel/2, 20, currentLocationPaint );
        canvas.drawCircle(widthPixel/2,heightPixel/2, 5, currentLocationPaint );
    }

    private void drawNoteLocation(Canvas canvas) {

        @SuppressLint("UseCompatLoadingForDrawables")
        Drawable icon = res.getDrawable(R.drawable.ic_baseline_location_on_24,null);

        for(int i=0;i<noteLocation.length;i = i+2){
            Log.d(TAG,"X: " + noteLocation[i] + "Y: " + noteLocation[i+1]);
            icon.setBounds((int)noteLocation[i] - 30 ,(int)noteLocation[i+1] - 60,(int)noteLocation[i]+30,(int)noteLocation[i+1]);
            icon.draw(canvas);
            canvas.drawText(noteLocationName.get(i/2),noteLocation[i] + 40, noteLocation[i+1] ,noteLocationPaint);
        }
    }

    public ArrayList<Double> readMap(String str, double angleMax, double angleMin) {
        if (str == null){
            return null;
        }else {
            ArrayList<Double> dataList = new ArrayList<>();
            File file = new File(mOutputFileDir, str);
            String[] data;
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    data = line.split("\t");
                    if (data[0].matches("^[0-9](.*)")) {
                        if ((Double.parseDouble(data[4]) >= angleMin) && (Double.parseDouble(data[4]) < angleMax)) {
                            dataList.add(Double.parseDouble(data[0]));
                            dataList.add(Double.parseDouble(data[1]));
                        } else if ((Double.parseDouble(data[5]) >= angleMin) && (Double.parseDouble(data[5]) < angleMax)) {
                            dataList.add(Double.parseDouble(data[0]));
                            dataList.add(Double.parseDouble(data[1]));
                        }
                    }
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return dataList;
        }
    }

    public ArrayList<Double> readNoteLocation(String str){
        if (str == null){
            return null;
        }else {
            ArrayList<Double> dataList = new ArrayList<>();
            noteLocationName = new ArrayList<>();
            File file = new File(mOutputFileDir, str);
            String[] data;
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    data = line.split("\t");
                    if (data[0].matches("^@(.*)")) {
                        dataList.add(Double.parseDouble(data[1]));
                        dataList.add(Double.parseDouble(data[2]));
                        if(data[3] != null) {
                            noteLocationName.add(data[3]);
                        }
                    }
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return dataList;
        }
    }

    public void setNoteLocation(ArrayList<Double> locationList, Location currentLocation){
        float widthPixel = getBounds().width();
        float heightPixel = getBounds().height();
        float gridPixel = (widthPixel-2*margin)*gridSize/widthSize;
        float[] result = new float[10];
        float[] tmpLocation = null;

        if ( noteLocation != null || currentLocation != null) {
            tmpLocation = new float[locationList.size()];
            for(int i=0;i<locationList.size();i=i+2){
                Location.distanceBetween(currentLocation.getLatitude(),0,locationList.get(i),0,result);
                if(currentLocation.getLatitude() > locationList.get(i)){
                    tmpLocation[i+1] = (heightPixel/2) + gridPixel*result[0]/gridSize;
                }else{
                    tmpLocation[i+1] = (heightPixel/2) - gridPixel*result[0]/gridSize;
                }
                Location.distanceBetween(0,currentLocation.getLongitude(),0,locationList.get(i+1),result);
                if(currentLocation.getLongitude() > locationList.get(i+1)){
                    tmpLocation[i] = (widthPixel/2) - gridPixel*result[0]/gridSize;
                }else{
                    tmpLocation[i] = (widthPixel/2) + gridPixel*result[0]/gridSize;
                }
            }
        }
        noteLocation = passValue(tmpLocation);
    }

    public void setMap(ArrayList<Double> mapList, Location currentLocation, int type){
        float widthPixel = getBounds().width();
        float heightPixel = getBounds().height();
        float gridPixel = (widthPixel-2*margin)*gridSize/widthSize;
        float[] result = new float[10];
        float[] tmpMap = null;

        if (mapList != null || currentLocation != null) {
            assert mapList != null;
            tmpMap = new float[mapList.size()];
            for(int i=0;i<mapList.size();i=i+2){
                Location.distanceBetween(currentLocation.getLatitude(),0,mapList.get(i),0,result);
                if(currentLocation.getLatitude() > mapList.get(i)){
                    tmpMap[i+1] = (heightPixel/2) + gridPixel*result[0]/gridSize;
                }else{
                    tmpMap[i+1] = (heightPixel/2) - gridPixel*result[0]/gridSize;
                }
                Location.distanceBetween(0,currentLocation.getLongitude(),0,mapList.get(i+1),result);
                if(currentLocation.getLongitude() > mapList.get(i+1)){
                    tmpMap[i] = (widthPixel/2) - gridPixel*result[0]/gridSize;
                }else{
                    tmpMap[i] = (widthPixel/2) + gridPixel*result[0]/gridSize;
                }
            }
        }
        switch (type){
            case TYPE_MAP:
                map = passValue(tmpMap);
                break;
            case TYPE_MAP_LOW:
                mapLow = passValue(tmpMap);
                break;
            case TYPE_MAP_HIGH:
                mapHigh = passValue(tmpMap);
                break;
        }
    }

    public void setNullMap(){
        map = null;
        mapLow = null;
        mapHigh = null;
        noteLocation = null;
        noteLocationName = null;
    }

    private float[] passValue(float[] in){
        if (in == null) {
            return null;
        } else {
            return in.clone();
        }
    }







    public void setGridSize(float num){
        gridSize = num;
    }

    public void setWidthSize(float num) {
        widthSize = num;
    }

    @Override
    public void setAlpha(int alpha) {
        // This method is required
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // This method is required
    }

    @Override
    public int getOpacity() {
        // Must be PixelFormat.UNKNOWN, TRANSLUCENT, TRANSPARENT, or OPAQUE
        return PixelFormat.OPAQUE;
    }
}
