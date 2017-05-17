package com.clarifai.clarapplai.yingnan;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by YingnanWang on 4/15/17.
 */

public class EmbedUtils {

//    private static final double EMBED_DIFF_THRESHOLD = 0.05;

    public static byte[] updateEmbed(byte[] orie, double[] newe, int currentCnt){
        DoubleBuffer dbuf = ByteBuffer.wrap(orie).asDoubleBuffer();
        double[] retDouble = new double[dbuf.capacity()];
        dbuf.get(retDouble);

        if(retDouble.length!=newe.length){
            Log.d("EmbedUtils", "updateEmbed: " + retDouble.length + ", " + newe.length);
        }

        // update with equal weight
        for(int i=0;i<retDouble.length;++i){
            retDouble[i] = (retDouble[i] * currentCnt + newe[i]) / (currentCnt + 1);
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(retDouble.length * Double.SIZE / Byte.SIZE);
        DoubleBuffer buf2 = byteBuffer.asDoubleBuffer();
        buf2.put(retDouble);

        return byteBuffer.array();
    }

    public static FaceTagDBContent finMatchEmbed(List<FaceTagDBContent> allFace, double[] newe, double embed_diff_thresh){

        FaceTagDBContent ret = new FaceTagDBContent();
        double minDist = Double.MAX_VALUE;

        for (FaceTagDBContent it: allFace) {
            double tmpDist = getEuclideanDist(it.getEmbedDoubleArr(), newe);

            Log.d("EmbedUtils", "finMatchEmbed: " + it.getTag() + ": " + tmpDist);
            if(tmpDist < embed_diff_thresh && tmpDist < minDist){
                ret = it;
                minDist = tmpDist;
            }
        }

        return ret;
    }

    public static double getEuclideanDist(double[] orie, double[] newe){
        if(orie.length!=newe.length){
            Log.d("EmbedUtils", "getEuclideanDist: " + orie.length + ", " + newe.length);
            return Double.MAX_VALUE;
        }

        double ret = 0;
        for(int i=0;i<orie.length;++i){
            ret += (orie[i] - newe[i]) * (orie[i] - newe[i]);
        }

        return Math.sqrt(ret);
    }
}
