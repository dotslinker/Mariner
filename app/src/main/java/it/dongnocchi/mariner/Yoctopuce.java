package it.dongnocchi.mariner;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.widget.TextView;

import com.yoctopuce.YoctoAPI.YAPI;
import com.yoctopuce.YoctoAPI.YAPI_Exception;
import com.yoctopuce.YoctoAPI.YDigitalIO;
import com.yoctopuce.YoctoAPI.YModule;

import static com.yoctopuce.YoctoAPI.YDigitalIO.FindDigitalIO;


/**
 * Created by DianaM on 20/08/2015.
 */
//==========================================================================
public class Yoctopuce implements YDigitalIO.UpdateCallback{
    //==========================================================================
    String MaxiIO_SerialN;
    YDigitalIO MaxiIO;
    YModule tmp;
    Context AppContext;
    long Start_Time;
    public static final short Motor_OFF_ID = 0;
    public static final short Motor_ON_ID = 1;

    int  Motor_data_array_index = 0;
    //int Wheelchair_data_array_index = 0;
    static final short buffer_dim_batt_motor = 5;
    MotorData Motor_data = new MotorData(buffer_dim_batt_motor);
    //MotorData Wheelchair_data = new MotorData(buffer_dim_batt_motor);

    String Motor_Path;
    String Wheelchair_Path;

    int Motor_OldInputData;
    int Motor_NewInputData;
    //int Wheelchair_OldInputData;
    //int Wheelchair_NewInputData;
    private int _outputdata;

    TextView view;
    TextView MaxiIO_info;

    boolean UseYocto = false;

    // costruttore
    //==========================================================================
    public Yoctopuce(Context IN_AppContext, long in_start_time, String in_wheelc_path, String in_motor_path, TextView in_yocto_view){
        //==========================================================================
        // IN_AppContext = getApplicationContext() da ricavare così.
        AppContext = IN_AppContext;
        Start_Time = in_start_time;

        Motor_Path = in_motor_path;
        Wheelchair_Path = in_wheelc_path;

        MaxiIO_info = in_yocto_view;
    }


    //==========================================================================
    protected void Start_Yocto() {
        //==========================================================================

        // Connect to Yoctopuce Maxi-IO
        try {
            YAPI.EnableUSBHost(this);
            YAPI.RegisterHub("usb");

            tmp = YModule.FirstModule();
            while (tmp != null) {
                if (tmp.get_productName().equals("Yocto-Maxi-IO")) {

                    MaxiIO_SerialN = tmp.get_serialNumber();
                    MaxiIO = FindDigitalIO(MaxiIO_SerialN);
                    if(MaxiIO.isOnline()) {
                        MaxiIO.registerValueCallback(this);
                        YAPI.HandleEvents();
                        MaxiIO_info.setText("MaxiIO: OK");
                    }
                }
                else {
                    MaxiIO_info.setText("MaxiIO: NO");
                }
                tmp = tmp.nextModule();
            }

        } catch (YAPI_Exception e) {
            e.printStackTrace();
            SaveErrorLog("start\t" + e.toString());
        }
        r.run();
        handler.postDelayed(r, 1000);
    }


    //==========================================================================
    protected void Init_Yocto(){
        //==========================================================================
        // set the port as input
        try {
            MaxiIO.set_portDirection(0x0F);             //bit 0-3: OUT; bit 4-7: IN
            MaxiIO.set_portPolarity(0);                 // polarity set to regular
            MaxiIO.set_portOpenDrain(0);                // No open drain
            //MaxiIO.set_portState(0x00);                 // imposta valori logici di uscita inizialmente tutti bassi
            _outputdata = (_outputdata + 1) % 16;   // cycle ouput 0..15
            MaxiIO.set_portState(_outputdata);          // set output value
        }
        catch(YAPI_Exception e){
            e.printStackTrace();
            SaveErrorLog(e.toString());
        }
    }

    //==========================================================================
    protected void Stop_Yocto() {
        //==========================================================================
        YAPI.FreeAPI();
    }


    //==============================================================================================
    //==============================================================================================
    //  YOCTOPUCE: EVENTS HANDLING
    //==============================================================================================
    //==============================================================================================
    private Handler handler = new Handler();

    final Runnable r = new Runnable()
    {
        @Override
        public void run()
        {
            if (MaxiIO_SerialN != null) {

                YDigitalIO io = YDigitalIO.FindDigitalIO(MaxiIO_SerialN);
                try {
                   /* YAPI.EnableUSBHost(AppContext);
                    YAPI.RegisterHub("usb");*/
                    YAPI.HandleEvents();

                    // DO THIS EVERYTIME TO LET IT WORK PROPERLY
                    io.set_portDirection(0x0F);             //bit 0-3: OUT; bit 4-7: IN ( bit set to 0)
                    io.set_portPolarity(0);                 // polarity set to regular
                    io.set_portOpenDrain(0);                // No open drain

                    // da togliere per funzionamento finale, serve solo per debug
                    _outputdata = (_outputdata + 1) % 16;   // cycle ouput 0..15
                    io.set_portState(_outputdata);          // set output value

                    // read motor value
                    //int inputdata = io.get_bitState(7);      // read bit value

                } catch (YAPI_Exception e) {
                    e.printStackTrace();
                    SaveErrorLog("run\t" + e.toString());
                }
            }
            handler.postDelayed(this,1000);
        }
    };


    // NEW VALUE ON PORT:
    @Override
    //==========================================================================
    public void yNewValue(YDigitalIO yDigitalIO, String s) {
        //==========================================================================
        view = (TextView) view.findViewById(R.id.event_view);
        view.setText(s);

        try {
            // CHECK MOTOR PIN VALUE
            Motor_OldInputData = Motor_NewInputData;
            Motor_NewInputData = MaxiIO.get_bitState(7);

            // CHECK WHEELCHAIR ON/OFF PIN VALUE
            /*Wheelchair_OldInputData = Wheelchair_NewInputData;
            Wheelchair_NewInputData = MaxiIO.get_bitState(6);
            */

            // MOTOR EVENT HANDLING
/*
            if (Motor_NewInputData != Motor_OldInputData) {

                MotorData tmp = new MotorData(1);
                tmp.Time[0] = SystemClock.elapsedRealtime() - App_Start_Time;

                if (Motor_NewInputData == 1 && Motor_OldInputData == 0) {           // occurred motor event: now it is ON
                    tmp.Status[0] = Motor_ON_ID;

                } else if (Motor_NewInputData == 0 && Motor_OldInputData == 1) {    // occurred motor event: now it is OFF
                    tmp.Status[0] = Motor_OFF_ID;
                }

                // APPEND DATA AND SAVE ON FILE
                if (Motor_data_array_index < Motor_data.Time.length) {
                    Motor_data.Time[Motor_data_array_index] = tmp.Time[0];
                    Motor_data.Status[Motor_data_array_index] = tmp.Status[0];
                    Motor_data_array_index++;

                    if(Motor_data_array_index == Motor_data.Time.length){
                        Background_Save bg_save = new Background_Save(Motor_data, null, null, null , null, Motor_FilePath);
                        bg_save.execute();
                        Motor_data_array_index = 0;
                    }

                } else {
                    // MANAGE FULL ARRAY
                }
            }
            */

            /*
            // WHEELCHAIR ON/OFF EVENT HANDLING
            if (Wheelchair_NewInputData != Wheelchair_OldInputData) {

                MotorData tmp = new MotorData(1);
                //tmp.Time[0] = System.currentTimeMillis();
                tmp.Time[0] = SystemClock.elapsedRealtime() - App_Start_Time;

                if (Wheelchair_NewInputData == 1 && Wheelchair_OldInputData == 0) {           // occurred wheelchair event: now it is ON
                    tmp.Status[0] = Motor_ON_ID;

                } else if (Wheelchair_NewInputData == 0 && Wheelchair_OldInputData == 1) {    // occurred wheelchair event: now it is OFF
                    tmp.Status[0] = Motor_OFF_ID;
                }
                // APPEND DATA AND SAVE ON FILE
                if (Wheelchair_data_array_index < Wheelchair_data.Time.length) {
                    Wheelchair_data.Time[Wheelchair_data_array_index] = tmp.Time[0];
                    Wheelchair_data.Status[Wheelchair_data_array_index] = tmp.Status[0];
                    Wheelchair_data_array_index++;

                    if(Wheelchair_data_array_index == Wheelchair_data.Time.length){
                        Background_Save bg_save = new Background_Save(null, null, null, null, Wheelchair_data, Power_FilePath);
                        bg_save.execute();
                        Wheelchair_data_array_index = 0;
                    }

                } else {
                    // MANAGE FULL ARRAY
                }
            }
            */
        } catch (YAPI_Exception e) {
            e.printStackTrace();
            SaveErrorLog("yNewVaue\t" + e.toString());
        }
    }

    //==========================================================================
    public void IsYoctoConnected() {
        //==========================================================================
        try {
            YAPI.EnableUSBHost(AppContext);
            YAPI.RegisterHub("usb");

            tmp = YModule.FirstModule();
            while (tmp != null) {
                if (tmp.get_productName().equals("Yocto-Maxi-IO")) {

                    MaxiIO_SerialN = tmp.get_serialNumber();
                    MaxiIO = FindDigitalIO(MaxiIO_SerialN);

                    if(MaxiIO.isOnline()) {
                        UseYocto = true;
                        MaxiIO_info.setText("MaxiIO connected: YES");
                    }
                    else{
                        UseYocto = false;
                        MaxiIO_info.setText("MaxiIO connected: NO");
                    }
                }
                tmp = tmp.nextModule();
            }
        } catch (YAPI_Exception e) {
            e.printStackTrace();
            SaveErrorLog(e.toString());
        }
    }

    public void set_use_yocto(boolean in_use)   {  UseYocto = in_use;}
    public boolean tell_use_yocto ()            {  return UseYocto;  }

    //==========================================================================
    private void SaveErrorLog(String msg){
        //==========================================================================
        String StringToSend = "" + SystemClock.elapsedRealtime() + "\t" + msg +"\n";
        LogFile_Handler BkgSave_LogHandler = new LogFile_Handler(StringToSend);
        BkgSave_LogHandler.execute();
    }
}
