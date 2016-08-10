package it.dongnocchi.mariner;

/**
 * Created by Diana & Giovanni on 21/09/2015.
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.widget.Toast;

public class BootCompletedIntentReceiver extends WakefulBroadcastReceiver {
    private static boolean Starting = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            if (Starting) {
                Starting = false;
                Intent pushIntent = new Intent(context, AutoStartUp.class);
                context.startService(pushIntent);
            }
            else
                call_toast("cicciobello");
        }
    }

    //==========================================================================
    private void call_toast(CharSequence text){
        //==========================================================================
        // SETS A KIND OF POP-UP MESSAGE
        //Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(null, text, duration);
        toast.show();
    }

}