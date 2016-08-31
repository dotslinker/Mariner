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

    //TODO: da verificare tutta la politica di Logging
    public static final String TAG = MainActivity.class.getSimpleName();

    private SensorManager mSensorManager;
    private Sensor mAcc;
    private Sensor mGyro;
    private Sensor mTemperature;

    private BatteryManager myBatteryManager;

    private boolean ManualMode = false;

    boolean AccAquiring = false;
    private static final int ACC_READING_PERDIOD = 20000; // 20 ms
    boolean GyroAcquiring = false;
    private static final int GYRO_READING_PERDIOD = 20000; //20 ms
    boolean TemperatureAcquiring = false;
    private static final int TEMPERATURE_READING_PERDIOD = 10000000; //10s


    public static final int STATUS_INIT = 0;
    public static final int STATUS_SLEEP = 1;
    public static final int STATUS_IDLE = 2;
    public static final int STATUS_ACTIVE = 3;
    public static final int STATUS_DAILY_UPDATE = 4;
    public static final int STATUS_OFFLINE = 5;

    public static final String[] STATUS_STRING = {"INIT", "SLEEP", "IDLE", "ACTIVE", "DAILY_UPDATE", "OFFLINE"};

    //During the Calibration, we require a less frequent sampling period
    private static final int ACC_READING_PERDIOD_CALIB = 50000;
    private static final int GYRO_READING_PERDIOD_CALIB = 50000;

    //TODO: parametrizzare il valore del tempo massimo prima della notifica del onSensorChanged

    //boolean isWheelchair_ON = false;

    private static final int NUM_OF_TEMPERARURE_SAMPLES = 8650; //8640 sarebbe il numero corretto
    private static final int NUM_OF_SIGNAL_STRENGTH_SAMPLES = 8650; //8640 sarebbe il numero corretto

    // variables used for the 30 sec acquisition for calibration
    boolean CalibrationMode = false;

    private static final int NUM_OF_SECONDS_CALIBRATION = 10;

    private static final int CALIB_DATA_SIZE = 600; // 500 per 10 secondi

    //TODO: verificare che sia utile
    private long LastMeasuredTime;

    //private long ApplicationStartTime;
    private String AppUptimeString, DutyUptimeString;

    //Reference date reporting the reset performed at the very first start
    //and at the reset each night. Needs to be evaluated very close to Daily_referece_time
    static Date Daily_Reference_Date;

    //daily Reference time to be used for timestamping the acquisitions
    static long Daily_Reference_Time;

    // App starting date - this is unique until the app restarts
    static Date App_Start_Date;

    // App starting time
    static long AppStartTime;

    private long CalibrationStartTime;
    private long AcquisitionStartTime;

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
    TextView temperature_min_val_tview;
    TextView temperature_max_val_tview;
    TextView temperature_mean_val_tview;

    TextView signal_level_tview;
    TextView temp_val_tview;

    TextView app_uptime_tview, duty_uptime_tview;

    Button btPowerOn, btPowerOff, btMotorOn, btMotorOff;
    Button btSendEvent, btSendDailyReport, btSendHourlyReport;
    Button btCalibrate, btToggleView, btToggleMode;
    Button btDoHourlyUpdate, btDoDailyUpdate;

    private String CommonFilePreamble;

    // INDICATE WHEN YOCTO IS IN USE (AVAILABLE)
    private boolean YoctoInUse = false;

    private int Status;

    //Array containing the information about the running time of the App
    int [] RunningTime;

    // CLASSES FOR COMMUNICATIONS BETWEEN ACTIVITIES
    //User user;              //input to this class
    it.dongnocchi.mariner.LastFiles lastfiles;    //output

    // phone network variables
    TelephonyManager TelephonManager;
    it.dongnocchi.mariner.NetworkInfo myNetworkInfo;
    //int SignalStrength = 0;

    AzureManager myBlobManager;
    Configuration myConfig;
    AzureEventManager myEventManager;

    int actual_hour_measured;
    BroadcastReceiver OnceAnHour_Receiver;
    Calendar calendar;
    PendingIntent OnceAnHour_pintent;
    AlarmManager OnceAnHour_alarmMgr;

    BroadcastReceiver ViewRefreshUpdate_Receiver;
    PendingIntent ViewRefreshUpdate_pintent;
    AlarmManager ViewRefreshUpdate_alarmMgr;
    //private int ViewRefreshUpdate_period_ms = 1000;

    it.dongnocchi.mariner.NotSentFileHandler notSent;

    WindowManager.LayoutParams NewLayoutParams = null;

    it.dongnocchi.mariner.WheelchairData myData;

    FileLog myLogger;
    static final int MAX_LOGFILE_SIZE = 20000000;

    @Override
    //==========================================================================
    protected void onCreate(Bundle savedInstanceState) {
        //==========================================================================

        try {
            AppStartTime = System.nanoTime();
            App_Start_Date = new Date();

            RunningTime = new int[5]; //0 = days, 1 = hours, 2 = mins, 3 = sec, 4 = ms

            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            InitUX();

            lastfiles = new it.dongnocchi.mariner.LastFiles();                              // SET OUTPUT TO INIT ACTIVITY

            // INITIALISE SENSOR MANAGER
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

            mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

            myBatteryManager = new BatteryManager();

            // REGISTER BROADCAST RECEIVER FOR BATTERY EVENTS
            registerReceiver(mBatChargeOff, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
            registerReceiver(mBatChargeOn, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
            //registerReceiver(mBatLow, new IntentFilter(Intent.ACTION_BATTERY_LOW));
            //registerReceiver(mBatOkay, new IntentFilter(Intent.ACTION_BATTERY_OKAY));
            registerReceiver(mBatChanged, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            myConfig = new Configuration();
            myData = new it.dongnocchi.mariner.WheelchairData();

            acc_x_calib = new TimestampedDataArray(CALIB_DATA_SIZE);
            acc_y_calib = new TimestampedDataArray(CALIB_DATA_SIZE);
            acc_z_calib = new TimestampedDataArray(CALIB_DATA_SIZE);

            gyro_x_calib = new TimestampedDataArray(CALIB_DATA_SIZE);
            gyro_y_calib = new TimestampedDataArray(CALIB_DATA_SIZE);
            gyro_z_calib = new TimestampedDataArray(CALIB_DATA_SIZE);

            myBlobManager = new AzureManager(getApplicationContext(), new it.dongnocchi.mariner.AsyncResponse() {
                @Override
                public void processFinish(String last_uploaded_file) {
                    // se ho caricato il file xml coi nomi dei file caricati, non scriverlo sul nuovo file xml
                    String xml_name = myConfig.get_UploadedFiles_XmlName();
                    if (last_uploaded_file != xml_name) { // l'ultimo file caricato è un file di dati
                        myBlobManager.AppendNew_UploadedFileName(last_uploaded_file);//lo aggiungo al file contenente i nomi dei file caricati
                    }
                }
            }, myConfig);

            //Verifica se è presente l'alimentazione, ed in questo caso aggiunge
            //un evento fittizio di PowerON
            if (isSupplyPowerPresent()) {
                NotifyPowerON();
            }

            StartCalibrateInertialSensors();

            TemperatureStartAcquiring();

            StartPeriodicOperations();

            start_network_listener();

            SetUXInteraction(false);

            // inizializzazione eventhub manager
            myEventManager = new AzureEventManager(getApplicationContext(), new it.dongnocchi.mariner.AsyncResponse() {
                @Override
                public void processFinish(String output) {
                }
            }, myConfig, myData);

            notSent = new it.dongnocchi.mariner.NotSentFileHandler(myConfig.get_Wheelchair_path());

            myLogger = new FileLog();

            DailyResetData();

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
            Log.d(TAG, "onCreate completed");

        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, "onCreate", ex);
        }
    }



    @Override
    protected void onRestart() {
        super.onRestart();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }



    private void Sleep(int ms_to_sleep)
    {
        try{
            Thread.sleep(ms_to_sleep);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            Log.e(TAG, "Sleep", ex);
        }

    }

    public void UpdateRunningTime()
    {

        LastMeasuredTime = System.nanoTime();//Calendar.getInstance().getTime().getTime();

        long mills = (LastMeasuredTime - Daily_Reference_Time) / 1000000;

        int Hours = (int) (mills / (1000 * 60 * 60));
        int Mins = (int) (mills / (1000 * 60)) % 60;
        int Secs = (int)( mills - (long)Hours * (1000 * 60 * 60) - (long)(Mins) * (1000 * 60))/1000;
        int ms = 0;

        RunningTime[0] = Hours / 24;
        RunningTime[1] = Hours % 24;
        RunningTime[2] = Mins;
        RunningTime[3] = Secs;

        //AppUptimeString = String.format("%2d:%2d:%2d", Hours, Mins, Secs);
    }


    private void DailyResetData()
    {
        //TODO: implementare il reset di tutte le strutture dati utilizzate
        Daily_Reference_Time = System.nanoTime();
        Daily_Reference_Date = new Date();

        myData.DailyReset(Daily_Reference_Time);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        CommonFilePreamble = formatter.format(Daily_Reference_Date);

        String new_logger_filename = myConfig.get_Acquisition_Folder() + CommonFilePreamble + ".log";

        //CLose the existing Logger (if any)
        FileLog.close();

        //Step 8 - Save logfile
        myBlobManager.AddFileToList(myConfig.getAcquisitionsFolder(), myLogger.getFileName(), myConfig.get_Acquisition_Container());

        //Open the new one for the new day
        FileLog.open(new_logger_filename, Log.ASSERT, MAX_LOGFILE_SIZE);
    }

    private void NotifyPowerON() {
        myData.AddPowerONEvent(AppStartTime);


    }

    private void NotifyPowerOFF() {
        myData.AddPowerOFFEvent(AppStartTime);


    }





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

        app_uptime_tview = (TextView) findViewById(R.id.app_uptime_tview);
        duty_uptime_tview = (TextView) findViewById(R.id.current_duty_uptime_tview);

        signal_level_tview = (TextView) findViewById(R.id.signal_level_val_tview);
        //temp_val_tview = (TextView) findViewById(R.id.temperature_val_tview);

        //temperature_tview = (TextView) findViewById(R.id.temperature_tview);
        temperature_mean_val_tview = (TextView) findViewById(R.id.temperature_mean_tview);
        temperature_min_val_tview = (TextView) findViewById(R.id.temperature_min_tview);
        temperature_max_val_tview = (TextView) findViewById(R.id.temperature_max_tview);

        btPowerOn = (Button) findViewById(R.id.PowerONBtn);
        btPowerOff = (Button) findViewById(R.id.PowerOFFBtn);
        btMotorOn = (Button) findViewById(R.id.MotorONBtn);
        btMotorOff = (Button) findViewById(R.id.MotorOFFBtn);

        btSendEvent= (Button) findViewById(R.id.send_event_button);
        btSendHourlyReport= (Button) findViewById(R.id.send_hourly_data_button);
        btSendDailyReport= (Button) findViewById(R.id.send_daily_data_button);

        btCalibrate = (Button) findViewById(R.id.calibrate_button);
        btToggleView = (Button) findViewById(R.id.toggle_display_button);
        btToggleMode = (Button) findViewById(R.id.toggle_manualmode_button);

        btDoDailyUpdate = (Button) findViewById(R.id.daily_update_button);
        btDoHourlyUpdate = (Button) findViewById(R.id.hourly_update_button);
    }

    //==============================================================================================
    //==============================================================================================
    //  CHARGE CONTROL
    //==============================================================================================
    //==============================================================================================
    public boolean isSupplyPowerPresent() {
        boolean to_return = false;

        //Context context = getApplicationContext();
        Intent intent = this.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

        to_return = (plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB);

        return to_return;
    }

    //==========================================================================
    private BroadcastReceiver mBatChargeOn = new BroadcastReceiver() {
        @Override
        //When Event is published, onReceive method is called
        public void onReceive(Context c, Intent i) {
            SetPower_ON(null);           // START ALL
        }
    };
    //==========================================================================
    private BroadcastReceiver mBatChargeOff = new BroadcastReceiver() {
        @Override
        //When Event is published, onReceive method is called
        public void onReceive(Context c, Intent i) {
            SetPower_OFF(null);
        }
    };
    //==========================================================================
    private BroadcastReceiver mBatChanged = new BroadcastReceiver() {
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
            ex.printStackTrace();
            Log.e(TAG, "start_network_listener()", ex);
        }
    }
    //==============================================================================================
    //==============================================================================================
    //  MOTOR AND POWER EVENTS HANDLING
    //==============================================================================================
    //==============================================================================================

    //==========================================================================
    public void SetPower_ON(View view) {
        //==========================================================================
        //invia evento su eventhub che segnala accensione carrozzina

        try {
            if (!myData.PowerON) {

                // TODO: verificare questa parte del WiFi
                // accendi wifi
                myNetworkInfo.MyWiFiManager(getApplicationContext(), true);

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
                LowerScreenBrightness();

                //NumOfWCActivation++;
                //isWheelchair_ON = true;
            }
        }
            catch(Exception ex)
            {
                ex.printStackTrace();
                Log.e(TAG, "SetPower_ON", ex);
            }
    }

    //==========================================================================
    public void SetPower_OFF(View view) {
        //==========================================================================
        try{
            if (myData.PowerON) {
                ResetScreenBrightness();
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

                //StopInertialAcquisition();
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            Log.e(TAG, "SetPower_OFF", ex);
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
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            Log.e(TAG, "Power_OFF_Click", ex);

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
            ex.printStackTrace();
            Log.e(TAG, "Power_ON_Click", ex);
        }
    }



    //TODO: verificare se sia da tenere o meno...

    //==========================================================================
    protected void SwitchOffEverything() {
        //==========================================================================
        // qui wheelchair off + alarm off
        SetPower_OFF(null);
        CancelAlarm(OnceAnHour_alarmMgr, OnceAnHour_pintent);
    }

    //==========================================================================
    public void SetMotor_ON(View view) {
        //==========================================================================
        btMotorOn.setBackgroundColor(Color.GREEN);
        btMotorOff.setEnabled(true);

        StartInertialAcquisition();
    }

    //==========================================================================
    public void SetMotor_OFF(View view) {
        //==========================================================================
        btMotorOn.setBackgroundColor(Color.GRAY);
        btMotorOff.setEnabled(false);

        StopInertialAcquisition();
    }

    //==========================================================================
    private void StartPeriodicOperations() {
        //==========================================================================
        // this sets the system that calls WhatToDoAt2InTheNight at 2 in the night
        // and sends an event for each hour
        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        OnceAnHour_Receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                //SimpleDateFormat formatter = new SimpleDateFormat("HH_mm_ss");
                //Date now = new Date();
                //stringafake = formatter.format(now);
                //event_textview.setText(stringafake);

                //SendEvent_SystemStatus();
                //SendHourlyStatusEvent();

                //NumOfSecWorking_LastHour = 0;

                Calendar rightNow = Calendar.getInstance();
                actual_hour_measured = rightNow.get(Calendar.HOUR_OF_DAY); // gets hour in 24h format

                if (actual_hour_measured == 2) { // do this only once a day at 2 in the night

                    SaveData();

                    //CalibrateAccelerometer();

                    //UpdateListofFilesToUpload();

                    upload_lastFiles();

                    //TODO: Verificare che vada bene rimuovere le linee commentate qui sotto
                    //CreateMyWheelchairFile();
                    //WhatToDoAt2InTheNight(null); // STOP INERTIAL ACQUISITION AND CALIBRATE SMARTPHONE POSITION

//                    } else if(actual_hour == 3){
//                        //spegni la rete wifi
//                        myNetworkInfo.MyWiFiManager(getApplicationContext(), false);
                }
            }
        };

        StartHourlyTimer();

        ViewRefreshUpdate_Receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Aggiorno i dati presenti sullo schermo

                myData.SignalStrength = myNetworkInfo.getSignalStrength();

                UpdateUX();


            }
        };

        StartUXUpdateTimer();

    }

    private void Stop_Periodic_Operations()
    {
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
        //OnceAnHour_alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), (600*1000) , OnceAnHour_pintent); // AlarmManager.INTERVAL_HOUR
        OnceAnHour_alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_FIFTEEN_MINUTES, OnceAnHour_pintent); // AlarmManager.INTERVAL_HOUR
    }

    //==========================================================================
    private void StartUXUpdateTimer() {
        //==========================================================================
        // sets the receiver for once an hour alarm
        this.registerReceiver(ViewRefreshUpdate_Receiver, new IntentFilter("new second"));
        ViewRefreshUpdate_pintent = PendingIntent.getBroadcast(this, 0, new Intent("new second"), 0);
        ViewRefreshUpdate_alarmMgr = (AlarmManager) (this.getSystemService(Context.ALARM_SERVICE));
        //OnceAnHour_alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), (600*1000) , OnceAnHour_pintent); // AlarmManager.INTERVAL_HOUR
        ViewRefreshUpdate_alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 200, ViewRefreshUpdate_pintent); // AlarmManager.INTERVAL_HOUR
    }

    //==========================================================================
    private void CancelAlarm(AlarmManager Alarm_StopToFire, PendingIntent PendingIntentToStop) {
        //==========================================================================
        if (Alarm_StopToFire != null) {
            Alarm_StopToFire.cancel(PendingIntentToStop);
        }
    }




    @Override
    //==========================================================================
    public void onSensorChanged(SensorEvent event) {
        //==========================================================================
        // APPEND INERTIAL SENSORS DATA

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
else
                    {
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
                    else{
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
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            Log.d(TAG, "onSensorChanged", ex);
        }

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void StartInertialAcquisition() {
        //        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //        mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        try {

            if(CalibrationMode)
            {
                AccAquiring = mSensorManager.registerListener(this, mAcc, ACC_READING_PERDIOD_CALIB, 10000);
                GyroAcquiring = mSensorManager.registerListener(this, mGyro, GYRO_READING_PERDIOD_CALIB, 10000);// 20.000 us ----> FsAMPLE = 50Hz
            }
            else
            {
                //TODO: verificare che 500 us sia il limite superiore per il jitter
                AccAquiring = mSensorManager.registerListener(this, mAcc, ACC_READING_PERDIOD, 5000);
                GyroAcquiring = mSensorManager.registerListener(this, mGyro, GYRO_READING_PERDIOD, 5000);// 20.000 us ----> FsAMPLE = 50Hz
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            //TODO: verificare cos'è sto saveerrorlog
            SaveErrorLog(ex.toString());
            Log.d(TAG, "StartInertialAcquisition", ex);
        }
        //AccStartAcquiring();
        //GyroStartAcquiring();
    }

    private void StopInertialAcquisition() {

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
    protected void TemperatureStartAcquiring() {
        //==========================================================================
        //super.onResume();
        TemperatureAcquiring = mSensorManager.registerListener(this, mTemperature, TEMPERATURE_READING_PERDIOD, 10000);// 20.000 us ----> FsAMPLE = 50Hz
    }


    //==========================================================================
    protected void TemperatureStopAcquiring() {
        //==========================================================================
        if (TemperatureAcquiring) {
            //super.onPause();
            mSensorManager.unregisterListener(this, mTemperature);
            TemperatureAcquiring = false;
        }
    }

    //==========================================================================
    private void UpdateUX() {
        //==========================================================================
        try {
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

                acc_x_1_tview.setText(String.valueOf(myData.myInertialData.m_acc_x));
                acc_y_1_tview.setText(String.valueOf(myData.myInertialData.m_acc_y));
                acc_z_1_tview.setText(String.valueOf(myData.myInertialData.m_acc_z));
            }

            acc_x_3_tview.setText(String.format("%.3f", myData.myInertialData.acc_offset[0]));
            acc_y_3_tview.setText(String.format("%.3f", myData.myInertialData.acc_offset[1]));
            acc_z_3_tview.setText(String.format("%.3f", myData.myInertialData.acc_offset[2]));

            gyro_x_3_tview.setText(String.format("%.3f", myData.myInertialData.gyro_offset[0]));
            gyro_y_3_tview.setText(String.format("%.3f", myData.myInertialData.gyro_offset[1]));
            gyro_z_3_tview.setText(String.format("%.3f", myData.myInertialData.gyro_offset[2]));

            acc_vel_x_tview.setText(String.valueOf(myData.myInertialData.velocity_x));

            acc_distance_x_tview.setText(String.valueOf(myData.myInertialData.HourlyDistanceCovered));

            acc_period_mean_tview.setText(String.format("%.3f",acc_x_calib.mean_deltatime));
            acc_period_stdev_tview.setText(String.format("%.3f",acc_x_calib.stdev_deltatime));

            gyro_period_mean_tview.setText(String.format("%.3f",gyro_x_calib.mean_deltatime));
            gyro_period_stdev_tview.setText(String.format("%.3f",gyro_x_calib.stdev_deltatime));

            //String s = "Temp. (°C): " + String.format("%.2f", myData.myTempData.GetMeanTemperature()) + " [Max " + String.format("%.2f", myData.myTempData.GetMaxTemperature()) + "]";

            temperature_mean_val_tview.setText(String.format("%.2f °C", myData.myTempData.GetMeanTemperature()));
            temperature_min_val_tview.setText(String.format("%.2f °C", myData.myTempData.GetMinTemperature()));
            temperature_max_val_tview.setText(String.format("%.2f °C", myData.myTempData.GetMaxTemperature()));

            //temperature_tview.setText(s);

            gyro_angle_tview.setText("-.-");
            signal_level_tview.setText(String.valueOf(myData.SignalStrength) + " dBm");

            battery_textview.setText(myData.myBatteryData.level + "%");


            app_uptime_tview.setText(String.format("%02d:%02d:%02d", RunningTime[1], RunningTime[2], RunningTime[3]));
            duty_uptime_tview.setText("00:00:00");

            if( myData.PowerON)
                btPowerOn.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
            else
                btPowerOn.getBackground().setColorFilter(Color.DKGRAY, PorterDuff.Mode.MULTIPLY);

            if( myData.MotorON)
                btMotorOn.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
            else
                btMotorOn.getBackground().setColorFilter(Color.DKGRAY, PorterDuff.Mode.MULTIPLY);

            //app_uptime_tview.setText(AppUptimeString);

            //signal_level_tview.setText(String.valueOf());
            //updatetview_counter = 0;
            //}
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            Log.d(TAG, "UpdateUX", ex);
        }

    }

    //========== UPLOADS LAST FILES ACQUIRED ===================================
    private void upload_lastFiles () {
        //==========================================================================

        myBlobManager.UploadBlobs(myData.myBatteryData.level);

        // CHECK NEW APP UPDATES ==================================
        myBlobManager.CheckNewUpdates(myData.myBatteryData.level);
    }




    //TODO: da verificare se sia necessario tenere qui tutti i metodi Yocto, dato che ci sono le stesse funzioni anche in Yoctopuce

    //==============================================================================================
    //==============================================================================================
    // YOCTOPUCE - MAXI-IO
    //==============================================================================================
    //==============================================================================================
    String MaxiIO_SerialN;
    YDigitalIO MaxiIO;
    YModule tmp;
    //int Motor_OldInputData;
    int Motor_NewInputData;
    //int Wheelchair_OldInputData;
    //int Wheelchair_NewInputData;
    public static final short MaxiIO_MotorPin = 7;
    //public static final short MaxiIO_WheelcPin = 6;

    //==========================================================================
    protected void Start_Yocto() {
        //==========================================================================
        // Connect to Yoctopuce Maxi-IO
        try {
            YAPI.EnableUSBHost(getApplicationContext());
            YAPI.RegisterHub("usb");

            tmp = YModule.FirstModule();
            while (tmp != null) {
                if (tmp.get_productName().equals("Yocto-Maxi-IO")) {

                    MaxiIO_SerialN = tmp.get_serialNumber();
                    MaxiIO = FindDigitalIO(MaxiIO_SerialN);
                    if(MaxiIO.isOnline()) {
                        //call_toast("Maxi-IO connected");
                        Init_Yocto(MaxiIO);
                        MaxiIO.registerValueCallback(this);
                        YAPI.HandleEvents();
                    }
                }
                else {
                    //call_toast("MAXI-IO NOT CONNECTED");
                }
                tmp = tmp.nextModule();
            }
            r.run();
        } catch (YAPI_Exception ex) {
            ex.printStackTrace();
            SaveErrorLog("start_main\t" + ex.toString());
            Log.d(TAG, "Start_Yocto", ex);
        }

        //Faccio partire tra un secondo il runnable di rilevamento del dato dal sensore
        handler.postDelayed(r, 1000);
    }

    //==========================================================================
    protected void Init_Yocto(YDigitalIO moduleName){
        //==========================================================================
        // set the port as input
        try {
            moduleName.set_portDirection(0x0F);             //bit 0-3: OUT; bit 4-7: IN ( bit set to 0)
            moduleName.set_portPolarity(0);                 // polarity set to regular
            moduleName.set_portOpenDrain(0);                // No open drain
            //moduleName.set_portState(0x00);                 // imposta valori logici di uscita inizialmente tutti bassi
        }
        catch(YAPI_Exception e){
            e.printStackTrace();
            SaveErrorLog(e.toString());
            Log.d(TAG, "Init_Yocto", e);
        }
    }

    //==========================================================================
    protected void Stop_Yocto() {
        //==========================================================================
        YAPI.FreeAPI();
        handler.removeCallbacks(r);
    }

    //==========================================================================
    private Handler handler = new Handler();
    //==========================================================================
    //private int _outputdata;
    final Runnable r = new Runnable()
    {
        public void run()
        {
            if (MaxiIO_SerialN != null) {
                YDigitalIO io = YDigitalIO.FindDigitalIO(MaxiIO_SerialN);
                try {
                    YAPI.HandleEvents();
                    // DO THIS EVERYTIME TO LET IT WORK PROPERLY
                    Init_Yocto(MaxiIO);

                    // da togliere per versione finale app
                    /*_outputdata = (_outputdata + 1) % 16;   // cycle ouput 0..15
                    io.set_portState(_outputdata);          // set output value*/

                } catch (YAPI_Exception e) {
                    e.printStackTrace();
                    //SaveErrorLog("run_Y\t" + e.toString());
                    // tolto questo salvataggio perchè genera errori ma il runnable fa quello che deve fare
                    Log.d(TAG, "final Runnable r", e);
                }
            }
            handler.postDelayed(this, 200);
        }
    };

    // NEW VALUE ON PORT:
    @Override
    //==========================================================================
    public void yNewValue(YDigitalIO yDigitalIO, String s) {
        //==========================================================================

        long new_event_time = System.nanoTime();
        event_textview.setText(s);

        try {
            // CHECK MOTOR PIN VALUE
            //Motor_OldInputData = Motor_NewInputData;
            Motor_NewInputData = MaxiIO.get_bitState(MaxiIO_MotorPin);

            // MOTOR EVENT HANDLING
            if (Motor_NewInputData == 1) {
                myData.AddMotorONEvent(new_event_time);
                StartInertialAcquisition();//myData.updateMotorONStartTime();
            }

            if (Motor_NewInputData == 0) {

                myData.AddMotorOFFEvent(new_event_time);
                //myData.updateMotorONStartTime();
            }

        } catch (YAPI_Exception e) {
            e.printStackTrace();
            SaveErrorLog("yNewValue_main\t" + e.toString());
            Log.d(TAG, "yNewValue", e);
        }
    }
    //==========================================================================
    public boolean IsYoctoConnected() {
        //==========================================================================
        try {
            YAPI.EnableUSBHost(getApplicationContext());
            YAPI.RegisterHub("usb");

            tmp = YModule.FirstModule();
            while (tmp != null) {
                if (tmp.get_productName().equals("Yocto-Maxi-IO")) {

                    MaxiIO_SerialN = tmp.get_serialNumber();
                    MaxiIO = FindDigitalIO(MaxiIO_SerialN);

                    if(MaxiIO.isOnline()) {
                        YoctoInUse = true;
                        MaxiIO_textview.setText("MaxiIO connected: YES");
                    }
                    else{
                        YoctoInUse = false;
                        MaxiIO_textview.setText("MaxiIO connected: NO");
                    }
                }
                else {
                    YoctoInUse = false;
                }
                tmp = tmp.nextModule();
            }
        } catch (YAPI_Exception e) {
            e.printStackTrace();
            SaveErrorLog("isYoctoConnected_main\t" + e.toString());
            Log.d(TAG, "IsYoctoConnected", e);
        }
        lastfiles.isyoctoinuse = YoctoInUse;
        return YoctoInUse;
    }


    //******************************************************************************
    //******************************************************************************
    //
    //              Calibration Routines
    //
    //******************************************************************************
    //******************************************************************************

    private boolean CheckIfCalibrationCompleted()
    {
        boolean ret_val = false;
        long now =  System.nanoTime();//Calendar.getInstance().getTime().getTime();

        //TODO: controllare che non si possa usare una sola costante
        long delta_time = (now - CalibrationStartTime) / 1000000000;

        if( delta_time > NUM_OF_SECONDS_CALIBRATION)
            ret_val = true;

        return ret_val;
    }

    public void StartCalibrateInertialSensors()
    {
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

    public void StopCalibrateInertialSensors()
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
                    gyro_x_calib.mean, gyro_y_calib.mean, gyro_z_calib.mean );
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            Log.d(TAG, "StopCalibrateInertialSensors", ex);
        }

    }

    //TODO: da cancellare ed eventualmente spostare il codice altrove

    //==========================================================================
    public void DoDailyReport(View view) {
        //==========================================================================
        // per debug
        //TODO: Verificare che questa sequenza di operazioni sia esaustiva rispetto a quello che ci interessa

        //upload_lastFiles();
        myBlobManager.UploadBlobs(myData.myBatteryData.level);
        // CHECK NEW APP UPDATES ==================================
        myBlobManager.CheckNewUpdates(myData.myBatteryData.level);

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

    private void ResetHourlyCounters() {
        myData.ResetHourlyCounters();
        myData.myTempData.ResetMinMax();
    }


    //==========================================================================
    private void call_toast(CharSequence text){
        //==========================================================================
        // SETS A KIND OF POP-UP MESSAGE
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    //==========================================================================
    private void SaveErrorLog(String msg){
        //==========================================================================
        String StringToSend = "" + SystemClock.elapsedRealtime() + "\t" + msg +"\n";
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

        StartCalibrateInertialSensors();

    }


    public void ToggleManualModeButton(View view)
    {
        if( ManualMode )
        {
            SetUXInteraction(false);

            btToggleMode.setText("Manual Mode");
            btToggleMode.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);

            ManualMode = false;
        }
        else
        {
            SetUXInteraction(true);

            btToggleMode.setText("Auto Mode");
            btToggleMode.getBackground().setColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
            //setBackgroundColor(Color.LTGRAY);

            ManualMode = true;
        }
    }

    private void SetUXInteraction(boolean enabled) {
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

    //******************************************************************
    public void SendHourlyDataButton_Click(View view)
    //******************************************************************
    {
        //myData.updateHourlyUse();
        myEventManager.SendHourlyStatusEvent();
        ResetHourlyCounters();
    }

    //******************************************************************
    public void SendDailyDataButton_Click(View view)
    //******************************************************************
    {
        myEventManager.SendDailyReport();
    }

    //==========================================================================
    public void SendEventButton(View view){
        //==========================================================================
        try {
            //int i = 0;
            //i = i+1;
            //myEventManager.SendStringEvent(tag_view.getText().toString(), value_view.getText().toString());
            myEventManager.sendEventTestClick();
        }
        catch(Exception ex)
        {
            Log.e(TAG, "SendEventButton", ex);
        }

    }

    //==========================================================================
    private void SaveData()
    //==========================================================================
    {

//        FileOutputStream outputStream;
//        FileOutputStream BinaryOutputStream;
        DataOutputStream data_out;
        String filename;

        //Step 1 - Save Calibration file data
        try {
            filename = CommonFilePreamble + "-calib.bin";

            data_out = getDataOutputStream(filename);

            data_out.writeFloat(myData.myInertialData.acc_offset[0]);
            data_out.writeFloat(myData.myInertialData.acc_offset[1]);
            data_out.writeFloat(myData.myInertialData.acc_offset[2]);

            data_out.writeFloat(myData.myInertialData.gyro_offset[0]);
            data_out.writeFloat(myData.myInertialData.gyro_offset[1]);
            data_out.writeFloat(myData.myInertialData.gyro_offset[2]);

            data_out.flush();
            data_out.close();

            myBlobManager.AddFileToList(myConfig.getAcquisitionsFolder(), filename, myConfig.get_Acquisition_Container());
        }
        catch(Exception ex)
        {
            LogException(ex);
        }


        //Step 2 - Save Accelerometer data if available
        try {
            if (myData.myInertialData.acc_data_counter > 0) {

                filename = CommonFilePreamble + "-acc.bin";
                data_out = getDataOutputStream(filename);

                for (int i = 0; i < myData.myInertialData.acc_data_counter; i++) {
                    data_out.write(myData.myInertialData.AccTimestampArray[i]);
                    data_out.writeFloat(myData.myInertialData.AccXDataArray[i]);
                    data_out.writeFloat(myData.myInertialData.AccYDataArray[i]);
                    data_out.writeFloat(myData.myInertialData.AccZDataArray[i]);
                }

                data_out.flush();
                data_out.close();

                myBlobManager.AddFileToList(myConfig.getAcquisitionsFolder(), filename, myConfig.get_Acquisition_Container());
            }
        }
        catch(Exception ex) {
            LogException(ex);
        }

        //Step 3 - Save Gyro data if available
        try {
            if (myData.myInertialData.gyro_data_counter > 0) {
                filename = CommonFilePreamble + "-gyro.bin";
                data_out = getDataOutputStream(filename);

                for (int i = 0; i < myData.myInertialData.gyro_data_counter; i++) {
                    data_out.write(myData.myInertialData.GyroTimestampArray[i]);
                    data_out.writeFloat(myData.myInertialData.GyroXDataArray[i]);
                    data_out.writeFloat(myData.myInertialData.GyroYDataArray[i]);
                    data_out.writeFloat(myData.myInertialData.GyroZDataArray[i]);
                }

                data_out.flush();
                data_out.close();

                myBlobManager.AddFileToList(myConfig.getAcquisitionsFolder(), filename, myConfig.get_Acquisition_Container());

            }
        }
        catch(Exception ex) {
            LogException(ex);
        }
        //Step 4 - Save Temperature data

        try {
            filename = CommonFilePreamble + "-temp.bin";
            data_out = getDataOutputStream(filename);

            data_out.flush();
            data_out.close();

            myBlobManager.AddFileToList(myConfig.getAcquisitionsFolder(), filename, myConfig.get_Acquisition_Container());

        }catch(Exception ex)
        {
            LogException(ex);
        }
        //Step 5 - Save Event info
        try {

            filename = CommonFilePreamble + "-event.bin";
            data_out = getDataOutputStream(filename);

            data_out.flush();
            data_out.close();

            myBlobManager.AddFileToList(myConfig.getAcquisitionsFolder(), filename, myConfig.get_Acquisition_Container());

        }
        catch(Exception ex) {
            LogException(ex);
        }



        //Step 7 - Save Battery Data
        try {
            filename = CommonFilePreamble + "-bat.bin";


        }
        catch(Exception ex) {
            LogException(ex);
        }



        //TODO: verificare le seguenti linee tolte




/*      2016-0830
            BinaryOutputStream = new FileOutputStream(Acc_FilePath, true); //true: append to file

            BufferedOutputStream out = new BufferedOutputStream(BinaryOutputStream);//,numOfBytesInTheBuffer);
            data_out = new DataOutputStream(BinaryOutputStream);

*/

                /* 2016-0622 -
                for (int i = 0; i < InertialData.length; i++)
                {

                    //StringToSave += acc_data.Timestamps[i] + "\t" + acc_data.X[i] + "\t" + acc_data.Y[i] + "\t" + acc_data.Z[i] + "\n";

                    EightBytes = getBytesFromAny(acc_data.Timestamps[i], "long");
                    out.write(EightBytes, 0, 8);
                    FourBytes = getBytesFromAny((long) acc_data.X[i], "float");
                    out.write(FourBytes, 0, 4);
                    FourBytes = getBytesFromAny((long) acc_data.Y[i], "float");
                    out.write(FourBytes, 0, 4);
                    FourBytes = getBytesFromAny((long) acc_data.Z[i], "float");
                    out.write(FourBytes, 0, 4);
                    out.flush();
                }
                */

    }

    private void LogException(Exception ex){
        ex.printStackTrace();
        SaveErrorLog(ex.toString());
        Log.e(TAG, "SaveData", ex);
        FileLog.e(TAG, "SaveData", ex );
    }



    @NonNull
    private DataOutputStream getDataOutputStream(String filename) throws FileNotFoundException {
        DataOutputStream data_out;
        data_out = new DataOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(filename)));
        return data_out;
    }

    private int getDeltaTime()
    {
        return (int)((System.nanoTime() - Daily_Reference_Time)/100000);

    }

    private void ChangeStatus(int new_status)
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