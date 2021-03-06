package it.dongnocchi.mariner;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.util.Log;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.Thing;
import com.yoctopuce.YoctoAPI.YAPI;
import com.yoctopuce.YoctoAPI.YAPI_Exception;
import com.yoctopuce.YoctoAPI.YDigitalIO;
import com.yoctopuce.YoctoAPI.YModule;

import static com.yoctopuce.YoctoAPI.YDigitalIO.FindDigitalIO;

//==========================================================================
public class MainActivity extends Activity
        implements SensorEventListener, YAPI.DeviceArrivalCallback, YAPI.DeviceRemovalCallback, YDigitalIO.UpdateCallback {
//==========================================================================

    //xxyyy xx = major release, yyy = minor release
    public final int CURRENT_BUILD = 1045;

    public boolean debug_mode = false; //flag to enable debug mode of the application

    public final String TAG = MainActivity.class.getSimpleName();

    //TODO: da verificare la gestione dello status

    public final int STATUS_INIT = 0;
    public final int STATUS_SLEEP = 1;
    public final int STATUS_IDLE = 2;
    public final int STATUS_ACTIVE = 3;
    public final int STATUS_DAILY_UPDATE = 4;
    public final int STATUS_OFFLINE = 5;
    public final String[] STATUS_STRING = {"INIT", "SLEEP", "IDLE", "ACTIVE", "DAILY_UPDATE", "OFFLINE"};

    final int MAX_LOGFILE_SIZE = 20000000;
    private final int ACC_READING_PERDIOD = 20000; // 20 ms

    private final int GYRO_READING_PERDIOD = 20000; //20 ms

    private final int TEMPERATURE_READING_PERDIOD = 10000000; //10 s

    private final long delta_temperature_reading = 10000;
    private long LastTemperatureReadingTime, CurrentTemperatureReadingTime;

    // MAX Int = 4294967296

    private final int LIGHT_READING_PERDIOD = 10000000; //1s

    //During the Calibration, we require a less frequent sampling period
    private final int ACC_READING_PERDIOD_CALIB = 50000;
    private final int GYRO_READING_PERDIOD_CALIB = 50000;
    //private static final int NUM_OF_TEMPERARURE_SAMPLES = 8650; //8640 sarebbe il numero corretto
    //private static final int NUM_OF_SIGNAL_STRENGTH_SAMPLES = 8650; //8640 sarebbe il numero corretto
    private final int NUM_OF_SECONDS_CALIBRATION = 10;
    //private static final int CALIB_DATA_SIZE = 1000; // 500 per 10 secondi
    //Reference date reporting the reset performed at the very first start
    //and at the reset each night. Needs to be evaluated very close to Daily_referece_time
    static Date Daily_Reference_Date;
    //daily Reference time to be used for timestamping the acquisitions
    static long Daily_Reference_Time;
    static long Hourly_Reference_Time;

    static long Hourly_Alarm_Reference_Time;

    // App starting date - this is unique until the app restarts
    static Date App_Start_Date;
    // App starting time
    static long AppStartTime;

    //TODO: da sistemare per bene nella versione definitiva di produzione
    final int UXUPDATE_PERIOD_IN_MILLIS_SLOW = 1000;
    final int UXUPDATE_PERIOD_IN_MILLIS_FAST = 200;
    final int SLOW_INFO_UPDATE_PERIOD_IN_MILLIS = 1000 * 60 * 6; //Signal Strength, memory, etc.
    //final int SLOW_INFO_UPDATE_RATIO = SLOW_INFO_UPDATE_PERIOD_IN_MILLIS / UXUPDATE_PERIOD_IN_MILLIS;

    boolean InternetOnline = false;
    final int ONLINE_CHECK_UPDATE_PERIOD = 1000 * 30;
    int time_from_lat_ux_update_millis;
    boolean AccAquiring = false;
    boolean GyroAcquiring = false;
    boolean TemperatureAcquiring = false;
    // variables used for the 30 sec acquisition for calibration
    boolean CalibrationMode = false;
    //TimestampedDataArray acc_x_calib, acc_y_calib, acc_z_calib;
    //TimestampedDataArray gyro_x_calib, gyro_y_calib, gyro_z_calib;

    boolean InitialCalibrationPerformed = false;

    private int tens_sec_counter; //counter of tens seconds for timeout
    private int ms_to_sleep;
    private int ms_interval;

    // TextView
    TextView acc_samples_tview;
    TextView acc_x_1_tview, acc_y_1_tview, acc_z_1_tview;
    TextView acc_x_2_tview, acc_y_2_tview, acc_z_2_tview;
    TextView acc_x_3_tview, acc_y_3_tview, acc_z_3_tview;
    TextView acc_vel_x_tview;
    TextView acc_distance_x_tview;
    TextView acc_period_mean_tview, acc_period_stdev_tview;
    TextView gyro_samples_tview;
    TextView gyro_x_1_tview, gyro_y_1_tview, gyro_z_1_tview;
    TextView gyro_x_2_tview, gyro_y_2_tview, gyro_z_2_tview;
    TextView gyro_x_3_tview, gyro_y_3_tview, gyro_z_3_tview;
    TextView gyro_angle_total_tview, gyro_angle_tview;
    TextView gyro_period_mean_tview, gyro_period_stdev_tview;
    TextView MaxiIO_status_tview, MaxiIO_event_tview;
    TextView battery_textview;
    TextView sys_stat_textview;
    TextView sys_ref_date_textview;
    TextView WheelchairID_tview;
    TextView build_tview;
    TextView temperature_min_val_tview;
    TextView temperature_max_val_tview;
    TextView temperature_mean_val_tview;
    TextView signal_level_tview;
    TextView memory_used_tview, memory_avail_tview;
    TextView app_uptime_tview, duty_uptime_tview;
    TextView ligth_val_tview, number_of_touch_tview;
    TextView hourly_ref_time_tview;

    TextView power_on_tview, power_off_tview, motor_on_tview, motor_off_tview;

    TextView system_online_tview;

    Button btPowerOn, btPowerOff, btMotorOn, btMotorOff;
    //Button btSendEvent, btSendDailyReport, btSendHourlyReport;
    Button btCalibrate, btToggleView, btToggleMode;
    //Button btDoHourlyUpdate;
    Button btDoAppUpdate; //btDoDailyUpdate;
    boolean UpdateTextViewsEnabled = false;

    //Array containing the information about the running time of the App
    int[] RunningTime;
    // CLASSES FOR COMMUNICATIONS BETWEEN ACTIVITIES
    //User user;              //input to this class
    //LastFiles lastfiles;    //output
    // phone network variables
    TelephonyManager myTelephonyManager;
    NetworkInfo myNetworkInfo;
    AzureManager myAzureManager;
    Configuration myConfig;
    AzureEventManager myEventManager;
    int slow_info_update_counter;
    int current_hour_of_day;
    int previous_hour_of_day;

    //BroadcastReceiver OnceEveryHour_Receiver;
    Calendar MainCalendar;
    PendingIntent OnceAnHour_pintent;
    //AlarmManager myAlarmManager;

    //final int DAILY_REPORT_HOUR = 2;

    //int SignalStrength = 0;
    //BroadcastReceiver ViewRefreshUpdate_Receiver;
    //PendingIntent ViewRefreshUpdate_pintent;
    //AlarmManager ViewRefreshUpdate_alarmMgr;

    Handler RefreshUX_Handler;
    private boolean RefreshUX = false;
    //private final boolean NEW_REFRESH = true;

    Handler RefreshOnlineStatus_Handler;
    private boolean RefreshOnlineStatus = false;

    //it.dongnocchi.mariner.NotSentFileHandler notSent;
    //WindowManager.LayoutParams NewLayoutParams = null;
    WheelchairData myData;
    String logger_filename;
    String logger_filename_complete;

    //==============================================================================================
    // YOCTOPUCE - MAXI-IO
    //==============================================================================================
    public final short MaxiIO_MotorPin = 7;

    String MaxiIO_SerialN;

    YDigitalIO MaxiIO;
    //YModule myYModule;
    private Handler YoctoHandler = new Handler();

    //int Motor_OldInputData;

    int Motor_NewInputData;
    private SensorManager mSensorManager;
    private Sensor mAcc;
    //private int ViewRefreshUpdate_period_ms = 1000;
    private Sensor mGyro;
    private Sensor mTemperature;
    private Sensor myLight;

    private BatteryManager myBatteryManager;
    private boolean ManualMode = false;

    private long CalibrationStartTime;

    //FileLog myFileLog;
    //private long AcquisitionStartTime;
    private String CommonFilePreamble;
    // INDICATE WHEN YOCTO IS IN USE (AVAILABLE)
    private boolean YoctoInUse = false;
    private int Status;

    public static Context applicationContext = null;
    public static Thread.UncaughtExceptionHandler defaultHandler = null;
    public static Thread.UncaughtExceptionHandler exceptionHandler = null;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    //private GoogleApiClient client;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    //private GoogleApiClient client;

    //private boolean init_yocto_just_once = false;
    @Override
    //==========================================================================
    protected void onCreate(Bundle savedInstanceState)
    //==========================================================================
    {
        try {
            //TO send email - as in http://stackoverflow.com/questions/22395417/error-strictmodeandroidblockguardpolicy-onnetwork
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            //Initialize Time info and data structures
            AppStartTime = System.nanoTime();
            App_Start_Date = new Date();

            UpdateDailyReferenceTimeAndDate();

            Hourly_Reference_Time = Daily_Reference_Time;

            RunningTime = new int[5]; //0 = days, 1 = hours, 2 = mins, 3 = sec, 4 = ms

            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            MainCalendar = Calendar.getInstance();

            myConfig = new Configuration();
            myConfig.currentBuild = CURRENT_BUILD;

            //Initialize UX
            InitUX();

            RefreshUX_Handler = new Handler();

            RefreshOnlineStatus_Handler = new Handler();

            build_tview.setText("Build: " + Integer.toString(CURRENT_BUILD));
            WheelchairID_tview.setText("Wheelchair ID: " + myConfig.get_WheelchairID());
            CreateAndOpenNewFileLogger();
            //myLogger = new FileLog();
            LastTemperatureReadingTime = System.currentTimeMillis();

            // INITIALISE SENSOR MANAGER and Sensors
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

            mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            myLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

            //myBatteryManager = new BatteryManager();

            // REGISTER BROADCAST RECEIVER FOR BATTERY EVENTS
            registerReceiver(mBatChargeOff, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
            registerReceiver(mBatChargeOn, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
            registerReceiver(mBatLow, new IntentFilter(Intent.ACTION_BATTERY_LOW));
            //registerReceiver(mBatOkay, new IntentFilter(Intent.ACTION_BATTERY_OKAY));
            registerReceiver(mBatChanged, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            myData = new WheelchairData(Daily_Reference_Time);

            //set the log filename to the DailyLogFileName varabile
            myData.DailyLogFileName = logger_filename;

            /*
            acc_x_calib = new TimestampedDataArray(CALIB_DATA_SIZE);
            acc_y_calib = new TimestampedDataArray(CALIB_DATA_SIZE);
            acc_z_calib = new TimestampedDataArray(CALIB_DATA_SIZE);

            gyro_x_calib = new TimestampedDataArray(CALIB_DATA_SIZE);
            gyro_y_calib = new TimestampedDataArray(CALIB_DATA_SIZE);
            gyro_z_calib = new TimestampedDataArray(CALIB_DATA_SIZE);
            */


            /*
            myAzureManager = new AzureManager(getApplicationContext(), new it.dongnocchi.mariner.AsyncResponse() {
                @Override
                public void processFinish(String last_uploaded_file) {

                    // se ho caricato il file xml coi nomi dei file caricati, non scriverlo sul nuovo file xml
                    String xml_name = myConfig.get_UploadedFiles_XmlName();
                    if (last_uploaded_file != xml_name) { // l'ultimo file caricato è un file di dati
                        myAzureManager.AppendNew_UploadedFileName(last_uploaded_file);//lo aggiungo al file contenente i nomi dei file caricati
                    }
                }
            }, myConfig);
            */

            //DebugTestWriteBin();

            //TODO: semplificare il codice del AzureManager
            myAzureManager = new AzureManager(getApplicationContext(), new AsyncResponse() {
                @Override
                public void processFinish(String last_uploaded_file) {
                }
            }, myConfig);

            //Verifica se è presente l'alimentazione, ed in questo caso aggiunge
            //un evento fittizio di PowerON

            start_network_listener();

            UpdateStorageMemoryAvailable();

            // inizializzazione eventhub manager
            myEventManager = new AzureEventManager(getApplicationContext(), new AsyncResponse() {
                @Override
                public void processFinish(String output) {
                }
            }, myConfig, myData);


            if (isSupplyPowerPresent()) {
                PowerIsON(null);
                //myData.AddPowerONEvent(AppStartTime);
            }

            SetGUIButtonEnabled(false);

            //TODO: da verificare perchè parzialmente sovrapponibile al precendnte UpdateDailyReferenceTimeAndDate();
            DailyResetData();

            UpdateDailyReferenceTimeAndDate();

            //StartCalibration();

            StartTemperatureAcquisition();

            StartLightAcquisition();

            StartHourlyTimer();

            StartPeriodicRefreshUX(); //StartPeriodicUpdateUX();

            InternetOnline = myEventManager.isInternetOnline();

            //Let's check if there are files and json events to upload
            if(InternetOnline)
            {
                system_online_tview.setText("ONLINE");

                myAzureManager.UpdateFilesToSendList();
                if(myAzureManager.FilesToSend.size()>0 )
                {
                    myAzureManager.UploadFilesToBlobs();
                    WaitForEmptyBlobListOrTimeout(300);
                }

                myEventManager.SendJsonHourlyEventListFromFile();
                myEventManager.SendJsonDailyReportListFromFile();
                //myAzureManager.sen
            }
            else
                system_online_tview.setText("OFFLINE");

            StartPeriodicOnlineCheck(ONLINE_CHECK_UPDATE_PERIOD);

            //notSent = new it.dongnocchi.mariner.NotSentFileHandler(myConfig.get_Wheelchair_path());

                /*
                //TODO: da togliere. è servito per effettuare il test su TimestampedDataArray
                for(int i=0; i < 2000; i++ ) {
                    long t = SystemClock.elapsedRealtime();
                    acc_x_calib.Add((float) i, t);
                    Thread.sleep(5);
                }
                acc_x_calib.UpdateStats();

                float a = acc_x_calib.mean;
                float b = acc_x_calib.stdev;
                float c = acc_x_calib.mean_deltatime;
                float d = acc_x_calib.stdev_deltatime;
                float e = 0.0f;
                */
            //int val = myData.StorageMemoryAvailable;

            //CreateMyWheelchairFile();
            //call_toast(ByteOrder.nativeOrder().toString()); system is little endian
            //FileLog.d(TAG, "onCreate completed");
            Start_Yocto();

            ///*****************************************************************
            //  Inserito per provare a loggare gli errori non gestiti della App
            ///*****************************************************************
            if (defaultHandler == null) {
                defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
            }

            if (applicationContext == null) {
                applicationContext = getApplicationContext();
            }

            if (exceptionHandler == null) {
                exceptionHandler = new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                        Log.e("Uncaught Exception", paramThrowable.getMessage());
                        logUnhandledExceptionError(paramThrowable);
                        defaultHandler.uncaughtException(paramThread, paramThrowable);
                    }
                };

                Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
            }
            ///**********************************************
            ///**********************************************

            myEventManager.SendEventNew("APP_ON_CREATE", myData.myBatteryData.level, "");

            if(InternetOnline)
                SendEMail("App OnCreate (build " + Integer.toString(CURRENT_BUILD) + ")", "", "The App has been created");

        } catch (Exception ex) {
            LogException(TAG, "onCreate exception: ", ex);
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        //client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

        /*
        private void test() {
            int a = 6;
        }
        */

    @Override
    protected void onStart() {
        super.onStart();

        FileLog.d(TAG, "App START", null);

        if(!InitialCalibrationPerformed)
        {
            StartCalibration();
            InitialCalibrationPerformed = true;
        }

        //Start_Yocto();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //myEventManager.SendEventNew("APP_ON_RESTART", myData.myBatteryData.level, "");
        FileLog.d(TAG, "App RESTART", null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //myEventManager.SendEventNew("APP_ON_PAUSE", myData.myBatteryData.level, "");
        if(debug_mode)
            FileLog.d(TAG, "App Pause", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(debug_mode)
            FileLog.d(TAG, "App RESUME", null);
        //myEventManager.SendEventNew("APP_ON_RESUME", myData.myBatteryData.level, "");
    }

    @Override
    protected void onStop() {
        //myEventManager.SendEventNew("APP_ON_STOP", myData.myBatteryData.level, "");
        FileLog.d(TAG, "App STOP", null);

        super.onStop();
        //Stop_Yocto();
    }

    @Override
    protected void onDestroy()
    {
        FileLog.d(TAG, "App OnDestroy", null);

        if(InternetOnline)
            SendEMail("App OnDestroy", "", "The App is about to be destroyed");

        Stop_Yocto();

        super.onDestroy();
    }


    private void logUnhandledExceptionError(final Throwable paramThrowable) {
        try {

            //static Context context = this.getBaseContext();// ;
            CharSequence text = "Hello toast!";
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(applicationContext, text, duration);
            toast.show();

            //ApplicationError error = new ApplicationError();

            String stackTrace = "";
            for (int i = 0; i < paramThrowable.getStackTrace().length; i++) {
                stackTrace += paramThrowable.getStackTrace()[i].toString() + "\n";
            }

            Log.e("Saving error...", "");

            Throwable tmp = paramThrowable;
            int j = 0;
            while ((tmp = tmp.getCause()) != null && j < 5) {
                j++;
                stackTrace += "Coused by:\n";
                for (int i = 0; i < tmp.getStackTrace().length; i++) {
                    stackTrace += tmp.getStackTrace()[i].toString() + "\n";
                }
            }

            String deviceInfo = "";
            deviceInfo += "OS version: " + System.getProperty("os.version") + "\n";
            deviceInfo += "API level: " + Build.VERSION.SDK_INT + "\n";
            deviceInfo += "Manufacturer: " + Build.MANUFACTURER + "\n";
            deviceInfo += "Device: " + Build.DEVICE + "\n";
            deviceInfo += "Model: " + Build.MODEL + "\n";
            deviceInfo += "Product: " + Build.PRODUCT + "\n";

                /*
                error.mDeviceInfo = deviceInfo;
                error.mErrorMessage = paramThrowable.getMessage();
                error.mStackTrace = stackTrace;

                error.save();
                */

            Log.e("Saved error:", paramThrowable.getMessage() + "\n" + stackTrace);

            DialogFragment dialog = new YesNoDialog();
            Bundle args = new Bundle();
            args.putString("title", paramThrowable.getMessage());
            args.putString("message", stackTrace);
            dialog.setArguments(args);
            //dialog.setTargetFragment(this, 0);
            dialog.show(getFragmentManager(), "tag");
        } catch (Exception e) {
            LogException(TAG, "logUnhandledExceptionError : ", e);
        }
    }

    //==========================================================================
    private BroadcastReceiver mBatChanged = new BroadcastReceiver()
            //==========================================================================
    {
        @Override
        public void onReceive(Context cont, Intent battery_intent) {

            // GET BATTERY LEVEL
            long eventtime = System.nanoTime();
            myData.AddBatteryValChangeEvent(eventtime, battery_intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
            //battery_textview.setText(myData.BatteryLevel + "%");
            // save data
            //Battery_AppendData(BatteryLevel, false);
        }
    };

    //==========================================================================
    private BroadcastReceiver mBatLow = new BroadcastReceiver()
            //==========================================================================
    {
        @Override
        public void onReceive(Context cont, Intent battery_intent) {

            // GET BATTERY LEVEL
            long eventtime = System.nanoTime();

            if( InternetOnline) {
                myEventManager.SendEventNew("ALERT: BATTERY LOW", myData.myBatteryData.level, "");
                if( !myData.HourlyAlerts.BatteryLow) {
                    myData.HourlyAlerts.BatteryLow = true;
                    SendEMail("BATTERY LOW", "", "Current Battery Level = " + Integer.toString(myData.myBatteryData.level) + "%");
                }
            }

            myData.AddBatteryValChangeEvent(eventtime, battery_intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
            //battery_textview.setText(myData.BatteryLevel + "%");
            // save data
            //Battery_AppendData(BatteryLevel, false);
        }
    };

    //==========================================================================
    private class DailyUpdaterTask extends AsyncTask<Void, Boolean, String>
    //==========================================================================
    {
        public DailyUpdaterTask() {
    };


        @Override
        protected String doInBackground(Void... params) {
            try {
                DoDailyUpdate();

            } catch (Exception ex) {
                LogException(TAG, "DailyUpdaterTask exception: ", ex);
            }
            return null;
        }

        ;
    }


    //==========================================================================
    private class HourlyUpdaterTask extends AsyncTask<Void, Boolean, String>
    //==========================================================================
    {
        public HourlyUpdaterTask() {
        };

        @Override
        protected String doInBackground(Void... params) {
            try {
                long actual_time = System.nanoTime();
                current_hour_of_day = GetActualHourOfDay();//

                LogDebug(TAG, "HourlyUpdaterTask - doInBackground()");

                if (current_hour_of_day != previous_hour_of_day) {
                    LogDebug(TAG, "OnceEveryHour_Receiver - (h" + current_hour_of_day + "/" + previous_hour_of_day + ")");

                    myData.updateHourlyUse(Hourly_Reference_Time, actual_time);

                    if (current_hour_of_day == myConfig.get_DailyUpdateHour()) { // do this only once a day at 2 in the night
                        DoDailyUpdate();

                    } else {
                        //myData.updateHourlyUse(Hourly_Reference_Time, actual_time);

                        //If we're online and there is a que of events not sent, let's send them
                        if(InternetOnline)
                            myEventManager.SendJsonHourlyEventListFromFile();

                        myEventManager.SendHourlyStatusEvent(InternetOnline);

                        ResetHourlyCounters();
                    }

                    Hourly_Reference_Time = actual_time;

                    //LogDebug(TAG, "OnceEveryHour_Receiver - End");

                    previous_hour_of_day = current_hour_of_day;
                }
            } catch (Exception ex) {
                LogException(TAG, "HourlyUpdaterTask doInBackground() exception: ", ex);
            }

            return null;
        };
    }

    //==========================================================================
    private void DoDailyUpdate() throws InterruptedException {
    //==========================================================================

        LogDebug(TAG, "Daily Report Start");
        //FileLog.d("MARINER", "Start Daily Report", null);
        //Step 1 - let's stop periodic operations and acquisitions (if any)
        StopPeriodicRefreshUX();//StopPeriodicUpdateUX();

        StopPeriodicOnlineCheck();

        //myData.updateDailyUse();
        UpdateStorageMemoryAvailable();

        StopAllAcquisitions();
        LogDebug(TAG, "Stop All Acquisitions");

        Sleep(2000, 100);

        //Step 2 -
        SaveData();
        LogDebug(TAG, "Data Saved");


        LogDebug(TAG, "Daily Report End");

        CloseAndSaveLogger();

        UpdateDailyReferenceTimeAndDate();

        CreateAndOpenNewFileLogger();
        LogDebug(TAG, "New File Logger Created");

        //Let's check if we're online
        InternetOnline = myEventManager.isInternetOnline();

        myEventManager.SendDailyReport(InternetOnline);

        DailyResetData();

        //In case there are offline files
        myAzureManager.UpdateFilesToSendList();

        //CalibrateAccelerometer();
        //UpdateListofFilesToUpload();

        if(InternetOnline) {
            SendEMail("Daily report", myConfig.get_Acquisition_Folder()+ myData.DailyLogFileName,
                    "Daily Report Sent" + System.getProperty("line.separator") +
                            myEventManager.LastDailyReport);

            LogDebug(TAG, "Internet Online - Sending info");
            myAzureManager.UploadFilesToBlobs();
            WaitForEmptyBlobListOrTimeout(300);
            LogDebug(TAG, "Upload Blobs done");

            //If there are saved events, let's send them to the Event HUb
            myEventManager.SendJsonHourlyEventListFromFile();
            myEventManager.SendJsonDailyReportListFromFile();

            myAzureManager.ConfigDownloaded = false;
            myAzureManager.CheckAndUpdateConfig();


            WaitForConfigDownloadedOrTimeout(30);

            myAzureManager.CheckAndUpdateAPK();

            Sleep(2000, 100);

            LogDebug(TAG, "Checked (and updated)");

        }
        else {
            LogDebug(TAG, "Internet Offline - saving info locally");
            myAzureManager.AppendFilesToUploadList();
            LogDebug(TAG, "Blobs to Upload inserted in uploading list");
        }
        //tens_sec_counter = 18;
        //CheckForEmptyBlobListOrTimeout();

        //Check for App updates
        //myAzureManager.CheckNewUpdates(myData.myBatteryData.level);

        //myData.DailyReset(Daily_Reference_Time);//DailyResetData();

        ResetHourlyCounters();

        StartCalibration();

        StartTemperatureAcquisition();

        StartLightAcquisition();

        StartPeriodicRefreshUX();//StartPeriodicUpdateUX();

        StartPeriodicOnlineCheck(ONLINE_CHECK_UPDATE_PERIOD);

        LogDebug(TAG, "Daily update - all functions (re)started");
        //FileLog.d("MARINER", "End Daily Report", null);
    }

    //==========================================================================
    private BroadcastReceiver OnceEveryHour_Receiver = new BroadcastReceiver()
            //==========================================================================
    {
        @Override
        public void onReceive(Context context, Intent intent) {

  //          if (true) {
                try {
                    HourlyUpdaterTask hut = new HourlyUpdaterTask();
                    hut.execute();

                } catch (Exception ex) {
                    LogException(TAG, "OnceEveryHour_Receiver exception: ", ex);
                }

/*
            } else {

                try {
                    long actual_time = System.nanoTime();
                    current_hour_of_day = GetActualHourOfDay();//

                    LogDebug(TAG, "OnceEveryHour_Receiver - (h" + current_hour_of_day + "/" + previous_hour_of_day + ")");

                    if (current_hour_of_day != previous_hour_of_day) {

                        myData.updateHourlyUse(Hourly_Reference_Time, actual_time);

                        if (current_hour_of_day == myConfig.get_DailyUpdateHour()) { // do this only once a day at 2 in the night
                            LogDebug(TAG, "Daily Report Start");
                            //FileLog.d("MARINER", "Start Daily Report", null);
                            //Step 1 - let's stop periodic operations and acquisitions (if any)
                            StopPeriodicRefreshUX();//StopPeriodicUpdateUX();

                            StopPeriodicOnlineCheck();

                            //myData.updateDailyUse();
                            UpdateStorageMemoryAvailable();

                            StopAllAcquisitions();

                            //Sleep(2000, 100);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                    //Step 2 -
                                    SaveData();

                                    myEventManager.SendDailyReport(InternetOnline);

                                    LogDebug(TAG, "Daily Report End");

                                    CloseAndSaveLogger();

                                    DailyResetData();

                                    CreateAndOpenNewFileLogger();

                                    //CalibrateAccelerometer();
                                    //UpdateListofFilesToUpload();

                                    //Upload Blobs
                                    //myAzureManager.UploadFilesToBlobs(myData.myBatteryData.level);
                                    myAzureManager.UploadFilesToBlobs();

                                    //tens_sec_counter = 18;
                                    //CheckForEmptyBlobListOrTimeout();

                                    //WaitForEmptyBlobListOrTimeout(180);
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {

                                            LogDebug(TAG, "Upload Blobs done");

                                            //Check for App updates
                                            //myAzureManager.CheckNewUpdates(myData.myBatteryData.level);
                                            myAzureManager.CheckAndUpdateAPK();


                                            //Sleep(2000, 100);
                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {

                                                    myAzureManager.CheckAndUpdateConfig();

                                                    LogDebug(TAG, "Checked (and updated)");

                                                    //myData.DailyReset(Daily_Reference_Time);//DailyResetData();

                                                    ResetHourlyCounters();

                                                    StartCalibration();

                                                    StartTemperatureAcquisition();

                                                    StartLightAcquisition();

                                                    StartPeriodicRefreshUX();//StartPeriodicUpdateUX();

                                                    StartPeriodicOnlineCheck(ONLINE_CHECK_UPDATE_PERIOD);

                                                    LogDebug(TAG, "Daily update - all functions started");
                                                    //FileLog.d("MARINER", "End Daily Report", null);

                                                }
                                            }, 10000);
                                        }
                                    }, 120000);
                                }
                            }, 2000);
                        } else {
                            //myData.updateHourlyUse(Hourly_Reference_Time, actual_time);
                            myEventManager.SendHourlyStatusEvent(InternetOnline);
                            ResetHourlyCounters();
                        }

                        Hourly_Reference_Time = actual_time;

                        //LogDebug(TAG, "OnceEveryHour_Receiver - End");

                        previous_hour_of_day = current_hour_of_day;
                    }
                } catch (Exception ex) {
                    LogException(TAG, "OnceEveryHour_Receiver exception: ", ex);
                }
            }
            */
        }
    };

    /*
        private void esempio_di_attesa() {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //cose da fare dopo il timeout
                }
            }, 10000);


        }

        private void CheckForEmptyBlobListOrTimeout() {
            if (myAzureManager.FilesToSend.size() > 0) {
                if (tens_sec_counter > 0) {
                    tens_sec_counter--;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            CheckForEmptyBlobListOrTimeout();
                        }
                    }, 10000);
                }
            }
        }
    */

    private boolean WaitForConfigDownloadedOrTimeout(int num_seconds_timeout) {
        int sleep_time = num_seconds_timeout * 1000;
        int sec_counter = 0;
        do {
            Sleep(1000, 100);
            if (num_seconds_timeout > 0)
                if (++sec_counter > num_seconds_timeout)
                    break;
        }
        while (!myAzureManager.ConfigDownloaded);

        return (myAzureManager.ConfigDownloaded);
    }


    private int WaitForEmptyBlobListOrTimeout(int num_seconds_timeout) {
        int sleep_time = num_seconds_timeout * 1000;
        int sec_counter = 0;
        do {
            Sleep(1000, 100);
            if (num_seconds_timeout > 0)
                if (++sec_counter > num_seconds_timeout)
                    break;
        }
        while (myAzureManager.FilesToSend.size() > 0);

        return (myAzureManager.FilesToSend.size());
    }

    private void UpdateStorageMemoryAvailable() {

        long temp;
        File external = Environment.getExternalStorageDirectory();
        temp = external.getFreeSpace();
        myData.StorageMemoryAvailable = (int) (temp / (1024 * 1024));
        //myData.StorageMemoryAvailable = (int) temp;
        //myData.StorageTotalMemory = (int) external.getTotalSpace();

    }

    //==========================================================================
    private BroadcastReceiver ViewRefreshUpdate_Receiver = new BroadcastReceiver()
            //==========================================================================
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Aggiorno i dati presenti sullo schermo

            RefreshGUI();
        }
    };


    //==========================================================================
    private BroadcastReceiver mBatChargeOn = new BroadcastReceiver()
            //==========================================================================
    {
        @Override
        //When Event is published, onReceive method is called
        public void onReceive(Context c, Intent i) {
            PowerIsON(null);
        }
    };

    //==========================================================================
    private BroadcastReceiver mBatChargeOff = new BroadcastReceiver()
            //==========================================================================
    {
        @Override
        //When Event is published, onReceive method is called
        public void onReceive(Context c, Intent i) {
            PowerIsOFF(null);
        }
    };

    //==========================================================================
    private void DailyResetData()
    //==========================================================================
    {        //TODO: implementare il reset di tutte le strutture dati utilizzate
        myData.DailyReset(Daily_Reference_Time);
    }

    //==============================================================================================
    //  SENSOR OPERATIONS
    //==============================================================================================

    @Override
    //==========================================================================
    public void onSensorChanged(SensorEvent event)
    //==========================================================================
    {
        try {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                if( AccAquiring ) {
                    if (CalibrationMode) {
                        //acc_x_calib.Add(event.values[0], event.timestamp);
                        //acc_y_calib.Add(event.values[1], event.timestamp);
                        //acc_z_calib.Add(event.values[2], event.timestamp);
                        myData.myInertialData.UpdateAccDataCalibration(event);

                        if (CheckIfCalibrationCompleted())
                            StopCalibrateInertialSensors();
                    }                //Acc_AppendData(event, false);
                    else {
                        myData.myInertialData.UpdateAccData(event);
                    }
                }
            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                if(GyroAcquiring) {
                    //Gyro_AppendData(event, false);
                    if (CalibrationMode) {
                        //gyro_x_calib.Add(event.values[0], event.timestamp);
                        //gyro_y_calib.Add(event.values[1], event.timestamp);
                        //gyro_z_calib.Add(event.values[2], event.timestamp);
                        myData.myInertialData.UpdateGyroDataCalibration(event);

                        if (CheckIfCalibrationCompleted())
                            StopCalibrateInertialSensors();

                    }                //Acc_AppendData(event, false);
                    else {
                        myData.myInertialData.UpdateGyroData(event);
                    }
                }
            } else if ((event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE)) {
                CurrentTemperatureReadingTime =  System.currentTimeMillis();
                if( (CurrentTemperatureReadingTime - LastTemperatureReadingTime) > delta_temperature_reading )
                {
                    myData.myTempData.AppendData(event);

                    if (myData.myTempData.CurrentTemperature >= myConfig.TemperatureThresholdAlert) {
                        if (InternetOnline && !myData.HourlyAlerts.TemperatureHigh) {
                            myData.HourlyAlerts.TemperatureHigh = true;
                            new Thread(new Runnable() {
                                public void run() {
                                    myEventManager.SendEventNew("ALERT: HIGH TEMPERATURE", myData.myTempData.CurrentTemperature, "");
                                    SendEMail("ALERT: TEMPERATURE HIGH", "",
                                            "Temperature is too high: " + Float.toString(myData.myTempData.CurrentTemperature) + "°C");

                                    // do something here
                                }
                            }).start();
                        }
                    }
                    myData.UpdateMemoryUsage();

                    //Aggiorno la visualizzazione dei dati
                    //RefreshGUI();
                }

                //TODO: da sistemare una routine con un intervallo di un secondo...
                UpdateRunningTime();

            } else if ((event.sensor.getType() == Sensor.TYPE_LIGHT)) {
                myData.UpdateLightValue(event.values[0]);
            }

        } catch (Exception ex) {
            LogException(TAG, "onSensorChanged exception: ", ex);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    //==============================================================================================
    //==============================================================================================
    //  MOTOR AND POWER EVENTS HANDLING
    //==============================================================================================
    //==============================================================================================

    private void InitUX() {
        acc_samples_tview = (TextView) findViewById(R.id.acc_samples);

        acc_x_1_tview = (TextView) findViewById(R.id.acc_x_1_tview);
        acc_y_1_tview = (TextView) findViewById(R.id.acc_y_1_tview);
        acc_z_1_tview = (TextView) findViewById(R.id.acc_z_1_tview);

        acc_x_2_tview = (TextView) findViewById(R.id.acc_x_2_tview);
        acc_y_2_tview = (TextView) findViewById(R.id.acc_y_2_tview);
        acc_z_2_tview = (TextView) findViewById(R.id.acc_z_2_tview);

        acc_x_3_tview = (TextView) findViewById(R.id.acc_x_3_tview);
        acc_y_3_tview = (TextView) findViewById(R.id.acc_y_3_tview);
        acc_z_3_tview = (TextView) findViewById(R.id.acc_z_3_tview);

        acc_period_mean_tview = (TextView) findViewById(R.id.acc_period_mean_tview);
        acc_period_stdev_tview = (TextView) findViewById(R.id.acc_period_stdev_tview);

        acc_vel_x_tview = (TextView) findViewById(R.id.acc_vel_x_val_tview);
        acc_distance_x_tview = (TextView) findViewById(R.id.acc_distance_x_val_tview);

        gyro_samples_tview = (TextView) findViewById(R.id.gyro_samples);

        gyro_x_1_tview = (TextView) findViewById(R.id.gyro_x_1_tview);
        gyro_y_1_tview = (TextView) findViewById(R.id.gyro_y_1_tview);
        gyro_z_1_tview = (TextView) findViewById(R.id.gyro_z_1_tview);

        gyro_x_2_tview = (TextView) findViewById(R.id.gyro_x_2_tview);
        gyro_y_2_tview = (TextView) findViewById(R.id.gyro_y_2_tview);
        gyro_z_2_tview = (TextView) findViewById(R.id.gyro_z_2_tview);

        gyro_x_3_tview = (TextView) findViewById(R.id.gyro_x_3_tview);
        gyro_y_3_tview = (TextView) findViewById(R.id.gyro_y_3_tview);
        gyro_z_3_tview = (TextView) findViewById(R.id.gyro_z_3_tview);

        gyro_angle_total_tview = (TextView) findViewById(R.id.gyro_ang_x_travelled_val_tview);
        gyro_angle_tview = (TextView) findViewById(R.id.gyro_angle_x_val_tview);

        gyro_period_mean_tview = (TextView) findViewById(R.id.gyro_period_mean_tview);
        gyro_period_stdev_tview = (TextView) findViewById(R.id.gyro_period_stdev_tview);

        battery_textview = (TextView) findViewById(R.id.battery_val_tview);
        MaxiIO_status_tview = (TextView) findViewById(R.id.MaxiIO_status_view);
        MaxiIO_event_tview = (TextView) findViewById(R.id.MaxiIO_event_view);
        sys_stat_textview = (TextView) findViewById(R.id.system_status_view);

        sys_ref_date_textview = (TextView) findViewById(R.id.system_ref_date_view);

        build_tview = (TextView) findViewById(R.id.system_build_view);
        WheelchairID_tview = (TextView) findViewById(R.id.system_id_view);

        app_uptime_tview = (TextView) findViewById(R.id.app_uptime_tview);
        duty_uptime_tview = (TextView) findViewById(R.id.current_duty_uptime_tview);
        hourly_ref_time_tview = (TextView) findViewById(R.id.hourly_ref_time_view);


        ligth_val_tview = (TextView) findViewById(R.id.light_val_tview);
        number_of_touch_tview = (TextView) findViewById(R.id.num_of_touch_val_tview);

        signal_level_tview = (TextView) findViewById(R.id.signal_level_val_tview);
        //temp_val_tview = (TextView) findViewById(R.id.temperature_val_tview);

        memory_used_tview = (TextView) findViewById(R.id.memory_val_tview);
        memory_avail_tview = (TextView) findViewById(R.id.memory_avail_val_tview);

        //temperature_tview = (TextView) findViewById(R.id.temperature_tview);
        temperature_mean_val_tview = (TextView) findViewById(R.id.temperature_mean_tview);
        temperature_min_val_tview = (TextView) findViewById(R.id.temperature_min_tview);
        temperature_max_val_tview = (TextView) findViewById(R.id.temperature_max_tview);

        power_on_tview = (TextView) findViewById(R.id.system_power_on_tview);
        power_off_tview = (TextView) findViewById(R.id.system_power_off_tview);
        motor_on_tview = (TextView) findViewById(R.id.system_motor_on_tview);
        motor_off_tview = (TextView) findViewById(R.id.system_motor_off_tview);

        system_online_tview = (TextView) findViewById(R.id.system_online_tview);

        btPowerOn = (Button) findViewById(R.id.PowerONBtn);
        btPowerOff = (Button) findViewById(R.id.PowerOFFBtn);
        btMotorOn = (Button) findViewById(R.id.MotorONBtn);
        btMotorOff = (Button) findViewById(R.id.MotorOFFBtn);
/*
        btSendEvent = (Button) findViewById(R.id.send_event_button);
        btSendHourlyReport = (Button) findViewById(R.id.send_hourly_data_button);
        btSendDailyReport = (Button) findViewById(R.id.send_daily_data_button);
      btDoHourlyUpdate = (Button) findViewById(R.id.hourly_update_button);
*/
        btCalibrate = (Button) findViewById(R.id.calibrate_button);
        btToggleView = (Button) findViewById(R.id.toggle_display_button);
        btToggleMode = (Button) findViewById(R.id.toggle_manualmode_button);

        btDoAppUpdate = (Button) findViewById(R.id.update_app_button);

        btToggleMode.setText("Auto Mode");
        btToggleMode.setBackgroundColor(Color.GREEN); //.getBackground().setColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
        btDoAppUpdate.setBackgroundColor(Color.CYAN);
        btToggleView.setBackgroundColor(Color.GREEN);
    }

    //==========================================================================
    private void SetGUIButtonEnabled(boolean enabled)
    //==========================================================================
    {
        btCalibrate.setEnabled(enabled);
        btToggleView.setEnabled(enabled);
        btPowerOn.setEnabled(enabled);
        btPowerOff.setEnabled(enabled);
        btMotorOn.setEnabled(enabled);
        btMotorOff.setEnabled(enabled);
        //btSendEvent.setEnabled(enabled);
        //btSendHourlyReport.setEnabled(enabled);
        //btSendDailyReport.setEnabled(enabled);

        //btDoHourlyUpdate.setEnabled(enabled);
        //btDoDailyUpdate.setEnabled(enabled);
        btDoAppUpdate.setEnabled(enabled);
    }

    //==============================================================================================
    //==============================================================================================
    //  CHARGE CONTROL
    //==============================================================================================
    //==============================================================================================

    public boolean isSupplyPowerPresent() {
        //Context context = getApplicationContext();
        Intent intent = this.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

        return (plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB);
    }

    /*
    //==========================================================================
    public void LowerScreenBrightness() {
        //==========================================================================
        NewLayoutParams = new WindowManager.LayoutParams();
        NewLayoutParams = getWindow().getAttributes();

        NewLayoutParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        NewLayoutParams.screenBrightness = 0;
        getWindow().setAttributes(NewLayoutParams);
    }

    //==========================================================================
    public void ResetScreenBrightness() {
        //==========================================================================
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
*/
    //==============================================================================================
    //==============================================================================================
    //  NETWORK OPERATIONS
    //==============================================================================================
    //==============================================================================================
    protected void start_network_listener() {
        try {
            /*
            myNetworkInfo = new NetworkInfo();
            myTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            myTelephonyManager.listen(myNetworkInfo, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            */

            //psListener = new myPhoneStateListener();
            myNetworkInfo = new NetworkInfo();
            myTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            myTelephonyManager.listen(myNetworkInfo,PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);


        } catch (Exception ex) {
            LogException(TAG, "start_network_listener() exception: ", ex);
        }
    }


    //==========================================================================
    public void PowerIsON(View view)
    //==========================================================================
    {
        try {
            if (!myData.PowerON) {

                //Send Event to Azure Back End
                myEventManager.SendEventNew("POWER_ON", 0, "");

                //Calculate the actual time
                long new_power_on_time = System.nanoTime();

                //Issue a new Power On Event
                myData.AddPowerONEvent(new_power_on_time);

                sys_stat_textview.setText("POWER ON");

                //ConnectYoctoMaxiIO(); // sets YoctoInUse if its connected
                //if(!init_yocto_just_once)
                /***************************************************
                 *
                 * Prova di eliminazione 2016-1206


                 InitYoctoMaxiIO(new_power_on_time);
                 */
            }
        } catch (Exception ex) {
            LogException(TAG, "PowerIsON exception: ", ex);
        }
    }


    //==========================================================================
    public void PowerIsOFF(View view)
    //==========================================================================
    {
        try {
            if (myData.PowerON) {
                //ResetScreenBrightness();
                //long T_off = SystemClock.elapsedRealtime() - App_Start_Time;
                //myEventManager.SendEvent("WheelchairData OFF", (float) T_off);

                myEventManager.SendEventNew("POWER_OFF", 0, "");

                long new_power_off_time = System.nanoTime();

                myData.AddPowerOFFEvent(new_power_off_time);
                //PowerOFFHourlyCounter++;

                sys_stat_textview.setText("POWER OFF");

                StopInertialAcquisition();
            }
        } catch (Exception ex) {
            LogException(TAG, "PowerIsOFF exception: ", ex);
        }
    }

    //==========================================================================
    public void Power_OFF_Click(View view)
    //==========================================================================
    {
        try {
            myEventManager.SendEventNew("POWER_OFF", 0, "");
            long new_power_off_time = System.nanoTime();
            myData.AddPowerOFFEvent(new_power_off_time);
            sys_stat_textview.setText("POWER OFF");
            Status = STATUS_SLEEP;
        } catch (Exception ex) {
            LogException(TAG, "Power_OFF_Click exception: ", ex);

        }
    }

    //==========================================================================
    public void Power_ON_Click(View view)
    //==========================================================================
    {
        try {
            myEventManager.SendEventNew("POWER_ON", 0, "");
            long new_power_on_time = System.nanoTime();
            myData.AddPowerONEvent(new_power_on_time);
            sys_stat_textview.setText("POWER ON");
            Status = STATUS_IDLE;
        } catch (Exception ex) {
            LogException(TAG, "Power_ON_Click exception: ", ex);
        }
    }

    //==========================================================================
    public void Motor_ON_Click(View view) {
        //==========================================================================
        btMotorOn.setBackgroundColor(Color.GREEN);
        btMotorOn.setEnabled(false);
        btMotorOff.setEnabled(true);

        StartInertialAcquisition();
    }

    //==========================================================================
    public void Motor_OFF_Click(View view) {
        //==========================================================================
        btMotorOn.setBackgroundColor(Color.GRAY);
        btMotorOff.setEnabled(false);
        btMotorOn.setEnabled(true);

        StopInertialAcquisition();

        myData.myInertialData.ResetIntegral();
    }


    private void StartPeriodicRefreshUX(final int delay) {
        RefreshUX = true;
        RefreshUX_Handler.postDelayed(new Runnable() {
            public void run() {
                RefreshGUI();
                if (RefreshUX)
                    RefreshUX_Handler.postDelayed(this, delay);
            }
        }, delay);
    }

    private void StartPeriodicRefreshUX() {
        StartPeriodicRefreshUX(SLOW_INFO_UPDATE_PERIOD_IN_MILLIS);
    }

    private void StopPeriodicRefreshUX() {
        RefreshUX = false;
    }


    private void StartPeriodicOnlineCheck(final int delay)
    {
        RefreshOnlineStatus = true;
        RefreshOnlineStatus_Handler.postDelayed(new Runnable() {
            public void run() {

                InternetOnline = myEventManager.isInternetOnline();

                if(InternetOnline)
                    system_online_tview.setText("ONLINE");
                else
                    system_online_tview.setText("OFFLINE");

                if (RefreshOnlineStatus)
                    RefreshOnlineStatus_Handler.postDelayed(this, delay);
            }
        }, delay);
    }

    private void StopPeriodicOnlineCheck() {
        RefreshOnlineStatus = false;
    }


    //==========================================================================
    private void StartHourlyTimer() {
        //==========================================================================
        // sets the receiver for once an hour alarm
        registerReceiver(OnceEveryHour_Receiver, new IntentFilter("every_new_hour"));
        OnceAnHour_pintent = PendingIntent.getBroadcast(this, 0, new Intent("every_new_hour"), 0);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        //myAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        //OnceAnHour_alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, MainCalendar.getTimeInMillis(), (600*1000) , OnceAnHour_pintent); // AlarmManager.INTERVAL_HOUR
        am.cancel(OnceAnHour_pintent);
        Hourly_Alarm_Reference_Time = GetNextStartHourAndHalfInMillis();
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, Hourly_Alarm_Reference_Time, AlarmManager.INTERVAL_HOUR, OnceAnHour_pintent); // AlarmManager.INTERVAL_HOUR
    }

    //==========================================================================
    private void StopHourlyTimer() {
        //==========================================================================
        this.unregisterReceiver(OnceEveryHour_Receiver);
        AlarmManager am = (AlarmManager) (this.getSystemService(Context.ALARM_SERVICE));
        am.cancel(OnceAnHour_pintent);
        //        CancelAlarm(myAlarmManager, OnceAnHour_pintent);
    }

    //==========================================================================
    private void CancelAlarm(AlarmManager Alarm_StopToFire, PendingIntent PendingIntentToStop)
    //==========================================================================
    {
        if (Alarm_StopToFire != null) {
            Alarm_StopToFire.cancel(PendingIntentToStop);
        }
    }


    //==========================================================================
    private void StartInertialAcquisition()
    //==========================================================================
    {
        //        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //        mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        try {
            if (CalibrationMode)
            {
                if(!AccAquiring)
                    AccAquiring = mSensorManager.registerListener(this, mAcc, ACC_READING_PERDIOD_CALIB);

                if(!GyroAcquiring)
                    GyroAcquiring = mSensorManager.registerListener(this, mGyro, GYRO_READING_PERDIOD_CALIB);// 20.000 us ----> FsAMPLE = 50Hz
            }
            else
            {
                if(!AccAquiring)
                    AccAquiring = mSensorManager.registerListener(this, mAcc, ACC_READING_PERDIOD);

                if(!GyroAcquiring)
                    GyroAcquiring = mSensorManager.registerListener(this, mGyro, GYRO_READING_PERDIOD);// 20.000 us ----> FsAMPLE = 50Hz
            }
        } catch (Exception ex) {
            LogException(TAG, "StartInertialAcquisition exception: ", ex);
        }
        //AccStartAcquiring();
        //GyroStartAcquiring();
    }

    //==========================================================================
    private void StopInertialAcquisition()
    //==========================================================================
    {
        if (AccAquiring) {
            //super.onPause();
            mSensorManager.unregisterListener(this, mAcc);
            AccAquiring = false;
        }

        if (GyroAcquiring) {
            //super.onPause();
            mSensorManager.unregisterListener(this, mGyro);
            GyroAcquiring = false;
        }

        //AccStartAcquiring();
        //GyroStopAcquiring();
    }

    //==========================================================================
    private void StopAllAcquisitions()
    //==========================================================================
    {
        StopInertialAcquisition();

        StopTemperatureAcquisition();

        StopLightAcquisition();
        //TODO: verificare che non si debba aspettare un poco...
    }


    //==========================================================================
    protected void StartLightAcquisition()
    //==========================================================================
    {    //super.onResume();
        mSensorManager.registerListener(this, myLight, LIGHT_READING_PERDIOD, 1000000);// 20.000 us ----> FsAMPLE = 50Hz
    }
    //public static final short MaxiIO_WheelcPin = 6;

    //==========================================================================
    protected void StopLightAcquisition()
    //==========================================================================
    {
        mSensorManager.unregisterListener(this, myLight);
    }


    //==========================================================================
    protected void StartTemperatureAcquisition()
    //==========================================================================
    {    //super.onResume();
        TemperatureAcquiring = mSensorManager.registerListener(this, mTemperature, TEMPERATURE_READING_PERDIOD, 100000);// 20.000 us ----> FsAMPLE = 50Hz
    }
    //public static final short MaxiIO_WheelcPin = 6;

    //==========================================================================
    protected void StopTemperatureAcquisition()
    //==========================================================================
    {
        if (TemperatureAcquiring) {
            //super.onPause();
            mSensorManager.unregisterListener(this, mTemperature);
            TemperatureAcquiring = false;
        }
    }

    //==============================================================================================
    //==============================================================================================
    //  UX Operations
    //==============================================================================================
    //==============================================================================================

    //TODO: da rivedere perchè causa una buona parte dei leakage che rendono necessario l'intervento del GC
    //==========================================================================
    private void RefreshGUI()
    //==========================================================================
    {
        try {

            if (UpdateTextViewsEnabled)
                time_from_lat_ux_update_millis += UXUPDATE_PERIOD_IN_MILLIS_FAST;
            else
                time_from_lat_ux_update_millis += UXUPDATE_PERIOD_IN_MILLIS_SLOW;

            if (time_from_lat_ux_update_millis >= SLOW_INFO_UPDATE_PERIOD_IN_MILLIS) {

                myData.SignalStrength = myNetworkInfo.getSignalStrength();

                slow_info_update_counter = 0;
            }

            if (UpdateTextViewsEnabled) {
                if (CalibrationMode) {
                    //                acc_x_calib.UpdateStats();
                    //                acc_y_calib.UpdateStats();
                    //                acc_z_calib.UpdateStats();

                    acc_x_1_tview.setText(String.format("%.3f", myData.myInertialData.Acc_Mean_X));
                    acc_y_1_tview.setText(String.format("%.3f", myData.myInertialData.Acc_Mean_Y));
                    acc_z_1_tview.setText(String.format("%.3f", myData.myInertialData.Acc_Mean_Z));

                    acc_x_2_tview.setText(String.format("%.4f", myData.myInertialData.Acc_DevStd_X));
                    acc_y_2_tview.setText(String.format("%.4f", myData.myInertialData.Acc_DevStd_Y));
                    acc_z_2_tview.setText(String.format("%.4f", myData.myInertialData.Acc_DevStd_Z));

                    gyro_x_1_tview.setText(String.format("%.3f", myData.myInertialData.Gyro_Mean_X));
                    gyro_y_1_tview.setText(String.format("%.3f", myData.myInertialData.Gyro_Mean_Y));
                    gyro_z_1_tview.setText(String.format("%.3f", myData.myInertialData.Gyro_Mean_Z));

                    gyro_x_2_tview.setText(String.format("%.4f", myData.myInertialData.Gyro_DevStd_X));
                    gyro_y_2_tview.setText(String.format("%.4f", myData.myInertialData.Gyro_DevStd_Y));
                    gyro_z_2_tview.setText(String.format("%.4f", myData.myInertialData.Gyro_DevStd_Z));

                } else {

                    acc_x_1_tview.setText(String.format("%.3f", myData.myInertialData.m_acc_x));
                    acc_y_1_tview.setText(String.format("%.3f", myData.myInertialData.m_acc_y));
                    acc_z_1_tview.setText(String.format("%.3f", myData.myInertialData.m_acc_z));

                    gyro_x_1_tview.setText(String.format("%.3f", myData.myInertialData.last_gyro_x));
                    gyro_y_1_tview.setText(String.format("%.3f", myData.myInertialData.last_gyro_y));
                    gyro_z_1_tview.setText(String.format("%.3f", myData.myInertialData.last_gyro_z));
                }

                power_on_tview.setText( "ON  " + String.valueOf(myData.PowerONDailyCounter));
                power_off_tview.setText("OFF " + String.valueOf(myData.PowerOFFDailyCounter));
                motor_on_tview.setText("ON  " + String.valueOf(myData.MotorONDailyCounter));
                motor_off_tview.setText("OFF " + String.valueOf(myData.MotorOFFDailyCounter));

                acc_samples_tview.setText(String.valueOf(myData.myInertialData.acc_data_counter));

                acc_x_3_tview.setText(String.format("%.3f", myData.myInertialData.acc_offset[0]));
                acc_y_3_tview.setText(String.format("%.3f", myData.myInertialData.acc_offset[1]));
                acc_z_3_tview.setText(String.format("%.3f", myData.myInertialData.acc_offset[2]));

                gyro_samples_tview.setText(String.valueOf(myData.myInertialData.gyro_data_counter));

                gyro_x_3_tview.setText(String.format("%.3f", myData.myInertialData.gyro_offset[0]));
                gyro_y_3_tview.setText(String.format("%.3f", myData.myInertialData.gyro_offset[1]));
                gyro_z_3_tview.setText(String.format("%.3f", myData.myInertialData.gyro_offset[2]));

                acc_vel_x_tview.setText(String.format("%.3f", myData.myInertialData.velocity_x));

                acc_distance_x_tview.setText(String.format("%.3f", myData.myInertialData.HourlyDistanceCovered));

                acc_period_mean_tview.setText(String.format("%.3f", myData.myInertialData.Acc_MeanDeltaTime));
                acc_period_stdev_tview.setText(String.format("%.3f", myData.myInertialData.Acc_StdDevDeltaTime));

                gyro_period_mean_tview.setText(String.format("%.3f", myData.myInertialData.Gyro_MeanDeltaTime));
                gyro_period_stdev_tview.setText(String.format("%.3f", myData.myInertialData.Gyro_StdDevDeltaTime));

                //String s = "Temp. (°C): " + String.format("%.2f", myData.myTempData.GetMeanTemperature()) + " [Max " + String.format("%.2f", myData.myTempData.GetMaxTemperature()) + "]";

                temperature_mean_val_tview.setText(String.format("%.2f °C", myData.myTempData.GetMeanTemperature()));
                temperature_min_val_tview.setText(String.format("%.2f °C", myData.myTempData.GetMinTemperature()));
                temperature_max_val_tview.setText(String.format("%.2f °C", myData.myTempData.GetMaxTemperature()));

                //temperature_tview.setText(s);

                gyro_angle_tview.setText("-.-");
                signal_level_tview.setText(String.valueOf(myData.SignalStrength) + " dBm");

                battery_textview.setText(myData.myBatteryData.level + "%");

                memory_used_tview.setText(myData.MemoryUsed + " MB");
                memory_avail_tview.setText(myData.MemoryAvailable + " MB");

                app_uptime_tview.setText(String.format("%02d:%02d:%02d", RunningTime[1], RunningTime[2], RunningTime[3]));
                duty_uptime_tview.setText("00:00:00");

                hourly_ref_time_tview.setText("Hourly Alarm Reference Time : " +
                        new SimpleDateFormat("HH:mm:ss").format(new Date(Hourly_Alarm_Reference_Time)));

                ligth_val_tview.setText(String.valueOf(myData.CurrentLightValue));
                number_of_touch_tview.setText(String.valueOf(myData.NumberOfTouch));
            }

            sys_ref_date_textview.setText("Reference Date : " +
                    new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss").format(Daily_Reference_Date));

            if (myData.PowerON)
                btPowerOn.setBackgroundColor(Color.GREEN); //getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
            else
                btPowerOn.setBackgroundColor(Color.DKGRAY); //getBackground().setColorFilter(Color.DKGRAY, PorterDuff.Mode.MULTIPLY);

            if (myData.MotorON)
                btMotorOn.setBackgroundColor(Color.GREEN); //.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
            else
                btMotorOn.setBackgroundColor(Color.DKGRAY); //.getBackground().setColorFilter(Color.DKGRAY, PorterDuff.Mode.MULTIPLY);

            //app_uptime_tview.setText(AppUptimeString);

            //signal_level_tview.setText(String.valueOf());
            //updatetview_counter = 0;
            //}
        } catch (Exception ex) {
            LogException(TAG, "RefreshGUI exception: ", ex);
        }
    }

    //==============================================================================================
    //==============================================================================================
    //  NEW Yocto Methods
    //==============================================================================================
    //==============================================================================================


    //==========================================================================
    protected void Start_Yocto()
    //==========================================================================
    {
        // Connect to Yoctopuce Maxi-IO
        try {
            YAPI.EnableUSBHost(getApplicationContext());
            YAPI.SetUSBPacketAckMS(50);
            YAPI.RegisterHub("usb");
            YAPI.RegisterDeviceArrivalCallback(this);
            YAPI.RegisterDeviceRemovalCallback(this);
            YoctoRunnable.run();
        } catch (YAPI_Exception ex) {
            LogException(TAG, "Start_Yocto exception: ", ex);
        }
        //Start the runnable in 100 ms
        YoctoHandler.postDelayed(YoctoRunnable, 100);
    }

    //==========================================================================
    final Runnable YoctoRunnable = new Runnable()
            //==========================================================================
    {
        public void run() {
            try {
                //CHANGE: add a call to UpdateDeviceList to receive plug/unplug events
                YAPI.UpdateDeviceList();
                YAPI.HandleEvents();
                // DO THIS EVERYTIME TO LET IT WORK PROPERLY
                //CHANGE: calling Init_Yocto every time is a bad idea. This will trigger 3 USB request
                //        every time. The correct solution is to call one time and save the setting
                //        with the method saveToFlash(). See change in Start_Yocto

            } catch (YAPI_Exception e) {
                LogException(TAG, "final Runnable YoctoRunnable exception: ", e);
            }
            YoctoHandler.postDelayed(this, 500);
        }
    };

    //==========================================================================
    protected void Init_Yocto(YDigitalIO YoctoIOModule)
    //==========================================================================
    {        // set the port as input
        try {
            YoctoIOModule.set_portDirection(0x0);             //bit 0-3: OUT; bit 4-7: IN ( bit set to 0)
            YoctoIOModule.set_portPolarity(0);                 // polarity set to regular
            YoctoIOModule.set_portOpenDrain(0);                // No open drain
        } catch (YAPI_Exception e) {
            LogException(TAG, "Init_Yocto exception: ", e);
        }
    }

    //==========================================================================
    protected void Stop_Yocto()
    //==========================================================================
    {
        try {
            YAPI.FreeAPI();
            YoctoHandler.removeCallbacks(YoctoRunnable);
        } catch (Exception e) {
            LogException(TAG, "Stop_Yocto exception: ", e);
        }
    }

    // NEW VALUE ON PORT:
    //==========================================================================
    public void yNewValue(YDigitalIO yDigitalIO, String newPortValue)
    //==========================================================================
    {
        long new_event_time = System.nanoTime();
        try {
            MaxiIO_event_tview.setText(newPortValue);
            int portvalue = Integer.valueOf(newPortValue, 16);
            // CHECK MOTOR PIN VALUE
            //Motor_OldInputData = Motor_NewInputData;
            //CHANGE: It's far more efficient to compute the bit state form newPortValue.
            //        a call to "MaxiIO.get_bitState(MaxiIO_MotorPin" will trigger an USB
            //        transaction that can take few hundreds milliseconds
            Motor_NewInputData = (portvalue >> MaxiIO_MotorPin) & 1;

            // MOTOR EVENT HANDLING
            if (Motor_NewInputData == 1) {
                myData.AddMotorONEvent(new_event_time);

                StartInertialAcquisition();
            }

            if (Motor_NewInputData == 0) {

                StopInertialAcquisition();
                if(myData.MotorON)
                    myData.AddMotorOFFEvent(new_event_time);
            }
        } catch (Exception ex) {
            LogException(TAG, "yNewValue exception: ", ex);
        }
    }



    //==========================================================================
    public boolean ConnectYoctoMaxiIO()
    //==========================================================================
    {

        //CHANGE: do not init YAPI or do any enumeration only use MaxiIO object that
        //        have been set by the yDeviceArrival callback
        if (MaxiIO != null && MaxiIO.isOnline()) {
            YoctoInUse = true;
            MaxiIO_status_tview.setText("MaxiIO connected: YES");
        } else {
            YoctoInUse = false;
            MaxiIO_status_tview.setText("MaxiIO connected: NO");
        }
        //lastfiles.isyoctoinuse = YoctoInUse;
        return YoctoInUse;
    }

    //CHANGE: yDeviceArrival will be called every time the Yocto-Maxi-IO is plugged
    @Override
    public void yDeviceArrival(YModule module) {
        try {
            if (module.get_productName().equals("Yocto-Maxi-IO")) {
                MaxiIO_SerialN = module.get_serialNumber();
                MaxiIO_event_tview.setText("Yocto-Maxi-IO " + MaxiIO_SerialN + " connected");

                MaxiIO = FindDigitalIO(MaxiIO_SerialN);
                if (MaxiIO.isOnline()) {
                    //call_toast("Maxi-IO connected");
                    Init_Yocto(MaxiIO);
                    //CHANGE: Save port configuration in device flash. So event if the device reboot
                    //        the port will work correctly
                    module.saveToFlash();
                    MaxiIO.registerValueCallback(this);
                }
            }
        } catch (YAPI_Exception ex) {
            LogException(TAG, "yDeviceArrival exception: ", ex);
        }
    }

    @Override
    public void yDeviceRemoval(YModule module) {
        try {
            String serialNumber = module.get_serialNumber();
            if (serialNumber.equals(MaxiIO_SerialN)) {
                MaxiIO_status_tview.setText("No Yocto-Maxi-IO connected");
                MaxiIO = null;
                MaxiIO_SerialN = null;
            }
        } catch (YAPI_Exception ex) {
            LogException(TAG, "yDeviceRemoval exception: ", ex);
        }
    }


    //******************************************************************************
    //******************************************************************************
    //
    //              Calibration Routines
    //
    //******************************************************************************
    //******************************************************************************

    //==========================================================================
    private boolean CheckIfCalibrationCompleted()
    //==========================================================================
    {
        long delta_time = (System.nanoTime() - CalibrationStartTime) / 1000000000L;

        return (delta_time >= NUM_OF_SECONDS_CALIBRATION);
    }

    //==========================================================================
    public void StartCalibration() {
        //==========================================================================
        //int CountDown =  NUM_OF_SECONDS_CALIBRATION * 5;
        CalibrationStartTime = System.nanoTime();//Calendar.getInstance().getTime().getTime();

        //TODO: verificare che non ci sia da inserire un qualche reset delle strutture mYInewtrialData
        //Step 0. Clean the needed data structures and set flags
            /*
            acc_x_calib.reset_data();
            acc_y_calib.reset_data();
            acc_z_calib.reset_data();

            gyro_x_calib.reset_data();
            gyro_y_calib.reset_data();
            gyro_z_calib.reset_data();
            */

        CalibrationMode = true; //isCalibrating = true;

        //Step 1. Activate the sensors
        StartInertialAcquisition();
        LogDebug(TAG, "Calibration Started");
    }

    //==========================================================================
    public void StopCalibrateInertialSensors()
    //==========================================================================
    {
        try {
            if (CalibrationMode) {
                StopInertialAcquisition();
                //TimeUnit.MILLISECONDS.sleep(200);
                //Thread.sleep(200);//Sleep(100);
                Sleep(200,20);

                myData.UpdateCalibrationData();

                myData.myInertialData.UpdateBias();

                CalibrationMode = false;
                LogDebug(TAG, "Calibration Completed");
            }
        } catch (Exception ex) {
            LogException(TAG, "StopCalibrateInertialSensors exception: ", ex);
        }
    }

    //==========================================================================
    private void ResetHourlyCounters() {
        //==========================================================================
        myData.ResetHourlyCounters();

        myData.myTempData.ResetMinMax();
    }


    //==========================================================================
    private void call_toast(CharSequence text) {
        //==========================================================================
        // SETS A KIND OF POP-UP MESSAGE
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    //TODO: approfondire e verificare la quesitone degli errori e del Logging
    //==========================================================================
    private void SaveErrorLog(String msg) {
        //==========================================================================
        String StringToSend = "" + SystemClock.elapsedRealtime() + "\t" + msg + "\n";
        LogFile_Handler BkgSave_LogHandler = new LogFile_Handler(StringToSend);
        BkgSave_LogHandler.execute();
    }

    //******************************************************************************
    //******************************************************************************
    //
    //              Button Click Routines
    //
    //******************************************************************************
    //******************************************************************************

    //==========================================================================
    public void CalibrateButton(View view)
    //==========================================================================
    {
        StopInertialAcquisition();

        StartCalibration();

    }

    //==========================================================================
    public void UpdateAppButton(View view)
    //==========================================================================
    {
        myAzureManager.CheckAndUpdateAPK();

        myAzureManager.CheckAndUpdateConfig();
    }

    //==========================================================================
    public void ToggleViewButton_Click(View view)
    //==========================================================================
    {
        UpdateTextViewsEnabled = !UpdateTextViewsEnabled;

        if(UpdateTextViewsEnabled)
            btToggleView.setBackgroundColor(Color.YELLOW);
        else
            btToggleView.setBackgroundColor(Color.GREEN);

        StopPeriodicRefreshUX();

        //Sleep(1000, 100);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //cose da fare dopo il timeout

                //CancelAlarm(ViewRefreshUpdate_alarmMgr, ViewRefreshUpdate_pintent);
                if (UpdateTextViewsEnabled) {
                    //Thread.sleep(100);
                    StartPeriodicRefreshUX(UXUPDATE_PERIOD_IN_MILLIS_FAST);
                    //StartUXUpdateTimer(UXUPDATE_PERIOD_IN_MILLIS_FAST);
                } else {
                    //Thread.sleep(100);
                    StartPeriodicRefreshUX(UXUPDATE_PERIOD_IN_MILLIS_SLOW);
                    //StartUXUpdateTimer(UXUPDATE_PERIOD_IN_MILLIS_SLOW);
                }
            }
        }, 1000);
    }

    //==========================================================================
    public void ToggleManualModeButton(View view)
    //==========================================================================
    {
        if (ManualMode) {
            SetGUIButtonEnabled(false);

            btToggleMode.setText("Auto Mode");
            btToggleMode.setBackgroundColor(Color.GREEN); //getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);

            btCalibrate.setVisibility(Button.INVISIBLE);
            btPowerOff.setVisibility(Button.INVISIBLE);
            btMotorOff.setVisibility(Button.INVISIBLE);

            btToggleView.setVisibility(Button.INVISIBLE);
            btDoAppUpdate.setVisibility(Button.INVISIBLE);

            ManualMode = false;
        } else {
            SetGUIButtonEnabled(true);

            btToggleMode.setText("Manual Mode");
            btToggleMode.setBackgroundColor(Color.YELLOW); //.getBackground().setColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
            //setBackgroundColor(Color.LTGRAY);

            if(debug_mode)
            {
                btCalibrate.setVisibility(Button.INVISIBLE);
                btPowerOff.setVisibility(Button.INVISIBLE);
                btMotorOff.setVisibility(Button.INVISIBLE);
            }
            else {
                btCalibrate.setVisibility(Button.INVISIBLE);
                btPowerOff.setVisibility(Button.INVISIBLE);
                btMotorOff.setVisibility(Button.INVISIBLE);
            }

            btToggleView.setVisibility(Button.VISIBLE);
            btDoAppUpdate.setVisibility(Button.VISIBLE);

            ManualMode = true;
        }
    }

    //==========================================================================
    public void SendHourlyDataButton_Click(View view)
    //==========================================================================
    {
        myData.updateHourlyUse(Hourly_Reference_Time, System.nanoTime());
        myEventManager.SendHourlyStatusEvent(InternetOnline);
        ResetHourlyCounters();
    }

    /*
    //==========================================================================
    public void SendDailyDataButton_Click(View view)
    //==========================================================================
    {

        if (true) {
            try {
                DailyUpdaterTask dut = new DailyUpdaterTask();
                dut.execute();

            } catch (Exception ex) {
                LogException(TAG, "OnceEveryHour_Receiver exception: ", ex);
            }

        } else {

            //Step 1 - let's stop periodic operations and acquisitions (if any)
            StopPeriodicRefreshUX();//StopPeriodicUpdateUX();

            StopPeriodicOnlineCheck();

            StopAllAcquisitions();

            //Sleep(2000, 100);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //cose da fare dopo il timeout

                    //Step 2 -

                    SaveData();

                    //myData.updateDailyUse();

                    CloseAndSaveLogger();

                    CreateAndOpenNewFileLogger();

                    UpdateStorageMemoryAvailable();

                    myEventManager.SendDailyReport(InternetOnline);

                    //CalibrateAccelerometer();
                    //UpdateListofFilesToUpload();

                    //Upload Blobs
                    //myAzureManager.UploadFilesToBlobs(myData.myBatteryData.level);
                    myAzureManager.UploadFilesToBlobs();

                    //        tens_sec_counter = 18;
                    //        CheckForEmptyBlobListOrTimeout();

                    //WaitForEmptyBlobListOrTimeout(180);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            myAzureManager.CheckAndUpdateConfig();

                            //Sleep(2000, 100);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //cose da fare dopo il timeout

                                    myAzureManager.CheckAndUpdateAPK();

                                    myData.DailyReset(Daily_Reference_Time);//DailyResetData();

                                    ResetHourlyCounters();

                                    StartCalibration();

                                    StartTemperatureAcquisition();

                                    StartLightAcquisition();

                                    StartPeriodicRefreshUX();//StartPeriodicUpdateUX();

                                    StartPeriodicOnlineCheck(ONLINE_CHECK_UPDATE_PERIOD);

                                }
                            }, 2000);

                        }
                    }, 120000);

                }
            }, 2000);

        }
    }
*/

    //==========================================================================
    public void SendEventButton_Click(View view)
    //==========================================================================
    {
        try {
            //int i = 0;
            //i = i+1;
            //myEventManager.SendStringEvent(tag_view.getText().toString(), value_view.getText().toString());
            myEventManager.sendEventTestClick();
        } catch (Exception ex) {
            LogException(TAG, "SendEventButton_Click exception: ", ex);
        }
    }

    //==========================================================================
    private void SaveData()
    //==========================================================================
    {
        DataOutputStream data_out;
        String filename;
        String filename_complete;
        //String XMLFileList

        //Step 1 - Save Calibration file data
        try {
            LogDebug(TAG, "Saving Calib data");

            filename = CommonFilePreamble + "-calib.bin";
            filename_complete = myConfig.get_Acquisition_Folder() + filename;

            data_out = getDataOutputStream(filename_complete);

            data_out.writeFloat(myData.myInertialData.acc_offset[0]);
            data_out.writeFloat(myData.myInertialData.acc_offset[1]);
            data_out.writeFloat(myData.myInertialData.acc_offset[2]);

            data_out.writeFloat(myData.myInertialData.gyro_offset[0]);
            data_out.writeFloat(myData.myInertialData.gyro_offset[1]);
            data_out.writeFloat(myData.myInertialData.gyro_offset[2]);

            data_out.flush();
            data_out.close();

            //myAzureManager.AddFileToSavedFileList(myConfig.getAcquisitionsFolder(), filename, myConfig.get_Acquisition_Container());
            myAzureManager.AddFileToSavedFileList(filename);
            myData.SavedFileListString += filename + "; ";

        } catch (Exception ex) {
            LogException(TAG, "SaveData (calib.) exception: ", ex);
        }

        try {
            LogDebug(TAG, "Saving " + String.valueOf(myData.myInertialData.acc_data_counter) + " acc. samples");

            //Step 2 - Save Accelerometer data if available
            if (myData.myInertialData.acc_data_counter > 0) {

                filename = CommonFilePreamble + "-acc.bin";
                filename_complete = myConfig.get_Acquisition_Folder() + filename;

                data_out = getDataOutputStream(filename_complete);

                for (int i = 0; i < myData.myInertialData.acc_data_counter; i++) {
                    data_out.writeInt(myData.myInertialData.AccTimestampArray[i]);
                    data_out.writeFloat(myData.myInertialData.AccXDataArray[i]);
                    data_out.writeFloat(myData.myInertialData.AccYDataArray[i]);
                    data_out.writeFloat(myData.myInertialData.AccZDataArray[i]);
                }

                data_out.flush();
                data_out.close();

                //myAzureManager.AddFileToSavedFileList(myConfig.getAcquisitionsFolder(), filename, myConfig.get_Acquisition_Container());
                myAzureManager.AddFileToSavedFileList(filename);
                myData.SavedFileListString += filename + "\r\n";

            }
        } catch (Exception ex) {
            LogException(TAG, "SaveData (acc.) exception: ", ex);
        }

        try {

            LogDebug(TAG, "Saving " + String.valueOf(myData.myInertialData.gyro_data_counter) + " gyro samples");

            //Step 3 - Save Gyro data if available
            if (myData.myInertialData.gyro_data_counter > 0) {
                filename = CommonFilePreamble + "-gyro.bin";
                filename_complete = myConfig.get_Acquisition_Folder() + filename;

                data_out = getDataOutputStream(filename_complete);

                for (int i = 0; i < myData.myInertialData.gyro_data_counter; i++) {
                    data_out.writeInt(myData.myInertialData.GyroTimestampArray[i]);
                    data_out.writeFloat(myData.myInertialData.GyroXDataArray[i]);
                    data_out.writeFloat(myData.myInertialData.GyroYDataArray[i]);
                    data_out.writeFloat(myData.myInertialData.GyroZDataArray[i]);
                }

                data_out.flush();
                data_out.close();

                //myAzureManager.AddFileToSavedFileList(myConfig.getAcquisitionsFolder(), filename, myConfig.get_Acquisition_Container());
                myAzureManager.AddFileToSavedFileList(filename);
                myData.SavedFileListString += filename + "\r\n";
            }
        } catch (Exception ex) {
            LogException(TAG, "SaveData (gyro) exception: ", ex);
        }

        try {
            LogDebug(TAG, "Saving " + String.valueOf(myData.myTempData.data_counter ) + " temp. samples");

            //Step 4 - Save Temperature data
            if (myData.myTempData.data_counter > 0) {
                filename = CommonFilePreamble + "-temp.bin";
                filename_complete = myConfig.get_Acquisition_Folder() + filename;

                data_out = getDataOutputStream(filename_complete);

                for (int i = 0; i < myData.myTempData.data_counter; i++) {
                    data_out.writeInt(myData.myTempData.TimestampArray[i]);
                    //data_out.writeFloat((float)i);
                    data_out.writeFloat(myData.myTempData.DataArray[i]);
                }

                data_out.flush();
                data_out.close();

                //myAzureManager.AddFileToSavedFileList(myConfig.getAcquisitionsFolder(), filename, myConfig.get_Acquisition_Container());
                myAzureManager.AddFileToSavedFileList(filename);
                myData.SavedFileListString += filename + "\r\n";
            }
        } catch (Exception ex) {
            LogException(TAG, "SaveData (temperature) exception: ", ex);
        }

        try {

            LogDebug(TAG, "Saving " + String.valueOf(myData.myEventData.data_counter ) + " events");

            //Step 5 - Save Event info
            if (myData.myEventData.data_counter > 0) {
                filename = CommonFilePreamble + "-event.bin";
                filename_complete = myConfig.get_Acquisition_Folder() + filename;

                data_out = getDataOutputStream(filename_complete);

                for (int i = 0; i < myData.myEventData.data_counter; i++) {
                    data_out.writeInt(myData.myEventData.Timestamps[i]);
                    data_out.writeInt(myData.myEventData.EventArray[i]);
                }

                data_out.flush();
                data_out.close();

                //myAzureManager.AddFileToSavedFileList(myConfig.getAcquisitionsFolder(), filename, myConfig.get_Acquisition_Container());
                myAzureManager.AddFileToSavedFileList(filename);
                myData.SavedFileListString += filename + "\r\n";
            }
        } catch (Exception ex) {
            LogException(TAG, "SaveData (events) exception: ", ex);
        }

        try {

            LogDebug(TAG, "Saving " + String.valueOf(myData.myBatteryData.data_counter ) + " battery samples");

            //Step 7 - Save Battery Data
            filename = CommonFilePreamble + "-bat.bin";
            filename_complete = myConfig.get_Acquisition_Folder() + filename;

            data_out = getDataOutputStream(filename_complete);

            if (myData.myBatteryData.data_counter > 0) {

                for (int i = 0; i < myData.myBatteryData.data_counter; i++) {
                    data_out.writeInt(myData.myBatteryData.Timestamps[i]);
                    //data_out.writeFloat((float)i);
                    data_out.writeFloat(myData.myBatteryData.Values[i]);
                }

                data_out.flush();
                data_out.close();

                myAzureManager.AddFileToSavedFileList(filename);
                myData.SavedFileListString += filename + "\r\n";
            }
        } catch (Exception ex) {
            LogException(TAG, "SaveData (battery) exception: ", ex);
        }

            /*
            try
            {

                //Step 8 - Save the list of files saved
                if (myAzureManager.FilesToSend.size() > 0 )
                {
                    filename = myConfig.get_Acquisition_Folder() + CommonFilePreamble + "-filelist.txt";
                    data_out = getDataOutputStream(filename);

                    for (int i = 0; i < myAzureManager.FilesToSend.size(); i++ )
                    {
                        data_out.writeBytes(myAzureManager.FilesToSend.get(i) + System.getProperty("line.separator"));
                    }

                    myAzureManager.AddFileToSavedFileList(filename);

                    data_out.flush();
                    data_out.close();
                }
            } catch (Exception ex) {
                LogException(TAG, "SaveData", ex);
            }
    */
    }

        /*
        private void DebugTestWriteBin() {
            long LastTime = System.nanoTime();//Calendar.getInstance().getTime().getTime();
            long mills = (LastTime - Daily_Reference_Time) / 100000;


            //AccTimestampArray[acc_data_counter] = (int) ((event.timestamp - Daily_Reference_Time) / 100000);


            try {

                //Step 7 - Save Battery Data
                String filename = "test-bat.bin";
                String filename_complete = myConfig.get_Acquisition_Folder() + filename;

                DataOutputStream data_out = getDataOutputStream(filename_complete);
                int act_time;
                for (int i = 0; i < 100; i++) {
                    LastTime = System.nanoTime();//Calendar.getInstance().getTime().getTime();
                    mills = (LastTime - Daily_Reference_Time) / 100000;
                    act_time = (int) ((System.nanoTime() - Daily_Reference_Time) / 100000L);
                    data_out.writeInt(act_time);
                    data_out.writeFloat(((float) i) / 10.0f);
                    //data_out.writeFloat(myData.myBatteryData.Values[i]);
                    Sleep(20, 20);
                }
                data_out.flush();
                data_out.close();

                //myAzureManager.AddFileToSavedFileList(filename);
                //myData.SavedFileListString += filename + "\YoctoRunnable\n";

            } catch (Exception ex) {
                LogException(TAG, "SaveData", ex);
            }
        }
    */


    //==========================================================================
    private void Sleep_CountDownTimer(int ms_to_sleep, int ms_interval)
    //==========================================================================
    {
        CountDownTimer cdt = new CountDownTimer(ms_to_sleep, ms_interval) {

            @Override
            public void onTick(long millisUntilFinished) {
                // do something after 1s
            }

            @Override
            public void onFinish() {
                // do something end times 5s
            }

        }.start();

            /*
            try {
                Thread.sleep(ms_to_sleep);
            } catch (Exception ex) {
                LogException(TAG, "Sleep", ex);
            }
            */
    }

    //==========================================================================
    private void Sleep(int new_ms_to_sleep, int new_ms_interval)
    //==========================================================================
    {
        int my_ms_to_sleep = new_ms_to_sleep;
        int my_ms_interval = new_ms_interval;

        try {
            for (; ; ) {
                Thread.sleep(my_ms_interval);
                my_ms_to_sleep -= my_ms_interval;
                if (my_ms_to_sleep <= 0)
                    break;
            }
        } catch (Exception ex) {
            LogException(TAG, "Sleep exception: ", ex);
        }
    }

    private void checkIfWaitFinished() {
        if (ms_to_sleep > 0) {
            ms_to_sleep -= ms_interval;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkIfWaitFinished();
                }
            }, ms_interval);
        }
    }


    //==========================================================================
    public void UpdateRunningTime()
    //==========================================================================
    {
        long LastMeasuredTime = System.nanoTime();//Calendar.getInstance().getTime().getTime();

        long mills = (LastMeasuredTime - Daily_Reference_Time) / 1000000L;

        int Hours = (int) (mills / (1000 * 60 * 60));
        int Mins = (int) (mills / (1000 * 60)) % 60;
        int Secs = (int) (mills - (long) Hours * (1000 * 60 * 60) - (long) (Mins) * (1000 * 60)) / 1000;
        int ms = 0;

        RunningTime[0] = Hours / 24;
        RunningTime[1] = Hours % 24;
        RunningTime[2] = Mins;
        RunningTime[3] = Secs;

        //AppUptimeString = String.format("%2d:%2d:%2d", Hours, Mins, Secs);
    }


    //==========================================================================
    public long GetActualTimeInMillis()
    //==========================================================================
    {
        MainCalendar.setTimeInMillis(System.currentTimeMillis());
        return MainCalendar.getTimeInMillis();
    }

    //==========================================================================
    public long GetNextStartHourAndTenInMillis() {
        //==========================================================================

        Calendar tmpCalendar = Calendar.getInstance();

        //int hourofday = tmpCalendar.get(Calendar.HOUR_OF_DAY);
        //tmpCalendar.setTimeInMillis(System.currentTimeMillis());
        tmpCalendar.set(Calendar.MILLISECOND, 0);
        tmpCalendar.set(Calendar.SECOND, 0);
        tmpCalendar.set(Calendar.MINUTE, 10);
        tmpCalendar.add(Calendar.HOUR_OF_DAY, 1);

        return tmpCalendar.getTimeInMillis();
    }

    //==========================================================================
    public long GetNextStartHourAndHalfInMillis() {
        //==========================================================================

        Calendar tmpCalendar = Calendar.getInstance();

        //int hourofday = tmpCalendar.get(Calendar.HOUR_OF_DAY);
        //tmpCalendar.setTimeInMillis(System.currentTimeMillis());
        tmpCalendar.set(Calendar.MILLISECOND, 0);
        tmpCalendar.set(Calendar.SECOND, 0);
        tmpCalendar.set(Calendar.MINUTE, 30);
        tmpCalendar.add(Calendar.HOUR_OF_DAY, 1);

        return tmpCalendar.getTimeInMillis();
    }

    //==========================================================================
    public long GetNextStartHourAndQuarterInMillis() {
        //==========================================================================

        Calendar tmpCalendar = Calendar.getInstance();

        //int hourofday = tmpCalendar.get(Calendar.HOUR_OF_DAY);
        //tmpCalendar.setTimeInMillis(System.currentTimeMillis());
        tmpCalendar.set(Calendar.MILLISECOND, 0);
        tmpCalendar.set(Calendar.SECOND, 0);
        tmpCalendar.set(Calendar.MINUTE, 15);
        tmpCalendar.add(Calendar.HOUR_OF_DAY, 1);

        return tmpCalendar.getTimeInMillis();
    }

    //==========================================================================
    public int GetActualHourOfDay()
    //==========================================================================
    {
        MainCalendar.setTimeInMillis(System.currentTimeMillis());
        return MainCalendar.get(Calendar.HOUR_OF_DAY);
    }

    //==========================================================================
    private void UpdateDailyReferenceTimeAndDate()
    //==========================================================================
    {
        Daily_Reference_Time = System.nanoTime();
        Daily_Reference_Date = new Date();
    }

    //TODO: da verificare l'implementazione della chiusura e riapertura del file .log
    //==========================================================================
    private void CloseAndSaveLogger()//boolean initializing)
    //==========================================================================
    {
        //CLose the existing Logger (if any)
        //if (!initializing) {
        FileLog.close();

        //Save logfile
        myAzureManager.AddFileToSavedFileList(logger_filename);
        //myAzureManager.AddFileToSavedFileList(myConfig.getAcquisitionsFolder(), logger_filename, myConfig.get_Acquisition_Container());
        myData.DailyLogFileName = logger_filename;
        //}
    }

    //==========================================================================
    private void CreateAndOpenNewFileLogger()
    //==========================================================================
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MMdd-HHmmss");
        CommonFilePreamble = myConfig.WheelchairID + "-" + formatter.format(Daily_Reference_Date);
        logger_filename = CommonFilePreamble + ".log";
        logger_filename_complete = myConfig.get_Acquisition_Folder() + logger_filename;
        //Open the new one for the new day
        FileLog.open(logger_filename_complete, Log.VERBOSE, MAX_LOGFILE_SIZE);
        FileLog.d("Wheelchair Remote Monitor", "ID = " + myConfig.get_WheelchairID() + " (build = " + Integer.toString(CURRENT_BUILD) + ")", null);
    }

    //==========================================================================
    private void LogException(String tag, String msg, Exception ex)
    //==========================================================================
    {
        //ex.printStackTrace();
        //SaveErrorLog(ex.toString());
        //Log.e(tag, msg, ex);
        FileLog.e(tag, msg, ex);
    }

    //==========================================================================
    private void LogDebug(String tag, String msg)
    //==========================================================================
    {
        //ex.printStackTrace();
        //SaveErrorLog(ex.toString());
        FileLog.d(tag, msg, null);
    }


    @NonNull
    private DataOutputStream getDataOutputStream(String filename) throws FileNotFoundException {
        DataOutputStream data_out;
        data_out = new DataOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(filename, true)));
        return data_out;
    }

    //==========================================================================
    private int getDeltaTime()
    //==========================================================================
    {

        return (int) ((System.nanoTime() - Daily_Reference_Time) / 100000);
    }

    //==========================================================================
    private void ChangeStatus(int new_status)
    //==========================================================================
    {
        Status = new_status;
        sys_stat_textview.setText("System status = " + STATUS_STRING[new_status]);

    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction0() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    public void SendEMail(String subject, String attachment, String body) {
        Mail m = new Mail("mariner.wheelchair@gmail.com", "c4p3c3l4tr066");

        String _subject = "Mariner Project (From " + myConfig.WheelchairID + ") " + subject;
        String _body = body + System.getProperty("line.separator");

        //String[] toArr = {"bla@bla.com", "lala@lala.com"};
        m.setTo(myConfig.SendEmailTo);
        //m.setFrom("PW - "+ myConfig.WheelchairID + " <mariner.wheelchair@gmail.com>");
        m.setFrom("mariner.wheelchair@gmail.com");
        m.setSubject(_subject);
        m.setBody(_body);

        try {
            if (attachment != "")
                m.addAttachment(attachment);
            if (m.send()) {
                LogDebug(TAG, "email sent");
                //Toast.makeText(MailApp.this, "Email was sent successfully.", Toast.LENGTH_LONG).show();
            } else {
                LogDebug(TAG, "email NOT sent");
                //Toast.makeText(MailApp.this, "Email was not sent.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            //Toast.makeText(MailApp.this, "There was a problem sending the email.", Toast.LENGTH_LONG).show();
            LogException(TAG, "Could not send email", e);
        }
    }

        /*
        //TODO: probabilmente da tagliare completamente, dato che non abbiamo più l'Init Activity
        //==========================================================================
        void ReturnToInit() {
            //==========================================================================
            // SWITCH OFF ACTIVITY AND SET RESULT TO INIT ACTIVITY
            Intent intent = new Intent();
            intent.putExtra("files", lastfiles);
            setResult(RESULT_OK, intent);
            finish();
        }


        //==========================================================================
        private void DeleteLastAcquisitionFiles() {
            //==========================================================================

            String CurLine;
            String CurLine_time;
            int i;
            List<String> records; //2016-0114 cancellato da pm = new ArrayList<>();
            List<String> records_toBeDeleted = new ArrayList<>();
            records = notSentFileHandler.ReadTheWholeFile();

            String LastLine = records.get(records.size() - 1);
            String LastLine_time = LastLine.substring(48, 67); // qualcosa del tipo "2015_10_28_14_30_17"

            for (i = 0; i < records.size(); i++) {
                CurLine = records.get(i);
                CurLine_time = CurLine.substring(48, 67); // qualcosa del tipo "2015_10_28_14_30_17"

                if (CurLine_time.equals(LastLine_time)) {
                    String CurLine_source = CurLine.substring(44, 47); // qualcosa del tipo "Acc"
                    if (!(CurLine_source.equals(getResources().getString(R.string.POWER_filename_prefix)))
                            && !(CurLine_source.equals(getResources().getString(R.string.BATTERY_filename_prefix)))) {
                        // cancella i file, se non sono della batteria o della carrozzina
                        records_toBeDeleted.add(CurLine);
                    }
                }
            }

            // dopo che ho visto tutti gli elementi di records ri scrivo il file e cancello i file di dati
            for (i = 0; i < records_toBeDeleted.size(); i++) {
                notSentFileHandler.DeleteLine(records_toBeDeleted.get(i));
                File FileToBeDeleted = new File(records_toBeDeleted.get(i));
                FileToBeDeleted.delete();
            }
        }

        //TODO: da rivedere per bene
        //==========================================================================
        public void WhatToDoAt2InTheNight(View view) {
            //==========================================================================
            // stop acquisitions and calibrate phone position with a 30sec acquisition
            SwitchOffEverything();
            //NumOfWCActivation_Current = NumOfWCActivation;

            CalibrationMode = true;
            //isCalibrating = true;

            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            AccAquiring = mSensorManager.registerListener(this, mAcc, 50000);
        }


        float ramp_acc = 0;
    */
        /*
        private void ProveDiSleep() {
            // Prove relative allo Sleep ...

            Sleep(1000, 100);

            Sleep(7000, 1000);

            Sleep(10000, 100);


            Handler handler = new Handler();
            handler.postDelayed(new

                                        Runnable() {
                                            @Override
                                            public void run() {
                                                ;
                                            }
                                        }

                    , 5000);

            handler = new

                    Handler();

            handler.postDelayed(new

                                        Runnable() {
                                            @Override
                                            public void run() {
                                                ;
                                            }
                                        }

                    , 10000);
        }
    */


} // fine della MainActivity