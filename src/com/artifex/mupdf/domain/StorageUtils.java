package com.artifex.mupdf.domain;

import android.content.Context;
import android.util.Log;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.appgeneration.magmanager.library.R;

public class StorageUtils {

    private static final String TAG = "StorageUtils";

    public static String getInternalPath(Context context) {
        return context.getDir("library", Context.MODE_PRIVATE).getAbsolutePath() + "/";
    }

    public static String getExternalPath(Context context) {
        return context.getExternalFilesDir(null).getAbsolutePath();
    }

    public static String getExternalCachePath(Context context) {
        return context.getExternalCacheDir().getAbsolutePath();
    }

    public static String getStoragePath(Context context) {
        if (context.getResources().getBoolean(R.bool.use_internal_storage)) {
            return getInternalPath(context);
        } else {
            return getExternalPath(context);
        }
    }

    /**
     * Move files between directorires
     *
     * @param src
     *            the source target
     * @param dst
     *            the destination target
     */
    public static int move(String src, String dst){
        if (src == null || dst == null) {
            return -1;
        }
        int count = -1;
        Log.d(TAG, "move " + src + " => " + dst);
        try {
            InputStream input = new FileInputStream(src);
            OutputStream output = new FileOutputStream(dst);
            byte data[] = new byte[1024];

            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();
        } catch (IOException e) {
            Log.e(TAG, "copyFromAssets failed", e);
        }
        new File(src).delete();
        return count;
    }

    public static String getStringFromFile(String path){
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Problem with open file", e);
            return null;
        }
        char[] buf = new char[1024];
        int numRead = 0;
        try {
            while ((numRead = reader.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
                buf = new char[1024];
            }
            reader.close();
        } catch (IOException e) {
            Log.e(TAG,"Problem with reading file", e);
            return null;
        }
        return fileData.toString();
    }
}
