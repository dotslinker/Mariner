package it.dongnocchi.mariner;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;

/**
 * Created by Giovanni on 26/06/2015.
 */
public class UpdateApp extends AsyncTask<String,Void,Boolean> {
    private Context context;
    private String ApkPath="";

    public void setContext(Context contextf){
        context = contextf;
    }

    //    User user;

    public UpdateApp(String path) {
        ApkPath = path;
    }

    @Override
    protected Boolean doInBackground(String... arg0) {
        try {

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(ApkPath)), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without this flag android returned a intent error!
            context.startActivity(intent);
            return true;

        } catch (Exception e) {
            Log.e("UpdateAPP", "Update error! " + e.getMessage());
            SaveErrorLog(e.toString());
            return false;
        }

    }
    //==========================================================================
    private void SaveErrorLog(String msg){
        //==========================================================================
        String StringToSend = "" + SystemClock.elapsedRealtime() + "\t" + msg +"\n";
        LogFile_Handler BkgSave_LogHandler = new LogFile_Handler(StringToSend);
        BkgSave_LogHandler.execute();
    }
}