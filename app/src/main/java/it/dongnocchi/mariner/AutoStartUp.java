package it.dongnocchi.mariner;

/**
 * Created by Diana & Giovanni on 21/09/2015.
 */

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class AutoStartUp extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        BootCompletedIntentReceiver.completeWakefulIntent(intent);
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        // do something when the service is created
        Intent dialogIntent = new Intent(this,MainActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);
    }


}