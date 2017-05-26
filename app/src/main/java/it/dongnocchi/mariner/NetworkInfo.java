package it.dongnocchi.mariner;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.telephony.CellSignalStrength;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by DianaM on 18/09/2015.
 */
public class NetworkInfo extends PhoneStateListener {

    private int mySignalStrength = 0;

    //==========================================================================
    public String getNetworkClass(Context context) {
        //==========================================================================
        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = mTelephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "GPRS";
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "EDGE";
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "Others";
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return "Others";
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "UMTS";
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return "Others";
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return "Others";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "HSUPA";
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "HSPA";
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return "Others";
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return "Others";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            default:
                return "Unknown";
        }
    }

    /*
    protected boolean isInternetOnline()
    {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                if (activeNetwork.isConnected())
                    haveConnectedWifi = true;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                if (activeNetwork.isConnected())
                    haveConnectedMobile = true;
            }
        }

        return haveConnectedWifi || haveConnectedMobile;
    }
    */


    //TODO: verificare questa parte del valore del segnale restituito.

    //==========================================================================
    public int getSignalStrength() {
        return mySignalStrength;
    }

    //==========================================================================


    //TODO: implementare qualcosa che consenta di avere il valore di segnale anche all'inizio senza dovere attendere il cambio di

/*
    @Override
    //==========================================================================
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        //==========================================================================
        super.onSignalStrengthsChanged(signalStrength);
        mySignalStrength = signalStrength.getGsmSignalStrength();
        mySignalStrength = (2 * mySignalStrength) - 113; // -> dBm
    }
*/

    @Override
    //==========================================================================
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        //==========================================================================
        super.onSignalStrengthsChanged(signalStrength);
        if (signalStrength.isGsm()) {
            if (signalStrength.getGsmSignalStrength() != 99)
                mySignalStrength = signalStrength.getGsmSignalStrength() * 2 - 113;
            else
                mySignalStrength = signalStrength.getGsmSignalStrength();
        } else {
            mySignalStrength = signalStrength.getCdmaDbm();
        }
    }



    //==========================================================================
    protected boolean MyWiFiManager(Context context, boolean enable) {
    //==========================================================================
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifi.setWifiEnabled(enable);
    }


    /*//==========================================================================
        protected void setMobileDataEnabled(Context context, boolean enabled) {
        //==========================================================================
            ConnectivityManager dataManager;
            dataManager  = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Method dataMtd = null;
            try {
                dataMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
                dataMtd.setAccessible(true);
                dataMtd.invoke(dataManager, true);        //True - to enable data connectivity .
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }*/
}
