package com.clarifai.clarapplai.yingnan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.clarifai.clarapplai.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by YingnanWang on 4/16/17.
 */

public class EditDBActivity extends BaseActivity {
    private final String TAG = getClass().getSimpleName();

    public static void actionStart(Context context){
        Intent intent = new Intent(context, EditDBActivity.class);
        context.startActivity(intent);
    }

    @BindView(R.id.lv_editdb) ListView mEditDBListView;
    @BindView(R.id.pbar_editdb) ProgressBar mEditDbPbar;

    private TagDatabaseHelper dbHelper;
    private FaceTagLVAdapter mLVAdapter;
    private List<FaceTagDBContent> dbAll = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editdb);
        ButterKnife.bind(this);

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mEditDbPbar.setVisibility(View.VISIBLE);
            }

            @Override
            protected Void doInBackground(Void... params) {
                dbHelper = new TagDatabaseHelper(EditDBActivity.this);
                dbAll = dbHelper.queryAll();
                threadSleep(1000);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mLVAdapter = new FaceTagLVAdapter(EditDBActivity.this, dbAll, R.layout.facetag_listview_pattern);
                mEditDBListView.setAdapter(mLVAdapter);

                if(dbAll.size()==0){
                    Toast.makeText(EditDBActivity.this, "Database is empty", Toast.LENGTH_SHORT).show();
                }
                mEditDbPbar.setVisibility(View.INVISIBLE);
            }
        }.execute();

        mEditDBListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                final int deleteIdx = i;
                new AlertDialog.Builder(EditDBActivity.this)
                        .setTitle("Delete Alert")
                        .setMessage("Do you want to delete tag " + dbAll.get(deleteIdx).getTag() + " from database?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dbHelper.delete(dbAll.get(deleteIdx).getTag());
                                mLVAdapter.remove(deleteIdx);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();

                return false;
            }
        });
    }

    private void threadSleep(int ms){
        try{
            Thread.sleep(ms);
        }catch(InterruptedException e){
            Log.e(TAG, "threadSleep: ");
        }
    }

}
