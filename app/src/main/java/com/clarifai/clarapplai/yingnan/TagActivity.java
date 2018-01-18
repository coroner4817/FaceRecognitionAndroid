package com.clarifai.clarapplai.yingnan;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.clarifai.clarapplai.R;
//import com.clarifai.clarapplai.api.*;
import com.clarifai.clarapplai.util.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.api.ClarifaiResponse;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.input.image.ClarifaiImage;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Embedding;
import cn.refactor.library.SmoothCheckBox;
import okhttp3.Response;

/**
 * Created by YingnanWang on 4/14/17.
 */

public class TagActivity extends BaseActivity {

    private final String TAG = getClass().getSimpleName();

    private static final int PICK_IMAGE_ID = 4817;
    private static final int MSG_START_FIND_FACE = 1;
    private static final int MSG_START_REQUEST_API = 2;
    private static final int MSG_START_MATCHING_FACE = 3;
    private static final int MSG_GET_TAG_RESULT_FOUND = 4;
    private static final int MSG_GET_TAG_RESULT_NOTFOUND = 5;

    private static final int MSG_FOUND_NO_FACE = 10;
    private static final int MSG_API_ERROR = 20;
    private static final int MSG_INTERNET_ERROR = 21;


    private boolean isBGWorking = false;

    private boolean imgPickerWithNewResult = false;
    private boolean recvValidEmbedResult = false;

    private Bitmap mSelectBitmap = null;
    private Bitmap mCropedBitmap = null;
    private FaceTagDBContent findFace = new FaceTagDBContent();

    private FaceCropper mFaceCropper = new FaceCropper();
    private Point faceInit;
    private Point faceEnd;

    // old api
//    private ClarifaiAPI mClarifaiAPI = new ClarifaiAPI(appID, appSecret);
    // new
    final ClarifaiClient client = new ClarifaiBuilder(appID, appSecret).buildSync();
    private double[] embedResult;

    private TagDatabaseHelper dbHelper;

    @BindView(R.id.pbar_tag) ProgressBar mTagProgressBar;
    @BindView(R.id.view_tag) TagDrawView mTagDrawView;
    @BindView(R.id.imgv_base) ImageView mBaseImageView;
    @BindView(R.id.tv_tag_status) TextView mStatusTV;
    @BindView(R.id.btn_repick) Button mRepickBtn;
    @BindView(R.id.btn_tag_submit) Button mTagSubmitBtn;
    @BindView(R.id.btn_add_to_db) Button mAddToDbBtn;
    @BindView(R.id.edit_tag) EditText mTagEditText;
    @BindView(R.id.checkbox_isfamily) SmoothCheckBox mCheckIsFamily;


    public static void actionStart(Context context){
        Intent intent = new Intent(context, TagActivity.class);
        context.startActivity(intent);
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_START_FIND_FACE:
                    mStatusTV.setText("Finding face...");
                    break;
                case MSG_START_REQUEST_API:
                    mStatusTV.setText("Requesting embed...");
                    Point tmpInit = ((ArrayList<Point>)msg.obj).get(0);
                    Point tmpEnd = ((ArrayList<Point>)msg.obj).get(1);
                    Point tmpCropSZ = ((ArrayList<Point>)msg.obj).get(2);

                    mTagDrawView.startAnim(
                            (tmpInit.x+tmpEnd.x)/2,
                            (tmpInit.y+tmpEnd.y)/2,
                            tmpCropSZ.x,
                            mBaseImageView.getImageMatrix(), mBaseImageView.getDrawable().getIntrinsicWidth(), mBaseImageView.getDrawable().getIntrinsicHeight());
                    break;
                case MSG_START_MATCHING_FACE:
                    mStatusTV.setText("Matching face...");
                    break;
                case MSG_FOUND_NO_FACE:
                    mStatusTV.setText("Find no face, please select another photo");
                    onTagFinish();
                    break;
                case MSG_API_ERROR:
                    mStatusTV.setText("Invalid response from Clarifai");
                    onTagFinish();
                    break;
                case MSG_INTERNET_ERROR:
                    mStatusTV.setText("No internet connection");
                    onTagFinish();
                    break;
                case MSG_GET_TAG_RESULT_FOUND:
                    mStatusTV.setText("Find a matching!");
                    // update edittext, checkbox, plot tag
                    mTagEditText.setText(findFace.getTag());
                    mTagEditText.requestFocus();
                    mCheckIsFamily.setChecked(findFace.getIsFamilyBool(), true);
                    mTagDrawView.plotTag(findFace.getTag());
                    onTagFinish();
                    break;
                case MSG_GET_TAG_RESULT_NOTFOUND:
                    mStatusTV.setText("Find a new face!");
                    mTagEditText.requestFocus();
                    onTagFinish();
                    break;
                default:
                    break;
            }
        }
    };

    private void onTagFinish(){
        enableInput();
        mTagDrawView.stopAnim();
        mTagProgressBar.setVisibility(View.INVISIBLE);
        //mBaseImageView.setImageBitmap(mCropedBitmap);
    }

    @OnClick(R.id.btn_repick) void repickPhoto(){
        if(isBGWorking){
            Toast.makeText(TagActivity.this, "Please Wait until finish", Toast.LENGTH_SHORT).show();
            return;
        }
        imgPickerWithNewResult = false;
        onPickImage();
    }

    @OnClick(R.id.btn_tag_submit) void submitBitmap(){
        if(isBGWorking){
            Toast.makeText(TagActivity.this, "Please Wait until finish", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!imgPickerWithNewResult){
            Toast.makeText(TagActivity.this, "Please repick a new photo", Toast.LENGTH_SHORT).show();
        }else{
            disableInput();
            imgPickerWithNewResult = false;
            recvValidEmbedResult = false;
            mTagProgressBar.setVisibility(View.VISIBLE);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    //crop face
                    sendMsgToHandler(MSG_START_FIND_FACE);
                    try{
                        // recycle my original bitmap!
                        mCropedBitmap = mFaceCropper.getCroppedImage(mSelectBitmap.copy(mSelectBitmap.getConfig(), true), 1);
                    }catch (Exception e){
                        sendMsgToHandler(MSG_FOUND_NO_FACE);
                        return;
                    }
                    threadSleep(1000);

                    if(mFaceCropper.getIsValidFace()){
                        // on face found
                        faceInit = mFaceCropper.getCropResult().getInit();
                        faceEnd = mFaceCropper.getCropResult().getEnd();

                        sendMsgToHandler(MSG_START_REQUEST_API, faceInit, faceEnd, mFaceCropper.getCropedSz());

                        // using com.clarifai.clarapplai, not working
                        // seems that under api.clarifai.com/v1, there is only tag, feedback and info. There is no embed directory
//                        ClarifaiEmbeddingRequest r = mClarifaiAPI.requestEmbeddings(mCropedBitmap);
//                        r.requestAsync(new ClarifaiEmbeddingRequest.Callback() {
//                            @Override
//                            public void onSuccess(ClarifaiEmbeddingResult result) {
//                                // on success
//                                embedResult = result.getEmbeddings();
//
//                                recvValidEmbedResult = true;
//                                // matching face
//                                sendMsgToHandler(MSG_START_MATCHING_FACE);
//                                threadSleep(1000);
//                                List<FaceTagDBContent> allFace = dbHelper.queryAll();
//                                findFace = EmbedUtils.finMatchEmbed(allFace, embedResult);
//                                if(findFace.isVaild()){
//                                    // on match
//                                    sendMsgToHandler(MSG_GET_TAG_RESULT_FOUND);
//                                }else{
//                                    // on new face
//                                    sendMsgToHandler(MSG_GET_TAG_RESULT_NOTFOUND);
//                                }
//                            }
//
//                            @Override
//                            public void onUnsuccessfulResponse(Response response) {
//                                Log.e(TAG, "onUnsuccessfulResponse: " + response.toString());
//                                sendMsgToHandler(MSG_API_ERROR);
//                            }
//
//                            @Override
//                            public void onNetworkFailure(IOException exception) {
//                                Log.e(TAG, "onUnsuccessfulResponse: " + exception.toString());
//                                sendMsgToHandler(MSG_INTERNET_ERROR);
//                            }
//                        });

                        // dummy test
//                        embedResult = randGen(1024);
//                        threadSleep(3000);

                        try{
                            ClarifaiResponse<List<ClarifaiOutput<Embedding>>> embeddings = client.getDefaultModels().generalEmbeddingModel().predict()
                                    .withInputs(ClarifaiInput.forImage(ClarifaiImage.of(getCroppedBitmapByteArray()))).executeSync();
                            embedResult = convertFloatsToDoubles(embeddings.get().get(0).data().get(0).embedding());

                            recvValidEmbedResult = true;
                            // matching face
                            sendMsgToHandler(MSG_START_MATCHING_FACE);
                            threadSleep(1000);
                            List<FaceTagDBContent> allFace = dbHelper.queryAll();
                            findFace = EmbedUtils.finMatchEmbed(allFace, embedResult, 0.6);
                            if(findFace.isVaild()){
                                // on match
                                sendMsgToHandler(MSG_GET_TAG_RESULT_FOUND);
                            }else{
                                // on new face
                                sendMsgToHandler(MSG_GET_TAG_RESULT_NOTFOUND);
                            }

                        }catch (Exception e){
                            Log.e(TAG, "onUnsuccessfulResponse: " + e.toString());
                            sendMsgToHandler(MSG_INTERNET_ERROR);
                        }

                    }else{
                        // on face not found
                        sendMsgToHandler(MSG_FOUND_NO_FACE);
                    }
                }
            }).start();
        }
    }

    @OnClick(R.id.btn_add_to_db) void addToDB(){
        if(isBGWorking){
            Toast.makeText(TagActivity.this, "Please Wait until finish", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!recvValidEmbedResult){
            Toast.makeText(TagActivity.this, "Nothing to be added", Toast.LENGTH_SHORT).show();
        }else{

            if(mTagEditText.getText().toString().isEmpty()){
                Toast.makeText(TagActivity.this, "Please input a tag", Toast.LENGTH_SHORT).show();
                return;
            }
            recvValidEmbedResult = false;

            new AsyncTask<FaceTagDBContent, Void, Boolean>() {

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    disableInput();
                    mTagProgressBar.setVisibility(View.VISIBLE);
                }

                @Override
                protected Boolean doInBackground(FaceTagDBContent... params) {
                    threadSleep(1000);
                    return dbHelper.add(params[0]);
                }

                @Override
                protected void onPostExecute(Boolean isExist) {
                    super.onPostExecute(isExist);
                    mTagProgressBar.setVisibility(View.INVISIBLE);
                    mTagEditText.clearFocus();
                    if(isExist){
                        mStatusTV.setText("Updated Database");
                    }else{
                        mStatusTV.setText("Added to Database");
                    }
                    enableInput();
                }

            }.execute(new FaceTagDBContent(
                    mTagEditText.getText().toString(),
                    embedResult,
                    mCropedBitmap,
                    1,
                    mCheckIsFamily.isChecked()
            ));
        }
    }

    private void disableInput(){
        isBGWorking = true;
        mTagEditText.setEnabled(false);
        mCheckIsFamily.setEnabled(false);
    }

    private void enableInput(){
        isBGWorking = false;
        mTagEditText.setEnabled(true);
        mCheckIsFamily.setEnabled(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag);
        ButterKnife.bind(this);

        dbHelper = new TagDatabaseHelper(this);

        mTagProgressBar.setVisibility(View.INVISIBLE);
        mBaseImageView.setImageBitmap(null);
        mFaceCropper.setMaxFaces(1);

        onPickImage();
    }

    public void onPickImage() {
        Intent chooseImageIntent = ImagePicker.getPickImageIntent(this);
        startActivityForResult(chooseImageIntent, PICK_IMAGE_ID);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case PICK_IMAGE_ID:
                try{
                    if(resultCode==RESULT_OK && data!=null){
                        // On Pick success
                        mTagDrawView.clearCanvas();
                        mStatusTV.setText("");
                        mSelectBitmap = ImagePicker.getImageFromResult(this, resultCode, data);
                        mBaseImageView.setImageBitmap(mSelectBitmap);
                        imgPickerWithNewResult = true;
                        mTagEditText.setText("");
                        mTagEditText.clearFocus();
                        mCheckIsFamily.setChecked(false, false);
                    }
                }catch(NullPointerException e){
                    Log.d(TAG, "onActivityResult: " + "backpressed before pick photo");
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private double[] randGen(int sz){
        double[] ret = new double[sz];
        Random rand = new Random();

        for(int i=0;i<ret.length;++i){
            ret[i] = rand.nextDouble();
        }

        return ret;
    }

    private void threadSleep(int ms){
        try{
            Thread.sleep(ms);
        }catch(InterruptedException e){
            Log.e(TAG, "randGen: ");
        }
    }

    private void sendMsgToHandler(int code){
        Message msg = new Message();
        msg.what = code;
        mHandler.sendMessage(msg);
    }

    private void sendMsgToHandler(int code, String str){
        Message msg = new Message();
        msg.what = code;
        msg.obj = str;
        mHandler.sendMessage(msg);
    }

    private void sendMsgToHandler(int code, final Point p1, final Point p2, final Point cropSZ){
        Message msg = new Message();
        msg.what = code;
        msg.obj = new ArrayList<Point>(){{
            add(p1);
            add(p2);
            add(cropSZ);
        }};
        mHandler.sendMessage(msg);
    }

    private static double[] convertFloatsToDoubles(float[] input)
    {
        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = input[i];
        }
        return output;
    }

    private byte[] getCroppedBitmapByteArray(){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        mCropedBitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    @Override
    public void onBackPressed() {
        if(isBGWorking){
            Toast.makeText(TagActivity.this, "Please Wait until finish", Toast.LENGTH_SHORT).show();
            return;
        }

        super.onBackPressed();
    }
}
