package com.clarifai.clarapplai.yingnan;

/**
 * Created by YingnanWang on 4/14/17.
 */

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.clarifai.clarapplai.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {

    private final String TAG = getClass().getSimpleName();

    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final String PERMISSION_READ_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;

    private TagDatabaseHelper dbHelper;
    private SQLiteDatabase db;

    @OnClick(R.id.btn_tag_a_photo) void launchTagActivity(){
        TagActivity.actionStart(MainActivity.this);
    }

    @OnClick(R.id.btn_plt_db) void launchVisualizeDBActivity(){
        VisualizeDBActivity.actionStart(MainActivity.this);
    }

    @OnClick(R.id.btn_edit_db) void launchEditDBActivity(){
        EditDBActivity.actionStart(MainActivity.this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // create db for the first time
        dbHelper = new TagDatabaseHelper(this);
        db = dbHelper.getWritableDatabase();
        db.close();

        if(!hasPermission()){
            requestPermission();
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_READ_STORAGE) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(PERMISSION_WRITE_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_READ_STORAGE) || shouldShowRequestPermissionRationale(PERMISSION_WRITE_STORAGE)) {
                Toast.makeText(this, "Storage permission are required for this demo", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[] {PERMISSION_READ_STORAGE, PERMISSION_WRITE_STORAGE}, PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: get all permisson");
                } else {
                    requestPermission();
                }
            }
        }
    }
}
