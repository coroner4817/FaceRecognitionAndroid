package com.clarifai.clarapplai.yingnan;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.clarifai.clarapplai.R;
import com.github.abel533.echarts.Label;
import com.github.abel533.echarts.Option;
import com.github.abel533.echarts.axis.CategoryAxis;
import com.github.abel533.echarts.axis.ValueAxis;
import com.github.abel533.echarts.code.Orient;
import com.github.abel533.echarts.code.Trigger;
import com.github.abel533.echarts.code.X;
import com.github.abel533.echarts.code.Y;
import com.github.abel533.echarts.data.Data;
import com.github.abel533.echarts.series.Graph;
import com.github.abel533.echarts.series.Line;
import com.github.abel533.echarts.series.Pie;
import com.github.abel533.echarts.style.ItemStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by YingnanWang on 4/16/17.
 */

/**
 * Reference my previous project: https://github.com/abel533/ECharts
 * Deploy echarts on android to show the pca result
 */

public class VisualizeDBActivity extends BaseActivity {

    private final String TAG = getClass().getSimpleName();

    private static final int PC_SPACE_DIM = 2;
    private static final boolean ifDebug = false;

    private TagDatabaseHelper dbHelper;
    private List<FaceTagDBContent> dbAll;
    private List<PointF> pcPoints;

    private PrincipalComponentAnalysis pcaHelper = new PrincipalComponentAnalysis();

    @BindView(R.id.pbar_visuldb) ProgressBar mVisualDbPbar;
    @BindView(R.id.webview_visual_db) WebView mEchartsWebview;

    public static void actionStart(Context context){
        Intent intent = new Intent(context, VisualizeDBActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acticity_visuldb);
        ButterKnife.bind(this);

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mVisualDbPbar.setVisibility(View.VISIBLE);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                dbHelper = new TagDatabaseHelper(VisualizeDBActivity.this);
                dbAll = dbHelper.queryAll();

                //PCA
                try{
                    pcPoints = new ArrayList<PointF>();
                    pcaHelper.setup(dbAll.size(), FaceTagDBContent.EMBED_SIZE);
                    for(FaceTagDBContent ft : dbAll){
                        pcaHelper.addSample(ft.getEmbedDoubleArr());
                    }
                    pcaHelper.computeBasis(PC_SPACE_DIM);
                    for(FaceTagDBContent ft : dbAll){
                        double[] pcProjection = pcaHelper.sampleToEigenSpace(ft.getEmbedDoubleArr());
                        pcPoints.add(new PointF((float)pcProjection[0], (float)pcProjection[1]));
                    }

                }catch (IllegalArgumentException e){
                    return false;
                }

                for(PointF p : pcPoints){
                    Log.d(TAG, "pc Point: " + p);
                }

                threadSleep(1000);
                return true;
            }

            @Override
            protected void onPostExecute(Boolean pcaSuccess) {
                super.onPostExecute(pcaSuccess);
                mVisualDbPbar.setVisibility(View.INVISIBLE);

                if(!pcaSuccess){
                    Toast.makeText(VisualizeDBActivity.this,
                            "Database is not big enough for perform PCA, add " + (PC_SPACE_DIM-dbAll.size()+1) + " more",
                            Toast.LENGTH_LONG).show();
                }else{
                    if(dbAll.size()==0){
                        Toast.makeText(VisualizeDBActivity.this, "Database is empty", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }.execute();

        mEchartsWebview.getSettings().setAllowFileAccess(true);
        mEchartsWebview.getSettings().setJavaScriptEnabled(true);
        mEchartsWebview.setFitsSystemWindows(true);
        mEchartsWebview.getSettings().setBuiltInZoomControls(true);
        mEchartsWebview.getSettings().setDisplayZoomControls(true);
//        mEchartsWebview.getSettings().setLoadWithOverviewMode(true);
//        mEchartsWebview.getSettings().setUseWideViewPort(true);
        mEchartsWebview.loadUrl("file:///android_asset/index.html");
        mEchartsWebview.setWebViewClient(new MyWebViewClient());
    }

    private void threadSleep(int ms){
        try{
            Thread.sleep(ms);
        }catch(InterruptedException e){
            Log.e(TAG, "threadSleep: ");
        }
    }

    private class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            showPCAPlot();
            mVisualDbPbar.setVisibility(View.INVISIBLE);
        }
    }
    
    private void showPCAPlot(){
        Log.d(TAG, "showPCAPlot: ");

        mEchartsWebview.loadUrl("javascript:clear()");

        Option option = new Option();
        option.title().text("Embed Top 2 Principal Component Projection").x("center");
        option.legend().data("Family Member", "Not Family Member").y("bottom");

        ValueAxis valueAxisX = new ValueAxis();
        ValueAxis valueAxisY = new ValueAxis();
        valueAxisX.setName("PC1");
        valueAxisY.setName("PC2");
        option.xAxis(valueAxisX);
        option.yAxis(valueAxisY);

        ItemStyle blackDataStyle = new ItemStyle();
        blackDataStyle.normal().setColor("#000000");

        Graph graphAll = new Graph();
        graphAll.name("Family Member")
                .symbolSize(50)
                .label().normal().setShow(true);

        for(int i=0;i<pcPoints.size();++i){
            if(dbAll.get(i).getIsFamilyBool()){
                graphAll.data(
                        new Data().setName(dbAll.get(i).getTag()).setX(pcPoints.get(i).x).setY(pcPoints.get(i).y)
                );
            }else{
                graphAll.data(
                        new Data().setName(dbAll.get(i).getTag()).setX(pcPoints.get(i).x).setY(pcPoints.get(i).y).itemStyle(blackDataStyle)
                );
            }
        }

        // for show legend only
        Graph graphTmp = new Graph();
        graphTmp.name("Not Family Member");

        option.series(graphAll, graphTmp);

        Gson gson = new Gson();
        String jsonOption = gson.toJson(option);
        mEchartsWebview.loadUrl("javascript:setOption("+jsonOption+")");

        if(ifDebug){
            Gson gsonBeautiful = new GsonBuilder().setPrettyPrinting().create();
            String json = gsonBeautiful.toJson(option);
            System.out.println(json);
        }
    }
}
