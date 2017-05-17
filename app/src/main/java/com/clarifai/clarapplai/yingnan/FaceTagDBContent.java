package com.clarifai.clarapplai.yingnan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

/**
 * Created by YingnanWang on 4/15/17.
 */

public class FaceTagDBContent {

    private String tag;
    private double[] embed;
    private Bitmap bm = null;
    private int count;
    private boolean isFamily;

    private static final int thumbSZ = 300;

    public static final int EMBED_SIZE = 1024;

    public FaceTagDBContent(){
        tag = null;
        embed = new double[1];
        bm = null;
        count = 0;
        isFamily = false;
    }

    public FaceTagDBContent(String t, double[] e, Bitmap b, int c, boolean f){
        tag = t;
        embed = e;
        bm = Bitmap.createScaledBitmap(b, thumbSZ, thumbSZ, false);;
        count = c;
        isFamily = f;
    }

    public FaceTagDBContent(String t, double[] e, int c, boolean f){
        tag = t;
        embed = e;
        count = c;
        isFamily = f;
    }

    // from db query
    public FaceTagDBContent(String t, byte[] e, byte[] b, int c, int f){
        tag = t;
        DoubleBuffer dbuf = ByteBuffer.wrap(e).asDoubleBuffer();
        embed = new double[dbuf.capacity()];
        dbuf.get(embed);

        bm = BitmapFactory.decodeByteArray(b, 0, b.length);;
        count = c;
        if(f==0){
            isFamily = false;
        }else if(f==1){
            isFamily = true;
        }
    }

    public String getTag(){
        return tag;
    }

    public byte[] getEmbedByteArr(){
        ByteBuffer byteBuffer = ByteBuffer.allocate(embed.length * Double.SIZE / Byte.SIZE);
        DoubleBuffer buf = byteBuffer.asDoubleBuffer();
        buf.put(embed);

        return byteBuffer.array();
    }

    public double[] getEmbedDoubleArr(){ return embed; }

    public Bitmap getThumbnailBM(){ return bm; }

    public byte[] getBitmapByteArr(){
        if(bm==null){
            return new byte[thumbSZ*thumbSZ];
        }
        Log.d("data", "getBitmapByteArr: " + bm.getHeight() + ", " + bm.getWidth());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    public int getCount(){
        return count;
    }

    public int getIsFamily(){
        if(isFamily){
            return 1;
        }else{
            return 0;
        }
    }

    public boolean getIsFamilyBool(){
        return isFamily;
    }

    public boolean isVaild() {
        if(tag!=null){
            return true;
        }
        return false;
    }

    public String getIsFamilyText(){
        if(isFamily){
            return "Yes";
        }else{
            return "No";
        }
    }
}
