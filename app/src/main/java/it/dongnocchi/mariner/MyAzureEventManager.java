package it.dongnocchi.mariner;

import android.content.Context;
import android.os.SystemClock;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.Base64;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/*
 * Created by DianaM on 27/08/2015.
 */

//==========================================================================
//  MAXIMUM SIZE FOR EVENT HUBS EVENTS: 256KB
//==========================================================================

//==========================================================================
public class MyAzureEventManager {
    //==========================================================================
    public static final short IntervalInHours = 24;
    public static final int CHECK_EVENTS_INTERVAL = IntervalInHours * 3600000; // conversion from hours to milliseconds

    private final short EVENT_READY = 0;
    private final short EVENT_BUSY = 1;
    private final short EVENT_FAILED = 2;
    private short Status = EVENT_READY; //meccanismo di protezione (contro le doppie spedizionei) __da completare__

    private List<String> EventsToSend = new ArrayList<>(); //not sent events memory
    private String EventsBuffer = "";
    private boolean isEmptingList = false;

    //private JSONObject JsonPacketToSend;

    MyConfiguration myConfig;
    //User user_local;
    Context context;

    boolean result = false;

    public AsyncResponse delegate = null;//Call back interface

    // Aggiunte da PM
//    private int PowerOnOffCounter = 0;
//    private int MotorOnOffCounter = 10;

    private String HubEndpoint = null;
    private String HubSasKeyName = null;
    private String HubSasKeyValue = null;
    //private String HubFullAccess = "<Enter Your DefaultFullSharedAccess Connection string>";

    // costruttore
    //==========================================================================
    public MyAzureEventManager(Context in_context, AsyncResponse asyncResponse, MyConfiguration mc) {
        //==========================================================================
        myConfig = mc;
        context = in_context;
        delegate = asyncResponse;//Assigning call back interfacethrough constructor

        //JsonPacketToSend = new JSONObject();
        /*CheckUpdates.run();
        handler.postDelayed(CheckUpdates, 1000);*/
    }

    //TODO: implementare un httpresponsehandler per ogni event hub
    AsyncHttpResponseHandler MyAsyncHttpResponseHandler = new AsyncHttpResponseHandler() {
        @Override
        //==========================================================================
        public void onSuccess(int i, Header[] headers, byte[] bytes) {
            //==========================================================================
            Status = EVENT_READY;
            result = true;

            if (isEmptingList) {
                EventsToSend.remove(EventsToSend.size() - 1);
                if (EventsToSend.size() == 0) {
                    isEmptingList = false;
                }
            }

            if (EventsToSend.size() != 0) {
                isEmptingList = true;

                //TODO: verificare le funzionalità del metodo sottostante e rimplementarle (cancellato il 2016-0226) valutando la suddivisione in tre canali

                //Empty_EventsToSend_List();
            }

            delegate.processFinish("success");
        }

        @Override
        //==========================================================================
        public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
            //==========================================================================
            Status = EVENT_FAILED;
            result = false;

            if (!isEmptingList)
                EventsToSend.add(EventsToSend.size(), EventsBuffer);

            delegate.processFinish("failure");
        }
    };

    //==========================================================================
    private void SaveErrorLog(String msg) {
        //==========================================================================
        String StringToSend = "" + SystemClock.elapsedRealtime() + "\t" + msg + "\n";
        LogFile_Handler BkgSave_LogHandler = new LogFile_Handler(StringToSend);
        BkgSave_LogHandler.execute();
    }


    //*************************************************************************

    //Aggiunta da PM
    //*************************************************************************
    //==========================================================================
    protected short SendEventNew(String EventName, int EventValue, String Note) {
    //==========================================================================

        try {

            JSONObject DataToSend = new JSONObject();
            java.sql.Timestamp currentTimestamp = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());

            Status = EVENT_BUSY;
            DataToSend.put("WheelchairID", myConfig.WheelchairID); //puoi chiamarla più volte per mandare più param nello stesso evento
            DataToSend.put("TimeInfo", currentTimestamp);
            //DataToSend.put("Time", time_string);
            DataToSend.put("EventName", EventName);
            DataToSend.put("EventValue", EventValue);
            DataToSend.put("Status", 1);
            DataToSend.put("Note", Note); //puoi chiamarla più volte per mandare più param nello stesso evento

            //String s = DataToSend.toString();
            SendJsonEvent(DataToSend,myConfig.Events_EventHub_url, myConfig.Events_EventHub_connstring );

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return Status;
    }


    public void SendJsonEvent(JSONObject jso, String url, String connstring)
    {
        String sas;
        AsyncHttpClient client = new AsyncHttpClient();

        //JSONObject params = new JSONObject();
        try {
            //----------------------------------
            // authorization to be generated every time with the script

            ParseConnectionString(connstring);
            sas = generateSasToken(url);

            client.addHeader("Authorization", sas);
            //JSONObject ParamsToSend = new JSONObject();

            EventsBuffer = jso.toString();
            StringEntity entity = new StringEntity(jso.toString());
            client.post(context, url, entity, "application/json", MyAsyncHttpResponseHandler);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    //==========================================================================
    protected short SendHourlyEvent(String WheelchairID, String EventType,
                                    float HourlyUse, float BatteryLevel, int SignalStrenght,
                                    int PowerOnOffCounter, int MotorOnOffCounter,
                                    float Latitude, float Longitude,
                                    int WheelchairStatus, String note )
    {
        //==========================================================================

        try {

            JSONObject ParamsToSend = new JSONObject();

            Calendar calendar = Calendar.getInstance();
            java.sql.Timestamp currentTimestamp = new java.sql.Timestamp(calendar.getTime().getTime());
            //java.sql.Date date = new java.sql.Date(System.currentTimeMillis());
            //Timestamp timestamp = new Timestamp(date, );
            //Timestamp ts = new Timestamp(System.currentTimeMillis());
            //java.sql.Timestamp my_timestamp = new Timestamp(System.currentTimeMillis());

            Status = EVENT_BUSY;
            ParamsToSend.put("WheelchairID", WheelchairID); //puoi chiamarla più volte per mandare più param nello stesso evento
            ParamsToSend.put("EventType", EventType);
            ParamsToSend.put("TimeInfo", currentTimestamp.toString());
            //ParamsToSend.put("TimeStamp", currentTimestamp);
            //TODO: bisogna sistemare la questione dei timestamp

            //ParamsToSend.put("Time", time_string);
            ParamsToSend.put("HourlyUse", HourlyUse);
            ParamsToSend.put("PhoneBatteryLevel", BatteryLevel);
            ParamsToSend.put("SignalStrength", (float)SignalStrenght);
            ParamsToSend.put("NumberOfPowerOnOff", PowerOnOffCounter);
            ParamsToSend.put("NumberOfMotorOnOff", MotorOnOffCounter);
            ParamsToSend.put("Latitude", Latitude);
            ParamsToSend.put("Longitude", Longitude);
            ParamsToSend.put("Status", WheelchairStatus);
            ParamsToSend.put("Note", note); //puoi chiamarla più volte per mandare più param nello stesso evento

            SendJsonEvent(ParamsToSend, myConfig.HourlyUpdate_EventHub_url, myConfig.HourlyUpdate_EventHub_connstring );

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return Status;
    }

    public void sendEventTestClick() {
        try {
            //----------------------------------
            // authorization to be generated every time with the script

            JSONObject ParamsToSend = new JSONObject();

            java.sql.Timestamp currentTimestamp = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
            java.sql.Date currentDate = new java.sql.Date(Calendar.getInstance().getTime().getTime());

            ParamsToSend.put("WheelchairID", myConfig.WheelchairID); //puoi chiamarla più volte per mandare più param nello stesso evento
            ParamsToSend.put("TimeInfo", currentTimestamp.toString());
            //ParamsToSend.put("Time", time_string);
            ParamsToSend.put("EventName", "EVENT_PROVA");
            ParamsToSend.put("EventValue", (float)7f);
            ParamsToSend.put("Status", 1);

            ParamsToSend.put("Note", ""); //puoi chiamarla più volte per mandare più param nello stesso evento

            String test_json = ParamsToSend.toString();

            SendJsonEvent(ParamsToSend,myConfig.Events_EventHub_url ,myConfig.Events_EventHub_connstring );

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    /**
     * Example code from http://msdn.microsoft.com/library/azure/dn495627.aspx to
     * construct a SaS token from the access key to authenticate a request.
     *
     * @param uri The unencoded resource URI string for this operation. The resource
     *            URI is the full URI of the Service Bus resource to which access is
     *            claimed. For example,
     *            "http://<namespace>.servicebus.windows.net/<hubName>"
     */
    private String generateSasToken(String uri) {

        String targetUri;
        try {
            targetUri = URLEncoder
                    .encode(uri.toLowerCase(), "UTF-8")
                    .toLowerCase();

            long expiresOnDate = System.currentTimeMillis();
            int expiresInMins = 60; // 1 hour
            expiresOnDate += expiresInMins * 60 * 1000;
            long expires = expiresOnDate / 1000;
            String toSign = targetUri + "\n" + expires;

            // Get an hmac_sha1 key from the raw key bytes
            byte[] keyBytes = HubSasKeyValue.getBytes("UTF-8");
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");

            // Get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);

            // Compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(toSign.getBytes("UTF-8"));

            // Using android.util.Base64 for Android Studio instead of
            // Apache commons codec
            String signature = URLEncoder.encode(
                    Base64.encodeToString(rawHmac, Base64.NO_WRAP).toString(), "UTF-8");

            // Construct authorization string
            String token = "SharedAccessSignature sr=" + targetUri + "&sig="
                    + signature + "&se=" + expires + "&skn=" + HubSasKeyName;
            return token;
        } catch (Exception e) {
            e.printStackTrace();
            //DialogNotify("Exception Generating SaS", e.getMessage().toString());
        }

        return null;
    }

    /**
     * Example code from http://msdn.microsoft.com/library/azure/dn495627.aspx
     * to parse the connection string so a SaS authentication token can be
     * constructed.
     *
     * @param connectionString This must be the DefaultFullSharedAccess connection
     *                         string for this example.
     */
    private void ParseConnectionString(String connectionString)
    {
        String[] parts = connectionString.split(";");
        if (parts.length != 3)
            throw new RuntimeException("Error parsing connection string: "
                    + connectionString);

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].startsWith("Endpoint")) {
                this.HubEndpoint = "https" + parts[i].substring(11);
            } else if (parts[i].startsWith("SharedAccessKeyName")) {
                this.HubSasKeyName = parts[i].substring(20);
            } else if (parts[i].startsWith("SharedAccessKey")) {
                this.HubSasKeyValue = parts[i].substring(16);
            }
        }
    }


}

