package com.clarifai.clarapplai.yingnan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by YingnanWang on 4/15/17.
 */

public class TagDatabaseHelper extends SQLiteOpenHelper {

    private final String TAG = getClass().getSimpleName();
    private static final String DB_NAME = "FaceTagDB.db";
    private static final String TABLE_NAME = "FaceTag";
    private static final int DB_VERSION = 1;

    private static final String CREATE_TAG_DB = "create table FaceTag ("
            + "id integer primary key autoincrement, "
            + "tag text, "
            + "embed blob, "
            + "thumbnail blob, "
            + "count integer, "
            + "isfamily integer)";

    private Context mContext;

    public TagDatabaseHelper(Context context){
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TAG_DB);
        Log.d(TAG, "onCreate: success");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public boolean add(FaceTagDBContent data){
        SQLiteDatabase db = getWritableDatabase();

        boolean ifTagExist = false;
        Cursor c = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE TRIM(tag) = '"+data.getTag().trim()+"'", null
        );
        int cnt = 0;
        if(c.moveToFirst()){
            do{
                cnt++;
                if(cnt>1){
                    throw new SQLException("Find multiple same name tag in the database");
                }
            }while(c.moveToNext());
        }

        if(cnt==1){
            ifTagExist = true;
        }

        ContentValues values = new ContentValues();
        if(!ifTagExist){
            // add
            Log.d(TAG, "add: add" + data.getEmbedByteArr().length + ", " + data.getBitmapByteArr().length);
            values.put("tag", data.getTag());
            values.put("embed", data.getEmbedByteArr());
            values.put("thumbnail", data.getBitmapByteArr());
            values.put("count", data.getCount());
            values.put("isfamily", data.getIsFamily());
            db.insert(TABLE_NAME, null, values);
        }else{
            // update embed, count and isFamily
            Log.d(TAG, "add: update");
            c.moveToFirst();
            values.put("embed",
                    EmbedUtils.updateEmbed(c.getBlob(c.getColumnIndex("embed")), data.getEmbedDoubleArr(), c.getInt(c.getColumnIndex("count")))
            );
            values.put("count", c.getInt(c.getColumnIndex("count")) + data.getCount());
            values.put("isfamily", data.getIsFamily());
            db.update(TABLE_NAME, values, "tag = ?", new String[]{ data.getTag() });
        }
        c.close();
        db.close();

        return ifTagExist;
    }

    public void delete(String tag){
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, "tag = ?", new String[]{ tag });
        db.close();
    }

    public List<FaceTagDBContent> queryAll(){
        List<FaceTagDBContent> ret = new ArrayList<FaceTagDBContent>();

        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(TABLE_NAME, null, null, null, null, null, null);

        if(c.moveToFirst()){
            do{
                FaceTagDBContent tmp = new FaceTagDBContent(
                        c.getString(c.getColumnIndex("tag")),
                        c.getBlob(c.getColumnIndex("embed")),
                        c.getBlob(c.getColumnIndex("thumbnail")),
                        c.getInt(c.getColumnIndex("count")),
                        c.getInt(c.getColumnIndex("isfamily"))
                );
                ret.add(tmp);
            }while(c.moveToNext());
        }

        c.close();
        db.close();

        return ret;
    }
}
