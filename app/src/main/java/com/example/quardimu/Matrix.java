package com.example.quardimu;

public class Matrix {

    public static float getRelateAngle(float[] v1, float[] v2) {
        return (float) Math.toDegrees(Math.acos(dotProduct(v1,v2)/(getLength(v1)*getLength(v2))));
    }

    private static float dotProduct(float[] v1, float[] v2) {
        return v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2];
    }

    private static float getLength(float[] v) {
        return (float) Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
    }

    public static float[] getVector(String s, int index_vector){
        String[] sData1 = s.split("\t");

        float[] fData = new float[9];
        for (int i=0;i<=8;i++) {
            fData[i] = Float.parseFloat(sData1[i]);
        }
        float[] vector;
        float[] axisX = {1,0,0};
        float[] axisY = {0,1,0};
        float[] axisZ = {0,0,1};

        float[] index_axis;

        index_axis = new float[]{1, 0, 0};

        switch (index_vector) {
            case 1:
                index_axis = axisX;
                break;
            case 2:
                index_axis = axisY;
                break;
            case 3:
                index_axis = axisZ;
                break;
        }

        float[] axisYP = mvProduct(rotaZ(fData[8]),axisY);
        float[] rotaYP = rota(axisYP,fData[7]);
        float[] axisXPP = mvProduct(rotaYP,mvProduct(rotaZ(fData[8]),axisX));
        float[] rotaXPP = rota(axisXPP,fData[6]);
        vector = mvProduct(rotaZ(fData[8]),index_axis);
        vector = mvProduct(rotaYP, vector);
        vector = mvProduct(rotaXPP,vector);

        return vector;
    }


    private static float[] mvProduct(float[] m, float[] v) {
        float[] temp = new float[3];
        for (int i=0;i<=2;i++){
            temp[i] = m[i*3] * v[0] + m[i*3+1] * v[1] + m[i*3+2] * v[2];
        }
        return temp;
    }

    private static float[] rotaZ(float d) {
        float[] temp = new float[9];
        temp[0] = (float) Math.cos(Math.toRadians(d));
        temp[1] = (float) - Math.sin(Math.toRadians(d));
        temp[2] = 0;
        temp[3] = (float) Math.sin(Math.toRadians(d));
        temp[4] = (float) Math.cos(Math.toRadians(d));
        temp[5] = 0;
        temp[6] = 0;
        temp[7] = 0;
        temp[8] = 1;
        return temp;
    }

    private static float[] rota(float[] u, float d) {
        float[] temp = new float[9];
        float r = (float) Math.toRadians(d);
        temp[0] = (float) ( Math.cos(r) + u[0]*u[0]*(1-Math.cos(r)));
        temp[1] = (float) (u[0]*u[1]*(1-Math.cos(r)) - u[2]*Math.sin(r));
        temp[2] = (float) (u[0]*u[2]*(1-Math.cos(r)) + u[1]*Math.sin(r));
        temp[3] = (float) (u[0]*u[1]*(1-Math.cos(r)) + u[2]*Math.sin(r));
        temp[4] = (float) (Math.cos(r) + u[1]*u[1]*(1-Math.cos(r)));
        temp[5] = (float) (u[1]*u[2]*(1-Math.cos(r)) - u[0]*Math.sin(r));
        temp[6] = (float) (u[0]*u[2]*(1-Math.cos(r)) - u[1]*Math.sin(r));
        temp[7] = (float) (u[1]*u[2]*(1-Math.cos(r)) + u[0]*Math.sin(r));
        temp[8] = (float) (Math.cos(r) + u[2]*u[2]*(1-Math.cos(r)));
        return temp;
    }
}
