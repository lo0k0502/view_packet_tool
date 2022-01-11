package com.example.quardimu.ui.Plot;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.location.Location;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import static com.example.quardimu.MainActivity.mOutputFileDir;

public class PlotDrawable extends Drawable {
    private Paint borderPaint, gridPaint, dataPaint;

    private float margin = 20;
    private float dataHeightPixel = 500;
    private int dataGridNumberX = 10;
    private int dataGridNumberY = 6;
    private int dataNumber = 0;

    private float kneeAngleMax = 180;
    float[][] mData = new float[dataNumber][0];


    public PlotDrawable (){

        borderPaint = new Paint();
        borderPaint.setARGB(255,128,128,128);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(5);

        gridPaint = new Paint();
        gridPaint.setARGB(100,128,128,128);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2);

        dataPaint = new Paint();
        dataPaint.setARGB(255,0,172,181);
        dataPaint.setStyle(Paint.Style.STROKE);
        dataPaint.setStrokeWidth(5);
        
    }
    public int getDataNumber(){
        return dataNumber;
    }
    public void initData(){
        dataNumber = 0;
        mData = new float[dataNumber][0];
    }
    public void addData(float[] num){
        float[][] tmp = new float[dataNumber][0];
        tmp = mData.clone();
        dataNumber++;
        mData = new float[dataNumber][0];
        mData[dataNumber-1] = num.clone();
        for(int i=0;i<dataNumber-1;i++){
            mData[i] = tmp[i].clone();
        }


    }
    public void setDataHeightPixel(float num){
        dataHeightPixel = num;
    }

    public void setDataGridNumberX(int num){
        dataGridNumberX = num;
    }
    public void setDataGridNumberY(int num){
        dataGridNumberY = num;
    }

    @Override
    public void draw(Canvas canvas) {
        for(int i=0;i<dataNumber;i++) {
            drawData(canvas, i*dataHeightPixel,mData[i]);
        }
    }

    private void drawData(Canvas canvas, float startHeightPixel, float[] data) {
        float widthPixel = getBounds().width();
        float gridPixelY = (dataHeightPixel - 2*margin)/dataGridNumberY;
        float gridPixelX = (widthPixel - 2*margin)/dataGridNumberX;
        canvas.drawRect(margin,startHeightPixel + margin,widthPixel - margin, startHeightPixel + dataHeightPixel - margin, borderPaint);
        for (int i=0;i<dataGridNumberY;i++) {
            canvas.drawLine(margin, startHeightPixel + margin + i*gridPixelY, widthPixel - margin,startHeightPixel + margin + i*gridPixelY,gridPaint );
        }
        for (int j=0;j<dataGridNumberX;j++){
            canvas.drawLine(margin + j * gridPixelX, startHeightPixel+margin,margin + j*gridPixelX,startHeightPixel + dataHeightPixel - margin,gridPaint);
        }

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
