package com.clarifai.clarapplai.yingnan;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by YingnanWang on 4/14/17.
 */

public class BaseActivity extends AppCompatActivity{

    private final String TAG = getClass().getSimpleName();

    public Context getBaseActivityContext(){
        return BaseActivity.this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCollector.addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
    }
}
