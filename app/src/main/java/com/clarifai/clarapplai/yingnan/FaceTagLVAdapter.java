package com.clarifai.clarapplai.yingnan;

import android.content.Context;

import com.clarifai.clarapplai.R;
import com.yuyh.easyadapter.abslistview.EasyLVAdapter;
import com.yuyh.easyadapter.abslistview.EasyLVHolder;

import java.util.List;

/**
 * Created by YingnanWang on 4/16/17.
 */

/**
 * From: https://github.com/smuyyh/EasyAdapter
 */

public class FaceTagLVAdapter extends EasyLVAdapter<FaceTagDBContent>{

    public FaceTagLVAdapter(Context context, List<FaceTagDBContent> list, int... layoutIds) {
        super(context, list, layoutIds);
    }

    @Override
    public void convert(EasyLVHolder holder, int position, FaceTagDBContent faceTagDBContent) {
        holder.setImageBitmap(R.id.facetag_lvpat_imgv, faceTagDBContent.getThumbnailBM());
        holder.setText(R.id.facetag_lvpat_tag_tv, faceTagDBContent.getTag());
        holder.setText(R.id.facetag_lvpat_cnt_tv, faceTagDBContent.getCount()+"");
        holder.setText(R.id.facetag_lvpat_isfamily_tv, faceTagDBContent.getIsFamilyText());
    }
}
