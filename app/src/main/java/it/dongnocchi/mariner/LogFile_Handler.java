package it.dongnocchi.mariner;

import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.Environment.getExternalStorageDirectory;

/**
 * Created by DianaM on 19/08/2015.
 */

/* ===========================================
* ========LOG FILE FORMAT:====================
* string must be composed by:
*
* - TIME: SystemClock.elapsedRealtime();
* - "\t"
* - ERROR MESSAGE
* - "\n"
*
* ============================================*/

public class LogFile_Handler extends AsyncTask<Void, Boolean, Boolean> {

    String StringToLog = "";

    // costruttore
    //==========================================================================
    public LogFile_Handler( String msg_to_log ) {
        //==========================================================================

        StringToLog = msg_to_log;
    }

    @Override
    //==========================================================================
    protected Boolean doInBackground(Void... params) {
        //==========================================================================

        FileOutputStream outputStream;

        // create folder if it doesn't exist
        File LogFilesFolder = new File(getExternalStorageDirectory().getAbsolutePath() + "/WheelchairData" + "/LogFiles");
        if (!LogFilesFolder.exists()) {
            LogFilesFolder.mkdir();
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd");
        Date today = new Date();

        //File LogFilePath = new File( LogFilesFolder + "/" + formatter.format(today).toString() + ".txt");
        File LogFilePath = new File( LogFilesFolder + "/" + formatter.format(today) + ".txt");

        try {
            outputStream = new FileOutputStream(LogFilePath, true);//true=append
            outputStream.write(StringToLog.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }
}


