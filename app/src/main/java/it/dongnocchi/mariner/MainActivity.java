package it.dongnocchi.mariner;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import android.util.Log;

import com.yoctopuce.YoctoAPI.YAPI;
import com.yoctopuce.YoctoAPI.YAPI_Exception;
import com.yoctopuce.YoctoAPI.YDigitalIO;
import com.yoctopuce.YoctoAPI.YModule;

import static com.yoctopuce.YoctoAPI.YDigitalIO.FindDigitalIO;


//==========================================================================
public class MainActivity extends Activity
        implements SensorEventListener, YDigitalIO.UpdateCallback {
    //==========================================================================

    /* TODO LIST:

    -) rivedere la politica di logging
    -) Inserire riga con titoli delle colonne dei dati
    -) Mettere tutto in forma di Landscape
    -) Rivedere la questione dello spegnimento della App e dei vari ReStart() etc.
    -) rivedere l'abilitazione e disabilitazione dei pulsanti e l'"auto mode"
    -) rivedere la questione della luminosità del display (per il momento tolta)
    -) rivedere e verificare le allocazioni di memoria
    */

    //TODO: da verificare tutta la politica di Logging

    //xxyyy xx = major release, yyy = minor release
    public static final int CURRENT_BUILD = 1003;

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final int STATUS_INIT = 0;
    public static final int STATUS_SLEEP = 1;
    public static final int STATUS_IDLE = 2;
    public static final int STATUS_ACTIVE = 3;
    public static final int STATUS_DAILY_UPDATE = 4;
    public static final int STATUS_OFFLINE = 5;
    public static final String[] STATUS_STRING = {"INIT", "SLEEP", "IDLE", "ACTIVE", "DAILY_UPDATE", "OFFLINE"};

    public static final short MaxiIO_MotorPin = 7;

    static final int MAX_LOGFILE_SIZE = 20000000;
    private static final int ACC_READING_PERDIOD = 20000; // 20 ms
    private static final int GYRO_READING_PERDIOD = 20000; //20 ms
    private static final int TEMPERATURE_READING_PERDIOD = 10000000; //10s
    //During the Calibration, we require a less frequent sampling period
    private static final int ACC_READING_PERDIOD_CALIB = 50000;
    private static final int GYRO_READING_PERDIOD_CALIB = 50000;
    private static final int NUM_OF_TEMPERARURE_SAMPLES = 8650; //8640 sarebbe il numero corretto
    private static final int NUM_OF_SIGNAL_STRENGTH_SAMPLES = 8650; //8640 sarebbe il numero corretto
    private static final int NUM_OF_SECONDS_CALIBRATION = 10;
    private static final int CALIB_DATA_SIZE = 600; // 500 per 10 secondi
    //Reference date reporting the reset performed at the very first start
    //and at the reset each night. Needs to be evaluated very close to Daily_referece_time
    static Date Daily_Reference_Date;
    //daily Reference time to be used for timestamping the acquisitions
    static long Daily_Reference_Time;
    static long Hourly_Reference_Time;
    // App starting date - this is unique until the app restarts
    static Date App_Start_Date;

    //TODO: parametrizzare il valore del tempo massimo prima della notifica del onSensorChanged

    //boolean isWheelchair_ON = false;
    // App starting time
    static long AppStartTime;
    //TODO: da sistemare per bene nella versione definitiva di produzione
    final int UXUPDATE_PERIOD_IN_MILLIS_SLOW = 1000;
    final int UXUPDATE_PERIOD_IN_MILLIS_FAST = 200;
    final int SLOW_INFO_UPDATE_PERIOD_IN_MILLIS = 1000 * 60 * 6; //Signal Strength, memory, etc.
    //final int SLOW_INFO_UPDATE_RATIO = SLOW_INFO_UPDATE_PERIOD_IN_MILLIS / UXUPDATE_PERIOD_IN_MILLIS;
    int time_from_lat_ux_update_millis;
    boolean AccAquiring = false;
    boolean GyroAcquiring = false;
    boolean TemperatureAcquiring = false;
    // variables used for the 30 sec acquisition for calibration
    boolean CalibrationMode = false;
    TimestampedDataArray acc_x_calib, acc_y_calib, acc_z_calib;
    TimestampedDataArray gyro_x_calib, gyro_y_calib, gyro_z_calib;
    // TextView
    TextView acc_x_1_tview, acc_y_1_tview, acc_z_1_tview;
    TextView acc_x_2_tview, acc_y_2_tview, acc_z_2_tview;
    TextView acc_x_3_tview, acc_y_3_tview, acc_z_3_tview;
    TextView acc_vel_x_tview;
    TextView acc_distance_x_tview;
    TextView acc_period_mean_tview, acc_period_stdev_tview;
    TextView gyro_x_1_tview, gyro_y_1_tview, gyro_z_1_tview;
    TextView gyro_x_2_tview, gyro_y_2_tview, gyro_z_2_tview;
    TextView gyro_x_3_tview, gyro_y_3_tview, gyro_z_3_tview;
    TextView gyro_angle_total_tview, gyro_angle_tview;
    TextView gyro_period_mean_tview, gyro_period_stdev_tview;
    TextView MaxiIO_textview;
    TextView battery_textview;
    TextView event_textview;
    TextView sys_stat_textview;
    TextView build_tview;
    TextView temperature_min_val_tview;
    TextView temperature_max_val_tview;
    TextView temperature_mean_val_tview;
    TextView signal_level_tview;
    TextView memory_used_tview, memory_avail_tview;
    TextView app_uptime_tview, duty_uptime_tview;
    Button btPowerOn, btPowerOff, btMotorOn, btMotorOff;
    Button btSendEvent, btSendDailyReport, btSendHourlyReport;
    Button btCalibrate, btToggleView, btToggleMode;
    Button btDoHourlyUpdate, btDoDailyUpdate;
    boolean UpdateTextViewsEnabled = false;
    //Array containing the information about the running time of the App
    int[] RunningTime;
    // CLASSES FOR COMMUNICATIONS BETWEEN ACTIVITIES
    //User user;              //input to this class
    //LastFiles lastfiles;    //output
    // phone network variables
    TelephonyManager TelephonManager;
    it.dongnocchi.mariner.NetworkInfo myNetworkInfo;
    AzureManager myAzureManager;
    Configuration myConfig;
    AzureEventManager myEventManager;
    int slow_info_update_counter;
    int actual_hour_of_day;
    //BroadcastReceiver OnceAnHour_Receiver;
    Calendar MainCalendar;
    PendingIntent OnceAnHour_pintent;
    AlarmManager OnceAnHour_alarmMgr;

    //TODO: da passare nel file config
    final int DAILY_REPORT_HOUR = 2;

    //int SignalStrength = 0;
    //BroadcastReceiver ViewRefreshUpdate_Receiver;
    PendingIntent ViewRefreshUpdate_pintent;
    AlarmManager ViewRefreshUpdate_alarmMgr;
    it.dongnocchi.mariner.NotSentFileHandler notSent;
    WindowManager.LayoutParams NewLayoutParams = null;
    it.dongnocchi.mariner.WheelchairData myData;
    String logger_filename;

    //==============================================================================================
    // YOCTOPUCE - MAXI-IO
    //==============================================================================================
    String MaxiIO_SerialN;
    YDigitalIO MaxiIO;
    YModule tmp;
    private Handler handler = new Handler();

    //int Motor_OldInputData;

    int Motor_NewInputData;
    private SensorManager mSensorManager;
    private Sensor mAcc;
    //private int ViewRefreshUpdate_period_ms = 1000;
    private Sensor mGyro;
    private Sensor mTemperature;
    private BatteryManager myBatteryManager;
    private boolean ManualMode = false;

    private long CalibrationStartTime;

    //FileLog myFileLog;
    private long AcquisitionStartTime;
    private String CommonFilePreamble;
    // INDICATE WHEN YOCTO IS IN USE (AVAILABLE)
    private boolean YoctoInUse = false;
    private int Status;


    @Override
    //==========================================================================
    protected void onCreate(Bundle savedInstanceState)
    //==========================================================================
    {
        try {
            //Initialize Time info and data structures

            AppStartTime = System.nanoTime();
            App_Start_Date = new Date();

            SetDailyReferenceTimeAndDate();

            Hourly_Reference_Time = Daily_Reference_Time;

            RunningTime = new int[5]; //0 = days, 1 = hours, 2 = mins, 3 = sec, 4 = ms

            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            MainCalendar = Calendar.getInstance();

            myConfig = new Configuration();
            myConfig.currentBuild = CURRENT_BUILD;


            //Initialize UX
            InitUX();

            build_tview.setText("Build: " + CURRENT_BUILD);

            CreateAndOpenNewFileLogger();
            //myLogger = new FileLog();

            //TODO: da rivedere l'intera politica di Logging
            FileLog.d("MARINER", "App started", null);
            //FileLog.close();
            //CreateAndOpenNewFileLogger();

            //lastfiles = new it.dongnocchi.mariner.LastFiles();                              // SET OUTPUT TO INIT ACTIVITY

            // INITIALISE SENSOR MANAGER and Sensors
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

            mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            myBatteryManager = new BatteryManager();

            // REGISTER BROADCAST RECEIVER FOR BATTERY EVENTS
            registerReceiver(mBatChargeOff, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
            registerReceiver(mBatChargeOn, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
            registerReceiver(mBatLow, new IntentFilter(Intent.ACTION_BATTERY_LOW));
            //registerReceiver(mBatOkay, new IntentFilter(Intent.ACTION_BATTERY_OKAY));
            registerReceiver(mBatChanged, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            myData = new it.dongnocchi.mariner.WheelchairData(Daily_Reference_Time);

            acc_x_calib = new TimestampedDataArray(CALIB_DATA_SIZE);
            acc_y_calib = new TimestampedDataArray(CALIB_DATA_SIZE);
            acc_z_calib = new TimestampedDataArray(CALIB_DATA_SIZE);

            gyro_x_calib = new TimestampedDataArray(CALIB_DATA_SIZE);
            gyro_y_calib = new TimestampedDataArray(CALIB_DATA_SIZE);
            gyro_z_calib = new TimestampedDataArray(CALIB_DATA_SIZE);

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

            //Verifica se è presente l'alimentazione, ed in questo caso aggiunge
            //un evento fittizio di PowerON
            if (isSupplyPowerPresent()) {
                myData.AddPowerONEvent(AppStartTime);
            }

            start_network_listener();

            //CloseSaveAndRestartLogger(true);

            DailyResetData();

            StartCalibration();

            StartTemperatureAcquisition();

            StartPeriodicOperations();

            SetUXInteraction(false);

            // inizializzazione eventhub manager
            myEventManager = new AzureEventManager(getApplicationContext(), new it.dongnocchi.mariner.AsyncResponse() {
                @Override
                public void processFinish(String output) {
                }
            }, myConfig, myData);

            notSent = new it.dongnocchi.mariner.NotSentFileHandler(myConfig.get_Wheelchair_path());

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

            //CreateMyWheelchairFile();
            //call_toast(ByteOrder.nativeOrder().toString()); system is little endian
            //FileLog.d(TAG, "onCreate completed");
            myEventManager.SendEventNew("APP_ON_CREATE", myData.myBatteryData.level, "");


        } catch (Exception ex) {
            LogException(TAG, "onCreate", ex);
        }
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        myEventManager.SendEventNew("APP_ON_RESTART", myData.myBatteryData.level, "");
        FileLog.d("MARINER", "App RESTART", null);

    }

    @Override
    protected void onPause() {
        myEventManager.SendEventNew("APP_ON_PAUSE", myData.myBatteryData.level, "");
        FileLog.d("MARINER", "App Pause", null);
        super.onPause();
    }

    @Override
    protected void onResume() {

        super.onResume();
        myEventManager.SendEventNew("APP_ON_RESUME", myData.myBatteryData.level, "");
        FileLog.d("MARINER", "App RESUME", null);

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
            myEventManager.SendEventNew("BATTERY_LOW", myData.myBatteryData.level, "");
            myData.AddBatteryValChangeEvent(eventtime, battery_intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
            //battery_textview.setText(myData.BatteryLevel + "%");
            // save data
            //Battery_AppendData(BatteryLevel, false);
        }
    };

    //==========================================================================
    private BroadcastReceiver OnceAnHour_Receiver = new BroadcastReceiver()
            //==========================================================================
    {
        @Override
        public void onReceive(Context context, Intent intent) {

            //SimpleDateFormat formatter = new SimpleDateFormat("HH_mm_ss");
            //Date now = new Date();
            //stringafake = formatter.format(now);
            //event_textview.setText(stringafake);
            //SendEvent_SystemStatus();
            //SendHourlyStatusEvent();
            //NumOfSecWorking_LastHour = 0;
            //Calendar rightNow = Calendar.getInstance();
            //rightNow.get(Calendar.HOUR_OF_DAY); // gets hour in 24h format

            long actual_time = System.nanoTime();
            actual_hour_of_day = GetActualHourOfDay();//

            myData.updateHourlyUse(Hourly_Reference_Time, actual_time);

            if (actual_hour_of_day == DAILY_REPORT_HOUR) { // do this only once a day at 2 in the night

                //Step 1 - let's stop periodic operations and acquisitions (if any)
                Stop_Periodic_Operations();

                StopAllAcquisitions();

                //Step 2 -

                SaveData();

                myData.updateDailyUse();

                myEventManager.SendDailyReport();

                //CalibrateAccelerometer();

                //UpdateListofFilesToUpload();

                //Upload Blobs
                myAzureManager.UploadBlobs(myData.myBatteryData.level);

                //TODO: sarà da ripristinare
                //Check for App updates
                //myAzureManager.CheckNewUpdates(myData.myBatteryData.level);

                myData.DailyReset(Daily_Reference_Time);//DailyResetData();

                ResetHourlyCounters();

                StartCalibration();

                StartTemperatureAcquisition();

                StartPeriodicOperations();

                //TODO: Verificare che vada bene rimuovere le linee commentate qui sotto
                //CreateMyWheelchairFile();
                //WhatToDoAt2InTheNight(null); // STOP INERTIAL ACQUISITION AND CALIBRATE SMARTPHONE POSITION

//                    } else if(actual_hour == 3){
//                        //spegni la rete wifi
//                        myNetworkInfo.MyWiFiManager(getApplicationContext(), false);
            } else {

                //myData.updateHourlyUse(Hourly_Reference_Time, actual_time);
                myEventManager.SendHourlyStatusEvent();
                ResetHourlyCounters();
            }

            Hourly_Reference_Time = actual_time;
        }
    };

    //==========================================================================
    private BroadcastReceiver ViewRefreshUpdate_Receiver = new BroadcastReceiver()
            //==========================================================================
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Aggiorno i dati presenti sullo schermo

            if (UpdateTextViewsEnabled)
                time_from_lat_ux_update_millis += UXUPDATE_PERIOD_IN_MILLIS_FAST;
            else
                time_from_lat_ux_update_millis += UXUPDATE_PERIOD_IN_MILLIS_SLOW;

            if (time_from_lat_ux_update_millis >= SLOW_INFO_UPDATE_PERIOD_IN_MILLIS) {

                myData.SignalStrength = myNetworkInfo.getSignalStrength();

                slow_info_update_counter = 0;
            }

            UpdateUX();
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
    final Runnable r = new Runnable()
            //==========================================================================
    {
        public void run() {
            if (MaxiIO_SerialN != null) {
                //YDigitalIO io = YDigitalIO.FindDigitalIO(MaxiIO_SerialN);
                try {
                    YAPI.HandleEvents();
                    // DO THIS EVERYTIME TO LET IT WORK PROPERLY
                    Init_Yocto(MaxiIO);

                    // da togliere per versione finale app
                    /*_outputdata = (_outputdata + 1) % 16;   // cycle ouput 0..15
                    io.set_portState(_outputdata);          // set output value*/

                } catch (YAPI_Exception e) {
                    LogException(TAG, "final Runnable r", e);
                }
            }
            handler.postDelayed(this, 200);
        }
    };




    //==========================================================================
    private void DailyResetData()
    //==========================================================================
    {        //TODO: implementare il reset di tutte le strutture dati utilizzate
        SetDailyReferenceTimeAndDate();
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
        //TODO: il try-catch è da rimuovere quando si andrà in produzione
        try {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    if (CalibrationMode) {
                        acc_x_calib.Add(event.values[0], event.timestamp);
                        acc_y_calib.Add(event.values[1], event.timestamp);
                        acc_z_calib.Add(event.values[2], event.timestamp);

                        if (CheckIfCalibrationCompleted())
                            StopCalibrateInertialSensors();

                        //TODO: da togliere in produzione. opzione di Debug temporanea
                        //UpdateUX();
                    }                //Acc_AppendData(event, false);
                    else {
                        myData.myInertialData.UpdateAccData(event);
                    }

                    break;

                case Sensor.TYPE_GYROSCOPE:
                    //Gyro_AppendData(event, false);
                    if (CalibrationMode) {
                        gyro_x_calib.Add(event.values[0], event.timestamp);
                        gyro_y_calib.Add(event.values[1], event.timestamp);
                        gyro_z_calib.Add(event.values[2], event.timestamp);

                        if (CheckIfCalibrationCompleted())
                            StopCalibrateInertialSensors();

                    }                //Acc_AppendData(event, false);
                    else {
                        myData.myInertialData.UpdateGyroData(event);
                    }
                    break;

                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    myData.myTempData.AppendData(event);

                    //TODO: da sistemare una routine con un intervallo di un secondo...
                    UpdateRunningTime();

                    //Aggiorno la visualizzazione dei dati
                    //UpdateUX();

                    break;
            }
        } catch (Exception ex) {
            LogException(TAG, "onSensorChanged", ex);
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
        MaxiIO_textview = (TextView) findViewById(R.id.MaxiIO_view);
        event_textview = (TextView) findViewById(R.id.event_view);
        sys_stat_textview = (TextView) findViewById(R.id.system_status_view);
        build_tview = (TextView) findViewById(R.id.system_build_view);

        app_uptime_tview = (TextView) findViewById(R.id.app_uptime_tview);
        duty_uptime_tview = (TextView) findViewById(R.id.current_duty_uptime_tview);

        signal_level_tview = (TextView) findViewById(R.id.signal_level_val_tview);
        //temp_val_tview = (TextView) findViewById(R.id.temperature_val_tview);

        memory_used_tview = (TextView) findViewById(R.id.memory_val_tview);
        memory_avail_tview = (TextView) findViewById(R.id.memory_avail_val_tview);

        //temperature_tview = (TextView) findViewById(R.id.temperature_tview);
        temperature_mean_val_tview = (TextView) findViewById(R.id.temperature_mean_tview);
        temperature_min_val_tview = (TextView) findViewById(R.id.temperature_min_tview);
        temperature_max_val_tview = (TextView) findViewById(R.id.temperature_max_tview);

        btPowerOn = (Button) findViewById(R.id.PowerONBtn);
        btPowerOff = (Button) findViewById(R.id.PowerOFFBtn);
        btMotorOn = (Button) findViewById(R.id.MotorONBtn);
        btMotorOff = (Button) findViewById(R.id.MotorOFFBtn);

        btSendEvent = (Button) findViewById(R.id.send_event_button);
        btSendHourlyReport = (Button) findViewById(R.id.send_hourly_data_button);
        btSendDailyReport = (Button) findViewById(R.id.send_daily_data_button);

        btCalibrate = (Button) findViewById(R.id.calibrate_button);
        btToggleView = (Button) findViewById(R.id.toggle_display_button);
        btToggleMode = (Button) findViewById(R.id.toggle_manualmode_button);

        btDoDailyUpdate = (Button) findViewById(R.id.daily_update_button);
        btDoHourlyUpdate = (Button) findViewById(R.id.hourly_update_button);
    }

    //==========================================================================
    private void SetUXInteraction(boolean enabled)
    //==========================================================================
    {
        btCalibrate.setEnabled(enabled);
        btToggleView.setEnabled(enabled);
        btPowerOn.setEnabled(enabled);
        btPowerOff.setEnabled(enabled);
        btMotorOn.setEnabled(enabled);
        btMotorOff.setEnabled(enabled);
        btSendEvent.setEnabled(enabled);
        btSendHourlyReport.setEnabled(enabled);
        btSendDailyReport.setEnabled(enabled);

        btDoHourlyUpdate.setEnabled(enabled);
        btDoDailyUpdate.setEnabled(enabled);
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

    //==============================================================================================
    //==============================================================================================
    //  NETWORK OPERATIONS
    //==============================================================================================
    //==============================================================================================
    protected void start_network_listener() {
        try {
            myNetworkInfo = new it.dongnocchi.mariner.NetworkInfo();
            TelephonManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            TelephonManager.listen(myNetworkInfo, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        } catch (Exception ex) {
            LogException(TAG, "start_network_listener()", ex);
        }
    }


    //==========================================================================
    public void PowerIsON(View view)
    //==========================================================================
    {
        try {
            if (!myData.PowerON) {

                // TODO: verificare questa parte del WiFi
                // accendi wifi
                //myNetworkInfo.MyWiFiManager(getApplicationContext(), true);

                //Send Event to Azure Back End
                myEventManager.SendEventNew("POWER_ON", 0, "");

                //Calculate the actual time
                long new_power_on_time = System.nanoTime();

                //Issue a new Power On Event
                myData.AddPowerONEvent(new_power_on_time);

                sys_stat_textview.setText("POWER ON");

                // CREATE LOCAL FILES
                //CreateMyFile();

                //IsYoctoConnected(); // sets YoctoInUse if its connected
                int k = 0;
                YoctoInUse = false;
                while (!(IsYoctoConnected())) {
                    k++;
                    if (k == 4000) {
                        MaxiIO_textview.setText("Yocto not found");
                        break;
                    }
                }
                if (YoctoInUse) {
                    float time_to_talk_with_yocto_ms = ((float) (System.nanoTime() - new_power_on_time)) / 1000000.0f;
                    Start_Yocto();
                    MaxiIO_textview.setText("Yocto connected in: " + time_to_talk_with_yocto_ms + " ms");
                }
                //call_toast("k= " + k);

                //AccStartAcquiring();
                //GyroStartAcquiring();

                //Wheelchair_AppendData(Motor_ON_ID, false);

                //TODO: verificare che non sia il caso di invertire LowerScreenBrighteness e ResetScreenBrightness
                //LowerScreenBrightness();
                //ResetScreenBrightness();

                //NumOfWCActivation++;
                //isWheelchair_ON = true;
            }
        } catch (Exception ex) {
            LogException(TAG, "PowerIsON", ex);
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

                sys_stat_textview.setText("Power OFF");

                // STOP ACQUISITIONS
                if (YoctoInUse) {
                    Stop_Yocto();
                    MaxiIO_textview.setText("Yocto = stopped");
                } else
                    MaxiIO_textview.setText("Yocto = not present");

                //LowerScreenBrightness();

                StopInertialAcquisition();
            }
        } catch (Exception ex) {
            LogException(TAG, "PowerIsOFF", ex);
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
            sys_stat_textview.setText("Power OFF");
            Status = STATUS_SLEEP;
        } catch (Exception ex) {
            LogException(TAG, "Power_OFF_Click", ex);

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
            sys_stat_textview.setText("Power ON");
            Status = STATUS_IDLE;
        } catch (Exception ex) {
            LogException(TAG, "Power_ON_Click", ex);
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

    //==========================================================================
    private void StartPeriodicOperations() {
        //==========================================================================
        StartHourlyTimer();
        StartUXUpdateTimer(UXUPDATE_PERIOD_IN_MILLIS_SLOW);
    }

    //==========================================================================
    private void Stop_Periodic_Operations() {
        //==========================================================================
        CancelAlarm(OnceAnHour_alarmMgr, OnceAnHour_pintent);
        CancelAlarm(ViewRefreshUpdate_alarmMgr, ViewRefreshUpdate_pintent);
    }

    //==========================================================================
    private void StartHourlyTimer() {
        //==========================================================================
        // sets the receiver for once an hour alarm
        this.registerReceiver(OnceAnHour_Receiver, new IntentFilter("new hour"));
        OnceAnHour_pintent = PendingIntent.getBroadcast(this, 0, new Intent("new hour"), 0);
        OnceAnHour_alarmMgr = (AlarmManager) (this.getSystemService(Context.ALARM_SERVICE));
        //OnceAnHour_alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, MainCalendar.getTimeInMillis(), (600*1000) , OnceAnHour_pintent); // AlarmManager.INTERVAL_HOUR
        OnceAnHour_alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, GetNextHourInMillis(), AlarmManager.INTERVAL_HOUR, OnceAnHour_pintent); // AlarmManager.INTERVAL_HOUR
    }

    //==========================================================================
    private void StartUXUpdateTimer(long interval)
    //==========================================================================
    {
        //update the MainCalendar
        MainCalendar.setTimeInMillis(System.currentTimeMillis());

        // sets the receiver for once an hour alarm
        this.registerReceiver(ViewRefreshUpdate_Receiver, new IntentFilter("new second"));
        ViewRefreshUpdate_pintent = PendingIntent.getBroadcast(this, 0, new Intent("new second"), 0);
        ViewRefreshUpdate_alarmMgr = (AlarmManager) (this.getSystemService(Context.ALARM_SERVICE));
        //OnceAnHour_alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, MainCalendar.getTimeInMillis(), (600*1000) , OnceAnHour_pintent); // AlarmManager.INTERVAL_HOUR
        ViewRefreshUpdate_alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, GetActualTimeInMillis(), interval, ViewRefreshUpdate_pintent); // AlarmManager.INTERVAL_HOUR
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

            if (CalibrationMode) {
                AccAquiring = mSensorManager.registerListener(this, mAcc, ACC_READING_PERDIOD_CALIB, 10000);
                GyroAcquiring = mSensorManager.registerListener(this, mGyro, GYRO_READING_PERDIOD_CALIB, 10000);// 20.000 us ----> FsAMPLE = 50Hz
            } else {
                //TODO: verificare che 500 us sia il limite superiore per il jitter
                AccAquiring = mSensorManager.registerListener(this, mAcc, ACC_READING_PERDIOD, 5000);
                GyroAcquiring = mSensorManager.registerListener(this, mGyro, GYRO_READING_PERDIOD, 5000);// 20.000 us ----> FsAMPLE = 50Hz
            }
        } catch (Exception ex) {
            LogException(TAG, "StartInertialAcquisition", ex);
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
            AccAquiring = false;
            mSensorManager.unregisterListener(this, mAcc);

        }

        if (GyroAcquiring) {
            //super.onPause();
            GyroAcquiring = false;
            mSensorManager.unregisterListener(this, mGyro);
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

        //TODO: verificare che non si debba aspettare un poco...
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
    private void UpdateUX()
    //==========================================================================
    {
        try {

            if (UpdateTextViewsEnabled) {
                if (CalibrationMode) {
//                acc_x_calib.UpdateStats();
//                acc_y_calib.UpdateStats();
//                acc_z_calib.UpdateStats();

                    acc_x_1_tview.setText(String.format("%.3f", acc_x_calib.mean));
                    acc_y_1_tview.setText(String.format("%.3f", acc_y_calib.mean));
                    acc_z_1_tview.setText(String.format("%.3f", acc_z_calib.mean));

                    acc_x_2_tview.setText(String.format("%.4f", acc_x_calib.stdev));
                    acc_y_2_tview.setText(String.format("%.4f", acc_y_calib.stdev));
                    acc_z_2_tview.setText(String.format("%.4f", acc_z_calib.stdev));

                    gyro_x_1_tview.setText(String.format("%.3f", gyro_x_calib.mean));
                    gyro_y_1_tview.setText(String.format("%.3f", gyro_y_calib.mean));
                    gyro_z_1_tview.setText(String.format("%.3f", gyro_z_calib.mean));

                    gyro_x_2_tview.setText(String.format("%.4f", gyro_x_calib.stdev));
                    gyro_y_2_tview.setText(String.format("%.4f", gyro_y_calib.stdev));
                    gyro_z_2_tview.setText(String.format("%.4f", gyro_z_calib.stdev));

                } else {

                    acc_x_1_tview.setText(String.format("%.3f", myData.myInertialData.m_acc_x));
                    acc_y_1_tview.setText(String.format("%.3f", myData.myInertialData.m_acc_y));
                    acc_z_1_tview.setText(String.format("%.3f", myData.myInertialData.m_acc_z));

                    gyro_x_1_tview.setText(String.format("%.3f", myData.myInertialData.last_gyro_x));
                    gyro_y_1_tview.setText(String.format("%.3f", myData.myInertialData.last_gyro_y));
                    gyro_z_1_tview.setText(String.format("%.3f", myData.myInertialData.last_gyro_z));

                }

                acc_x_3_tview.setText(String.format("%.3f", myData.myInertialData.acc_offset[0]));
                acc_y_3_tview.setText(String.format("%.3f", myData.myInertialData.acc_offset[1]));
                acc_z_3_tview.setText(String.format("%.3f", myData.myInertialData.acc_offset[2]));

                gyro_x_3_tview.setText(String.format("%.3f", myData.myInertialData.gyro_offset[0]));
                gyro_y_3_tview.setText(String.format("%.3f", myData.myInertialData.gyro_offset[1]));
                gyro_z_3_tview.setText(String.format("%.3f", myData.myInertialData.gyro_offset[2]));

                acc_vel_x_tview.setText(String.format("%.3f", myData.myInertialData.velocity_x));

                acc_distance_x_tview.setText(String.format("%.3f", myData.myInertialData.HourlyDistanceCovered));

                acc_period_mean_tview.setText(String.format("%.3f", acc_x_calib.mean_deltatime));
                acc_period_stdev_tview.setText(String.format("%.3f", acc_x_calib.stdev_deltatime));

                gyro_period_mean_tview.setText(String.format("%.3f", gyro_x_calib.mean_deltatime));
                gyro_period_stdev_tview.setText(String.format("%.3f", gyro_x_calib.stdev_deltatime));

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
            }

            if (myData.PowerON)
                btPowerOn.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
            else
                btPowerOn.getBackground().setColorFilter(Color.DKGRAY, PorterDuff.Mode.MULTIPLY);

            if (myData.MotorON)
                btMotorOn.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
            else
                btMotorOn.getBackground().setColorFilter(Color.DKGRAY, PorterDuff.Mode.MULTIPLY);

            //app_uptime_tview.setText(AppUptimeString);

            //signal_level_tview.setText(String.valueOf());
            //updatetview_counter = 0;
            //}
        } catch (Exception ex) {
            LogException(TAG, "UpdateUX", ex);
        }

    }

    //==============================================================================================
    //==============================================================================================
    //  Yocto Methods
    //==============================================================================================
    //==============================================================================================

    //==========================================================================
    protected void Start_Yocto()
    //==========================================================================
    {
        // Connect to Yoctopuce Maxi-IO
        try {
            YAPI.EnableUSBHost(getApplicationContext());
            YAPI.RegisterHub("usb");

            tmp = YModule.FirstModule();
            while (tmp != null) {
                if (tmp.get_productName().equals("Yocto-Maxi-IO")) {

                    MaxiIO_SerialN = tmp.get_serialNumber();
                    MaxiIO = FindDigitalIO(MaxiIO_SerialN);
                    if (MaxiIO.isOnline()) {
                        //call_toast("Maxi-IO connected");
                        Init_Yocto(MaxiIO);
                        MaxiIO.registerValueCallback(this);
                        YAPI.HandleEvents();
                    }
                }
                //else {
                //call_toast("MAXI-IO NOT CONNECTED");
                //}
                tmp = tmp.nextModule();
            }
            r.run();
        } catch (YAPI_Exception ex) {
            LogException(TAG, "Start_Yocto", ex);
        }

        //Faccio partire tra un secondo il runnable di rilevamento del dato dal sensore
        handler.postDelayed(r, 1000);
    }

    //==========================================================================
    protected void Init_Yocto(YDigitalIO moduleName)
    //==========================================================================
    {        // set the port as input
        try {
            moduleName.set_portDirection(0x0F);             //bit 0-3: OUT; bit 4-7: IN ( bit set to 0)
            moduleName.set_portPolarity(0);                 // polarity set to regular
            moduleName.set_portOpenDrain(0);                // No open drain
            //moduleName.set_portState(0x00);                 // imposta valori logici di uscita inizialmente tutti bassi
        } catch (YAPI_Exception e) {
            LogException(TAG, "Init_Yocto", e);
        }
    }

    //==========================================================================
    protected void Stop_Yocto()
    //==========================================================================
    {
        YAPI.FreeAPI();
        handler.removeCallbacks(r);
    }

    // NEW VALUE ON PORT:
    @Override
    //==========================================================================
    public void yNewValue(YDigitalIO yDigitalIO, String s)
    //==========================================================================
    {
        long new_event_time = System.nanoTime();
        event_textview.setText(s);

        try {
            // CHECK MOTOR PIN VALUE
            //Motor_OldInputData = Motor_NewInputData;
            Motor_NewInputData = MaxiIO.get_bitState(MaxiIO_MotorPin);

            // MOTOR EVENT HANDLING
            if (Motor_NewInputData == 1) {
                myData.AddMotorONEvent(new_event_time);
                StartInertialAcquisition();
            }

            if (Motor_NewInputData == 0) {
                StopInertialAcquisition();
                myData.AddMotorOFFEvent(new_event_time);
            }

        } catch (YAPI_Exception e) {
            LogException(TAG, "yNewValue", e);
        }
    }

    //==========================================================================
    public boolean IsYoctoConnected()
    //==========================================================================
    {
        try {
            YAPI.EnableUSBHost(getApplicationContext());
            YAPI.RegisterHub("usb");

            tmp = YModule.FirstModule();
            while (tmp != null) {
                if (tmp.get_productName().equals("Yocto-Maxi-IO")) {

                    MaxiIO_SerialN = tmp.get_serialNumber();
                    MaxiIO = FindDigitalIO(MaxiIO_SerialN);

                    if (MaxiIO.isOnline()) {
                        YoctoInUse = true;
                        MaxiIO_textview.setText("MaxiIO connected: YES");
                    } else {
                        YoctoInUse = false;
                        MaxiIO_textview.setText("MaxiIO connected: NO");
                    }
                } else {
                    YoctoInUse = false;
                }
                tmp = tmp.nextModule();
            }
        } catch (YAPI_Exception e) {
            LogException(TAG, "IsYoctoConnected", e);
        }

        //lastfiles.isyoctoinuse = YoctoInUse;
        return YoctoInUse;
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
        long delta_time = (System.nanoTime() - CalibrationStartTime) / 1000000000;

        return (delta_time > NUM_OF_SECONDS_CALIBRATION);
    }

    //==========================================================================
    public void StartCalibration() {
        //==========================================================================
        //int CountDown =  NUM_OF_SECONDS_CALIBRATION * 5;

        CalibrationStartTime = System.nanoTime();//Calendar.getInstance().getTime().getTime();

        //Step 0. Clean the needed data structures and set flags
        acc_x_calib.reset_data();
        acc_y_calib.reset_data();
        acc_z_calib.reset_data();

        gyro_x_calib.reset_data();
        gyro_y_calib.reset_data();
        gyro_z_calib.reset_data();

        CalibrationMode = true; //isCalibrating = true;

        //Step 1. Activate the sensors
        StartInertialAcquisition();
    }

    //==========================================================================
    public void StopCalibrateInertialSensors()
    //==========================================================================
    {
        try {

            StopInertialAcquisition();
            TimeUnit.MILLISECONDS.sleep(200);
            //Thread.sleep(200);//Sleep(100);

            CalibrationMode = false;

            acc_x_calib.UpdateStats();
            acc_y_calib.UpdateStats();
            acc_z_calib.UpdateStats();

            gyro_x_calib.UpdateStats();
            gyro_y_calib.UpdateStats();
            gyro_z_calib.UpdateStats();

            myData.myInertialData.UpdateBias(acc_x_calib.mean, acc_y_calib.mean, acc_z_calib.mean,
                    gyro_x_calib.mean, gyro_y_calib.mean, gyro_z_calib.mean);

        } catch (Exception ex) {
            LogException(TAG, "StopCalibrateInertialSensors", ex);
        }

    }

    //TODO: da cancellare ed eventualmente spostare il codice altrove

    //==========================================================================
    public void DoDailyReport(View view) {
        //==========================================================================
        // per debug
        //TODO: Verificare che questa sequenza di operazioni sia esaustiva rispetto a quello che ci interessa

        //upload_lastFiles();
        myAzureManager.UploadBlobs(myData.myBatteryData.level);
        // CHECK NEW APP UPDATES ==================================
        myAzureManager.CheckNewUpdates(myData.myBatteryData.level);

        myData.updateDailyUse();
        myEventManager.SendDailyReport();

        myData.ResetDailyCounters();

        // stop acquisitions and calibrate phone position with a 30sec acquisition
        //SwitchOffEverything ();
        //NumOfWCActivation_Current = NumOfWCActivation;

        CalibrationMode = true; //isCalibrating = true;
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        AccAquiring = mSensorManager.registerListener(this, mAcc, 50000);
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
        it.dongnocchi.mariner.LogFile_Handler BkgSave_LogHandler = new it.dongnocchi.mariner.LogFile_Handler(StringToSend);
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
    }


    //==========================================================================
    public void ToggleViewButton_Click(View view)
    //==========================================================================
    {
        UpdateTextViewsEnabled = !UpdateTextViewsEnabled;

        if (UpdateTextViewsEnabled) {
            CancelAlarm(ViewRefreshUpdate_alarmMgr, ViewRefreshUpdate_pintent);
            //Thread.sleep(100);
            StartUXUpdateTimer(UXUPDATE_PERIOD_IN_MILLIS_FAST);
        } else {
            CancelAlarm(ViewRefreshUpdate_alarmMgr, ViewRefreshUpdate_pintent);
            //Thread.sleep(100);
            StartUXUpdateTimer(UXUPDATE_PERIOD_IN_MILLIS_SLOW);
        }

    }


    //==========================================================================
    public void ToggleManualModeButton(View view)
    //==========================================================================
    {
        if (ManualMode) {
            SetUXInteraction(false);

            btToggleMode.setText("Manual Mode");
            btToggleMode.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);

            ManualMode = false;
        } else {
            SetUXInteraction(true);

            btToggleMode.setText("Auto Mode");
            btToggleMode.getBackground().setColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
            //setBackgroundColor(Color.LTGRAY);

            ManualMode = true;
        }
    }


    //==========================================================================
    public void SendHourlyDataButton_Click(View view)
    //==========================================================================
    {
        myData.updateHourlyUse(Hourly_Reference_Time, System.nanoTime());
        myEventManager.SendHourlyStatusEvent();
        ResetHourlyCounters();
    }

    //==========================================================================
    public void SendDailyDataButton_Click(View view)
    //==========================================================================
    {
        Stop_Periodic_Operations();

        StopAllAcquisitions();

        //Step 2 -

        SaveData();

        myData.updateDailyUse();

        myEventManager.SendDailyReport();

        //CalibrateAccelerometer();

        //UpdateListofFilesToUpload();

        //Upload Blobs
        myAzureManager.UploadBlobs(myData.myBatteryData.level);

        //Check for App updates
        myAzureManager.CheckNewUpdates(myData.myBatteryData.level);

        DailyResetData();

        StartCalibration();

        StartTemperatureAcquisition();

        StartPeriodicOperations();
    }

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
            LogException(TAG, "SendEventButton_Click", ex);
        }

    }

    //==========================================================================
    private void SaveData()
    //==========================================================================
    {

        DataOutputStream data_out;
        String filename;
        //String XMLFileList

        //Step 1 - Save Calibration file data
        try {
            filename = myConfig.get_Acquisition_Folder() + CommonFilePreamble + "-calib.bin";

            data_out = getDataOutputStream(filename);

            data_out.writeFloat(myData.myInertialData.acc_offset[0]);
            data_out.writeFloat(myData.myInertialData.acc_offset[1]);
            data_out.writeFloat(myData.myInertialData.acc_offset[2]);

            data_out.writeFloat(myData.myInertialData.gyro_offset[0]);
            data_out.writeFloat(myData.myInertialData.gyro_offset[1]);
            data_out.writeFloat(myData.myInertialData.gyro_offset[2]);

            data_out.flush();
            data_out.close();

            //myAzureManager.AddFileToSaveList(myConfig.getAcquisitionsFolder(), filename, myConfig.get_Acquisition_Container());
            myAzureManager.AddFileToSaveList(filename);

            //Step 2 - Save Accelerometer data if available
            if (myData.myInertialData.acc_data_counter > 0) {

                filename = myConfig.get_Acquisition_Folder() + CommonFilePreamble + "-acc.bin";
                data_out = getDataOutputStream(filename);

                for (int i = 0; i < myData.myInertialData.acc_data_counter; i++) {
                    data_out.write(myData.myInertialData.AccTimestampArray[i]);
                    data_out.writeFloat(myData.myInertialData.AccXDataArray[i]);
                    data_out.writeFloat(myData.myInertialData.AccYDataArray[i]);
                    data_out.writeFloat(myData.myInertialData.AccZDataArray[i]);
                }

                data_out.flush();
                data_out.close();

                //myAzureManager.AddFileToSaveList(myConfig.getAcquisitionsFolder(), filename, myConfig.get_Acquisition_Container());
                myAzureManager.AddFileToSaveList(filename);
            }

            //Step 3 - Save Gyro data if available
            if (myData.myInertialData.gyro_data_counter > 0) {
                filename = myConfig.get_Acquisition_Folder() + CommonFilePreamble + "-gyro.bin";
                data_out = getDataOutputStream(filename);

                for (int i = 0; i < myData.myInertialData.gyro_data_counter; i++) {
                    data_out.write(myData.myInertialData.GyroTimestampArray[i]);
                    data_out.writeFloat(myData.myInertialData.GyroXDataArray[i]);
                    data_out.writeFloat(myData.myInertialData.GyroYDataArray[i]);
                    data_out.writeFloat(myData.myInertialData.GyroZDataArray[i]);
                }

                data_out.flush();
                data_out.close();

                //myAzureManager.AddFileToSaveList(myConfig.getAcquisitionsFolder(), filename, myConfig.get_Acquisition_Container());
                myAzureManager.AddFileToSaveList(filename);
            }

            //Step 4 - Save Temperature data
            if (myData.myTempData.data_counter > 0) {
                filename = myConfig.get_Acquisition_Folder() + CommonFilePreamble + "-temp.bin";
                data_out = getDataOutputStream(filename);

                for (int i = 0; i < myData.myTempData.data_counter; i++) {
                    data_out.write(myData.myTempData.TimestampArray[i]);
                    data_out.writeFloat(myData.myTempData.DataArray[i]);
                }

                data_out.flush();
                data_out.close();

                //myAzureManager.AddFileToSaveList(myConfig.getAcquisitionsFolder(), filename, myConfig.get_Acquisition_Container());
                myAzureManager.AddFileToSaveList(filename);
            }
            //Step 5 - Save Event info
            if (myData.myEventData.data_counter > 0) {
                filename = myConfig.get_Acquisition_Folder() + CommonFilePreamble + "-event.bin";
                data_out = getDataOutputStream(filename);

                for (int i = 0; i < myData.myEventData.data_counter; i++) {
                    data_out.write(myData.myEventData.Timestamps[i]);
                    data_out.write(myData.myEventData.EventArray[i]);
                }

                data_out.flush();
                data_out.close();

                //myAzureManager.AddFileToSaveList(myConfig.getAcquisitionsFolder(), filename, myConfig.get_Acquisition_Container());
                myAzureManager.AddFileToSaveList(filename);
            }


            //Step 7 - Save Battery Data
            filename = myConfig.get_Acquisition_Folder() + CommonFilePreamble + "-bat.bin";
            data_out = getDataOutputStream(filename);

            if (myData.myBatteryData.data_counter > 0) {

                for (int i = 0; i < myData.myBatteryData.data_counter; i++) {
                    data_out.write(myData.myBatteryData.Timestamps[i]);
                    data_out.write(myData.myBatteryData.Values[i]);
                }

                data_out.flush();
                data_out.close();

                myAzureManager.AddFileToSaveList(filename);
            }

            //Step 8 - Save the list of files saved
            if (myAzureManager.FilesToSend.size() > 0 )
            {
                filename = myConfig.get_Acquisition_Folder() + CommonFilePreamble + "-filelist.txt";
                data_out = getDataOutputStream(filename);

                for (int i = 0; i < myAzureManager.FilesToSend.size(); i++ )
                {
                    data_out.writeBytes(myAzureManager.FilesToSend.get(i) + System.getProperty("line.separator"));
                }

                myAzureManager.AddFileToSaveList(filename);

                data_out.flush();
                data_out.close();
            }

        } catch (Exception ex) {
            LogException(TAG, "SaveData", ex);
        }
    }

    //==========================================================================
    private void Sleep(int ms_to_sleep)
    //==========================================================================
    {
        try {
            Thread.sleep(ms_to_sleep);
        } catch (Exception ex) {
            LogException(TAG, "Sleep", ex);
        }
    }

    //==========================================================================
    public void UpdateRunningTime()
    //==========================================================================
    {
        long LastMeasuredTime = System.nanoTime();//Calendar.getInstance().getTime().getTime();

        long mills = (LastMeasuredTime - Daily_Reference_Time) / 1000000;

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
    public long GetNextHourInMillis() {
        //==========================================================================
        MainCalendar.setTimeInMillis(System.currentTimeMillis());
        MainCalendar.set(Calendar.MINUTE, 0);
        MainCalendar.set(Calendar.SECOND, 0);
        MainCalendar.set(Calendar.MILLISECOND, 0);
        MainCalendar.add(Calendar.HOUR, 1);

        return MainCalendar.getTimeInMillis();
    }

    //==========================================================================
    public int GetActualHourOfDay()
    //==========================================================================
    {
        MainCalendar.setTimeInMillis(System.currentTimeMillis());
        return MainCalendar.get(Calendar.HOUR_OF_DAY);
    }

    //==========================================================================
    private void SetDailyReferenceTimeAndDate()
    //==========================================================================
    {
        Daily_Reference_Time = System.nanoTime();
        Daily_Reference_Date = new Date();
    }

    //TODO: da verificare se serva ancora
    //==========================================================================
    private void CloseSaveAndRestartLogger(boolean initializing)
    //==========================================================================
    {
        //CLose the existing Logger (if any)
        if (!initializing) {
            FileLog.close();

            //Save logfile
            myAzureManager.AddFileToSaveList(myConfig.getAcquisitionsFolder(), logger_filename, myConfig.get_Acquisition_Container());
        }

        CreateAndOpenNewFileLogger();
    }

    //==========================================================================
    private void CreateAndOpenNewFileLogger()
    //==========================================================================
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        CommonFilePreamble = myConfig.WheelchairID + "-" + formatter.format(Daily_Reference_Date);
        logger_filename = myConfig.get_Acquisition_Folder() + CommonFilePreamble + ".log";

        //Open the new one for the new day
        FileLog.open(logger_filename, Log.VERBOSE, MAX_LOGFILE_SIZE);
    }


    //==========================================================================
    private void LogException(String tag, String msg, Exception ex)
    //==========================================================================
    {

        //ex.printStackTrace();
        //SaveErrorLog(ex.toString());
        Log.e(tag, msg, ex);
        FileLog.e(tag, msg, ex);
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
        records = notSent.ReadTheWholeFile();

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
            notSent.DeleteLine(records_toBeDeleted.get(i));
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


} // fine della MainActivity