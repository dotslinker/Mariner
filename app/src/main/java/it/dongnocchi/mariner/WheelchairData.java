package it.dongnocchi.mariner;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Created by Paolo on 27/01/2016.
 */
public class WheelchairData {

    public boolean PowerON; //flag for Power supply present
    public boolean MotorON; //flag for Motor active

    public int PowerONHourlyCounter;
    public int PowerOFFHourlyCounter;

    public int MotorONHourlyCounter;
    public int MotorOFFHourlyCounter;

    public int PowerONDailyCounter;
    public int PowerOFFDailyCounter;

    public int MotorONDailyCounter;
    public int MotorOFFDailyCounter;

    public float FwMetersCovered; //Forward meters covered
    public float BwMetersCovered; //backward meters covered

    public float DegreesCoveredTurningLeft;
    public float DegreesCoveredTurningRight;

    //Amount of time in seconds
    public float HourlyPowerOnTime;
    public float HourlyMotorOnTime;

    public float HourlyPowerOnTimePerc;
    public float HourlyMotorOnTimePerc;

    public float DailyPowerOnTimePerc; //To be calculated over 12 hours
    public float DailyMotorOnTimePerc; //To be calculated over 12 hours

    long LastPowerOnTime, LastPowerOffTime, LastMotorOnTime, LastMotorOffTime;

    public int StorageMemoryAvailable;//, StorageTotalMemory;

    // Amount of seconds the power is on

    public float DailyPowerOnTime;

    //Amount of seconds the motor is on
    public float DailyMotorOnTime;

    //public float HourlyUse;

    //public float MinDailyTemperature;
    //public float MeanDailyTemperature;
    //public float MaxDailyTemperature;

    public long MinHourlyMemory;
    public long MeanHourlyMemory;
    public long MaxHourlyMemory;

    public long MinDailyMemory;
    public long MeanDailyMemory;
    public long MaxDailyMemory;

    public float DailyUse;
    public int Status;
    public String HourlyNote;
    //public String GPSPosition;

    public float CurrentLightValue;
    public float MinLightValue;
    public float MaxLightValue;
    public int NumOfHourlyLightTransitions;
    public int NumOfDailyLightTransitions;

    public float MaxDailyLightValue;

    public int NumberOfTouch;
    public int NumberOfDailyTouch;

    public String DailyLogFileName;
    public String UploadedFileListString;

    private Lock MotorStatusLocker = new ReentrantLock();
    private Lock PowerStatusLocker = new ReentrantLock();

    public InertialData myInertialData;
//    public MotorData myMotorData;
//    public PowerData myPowerData;
    public TemperatureData myTempData;
    public BatteryData myBatteryData;

    //TODO: da sistemare il periodo di aggiornamento del dato relativo alla potenza del segnale
    public int SignalStrength;

    public EventData myEventData;

    long DailyReferenceTime;

    private static final int NUM_OF_TEMPERARURE_SAMPLES = 8650; //8640 sarebbe il numero corretto

    public long MemoryUsed, MemoryAvailable;
    //public float MemoryUsedFloat_KB, MaxMemoryUsedFloat_KB;
    //public int MemoryUsedFloatSumCounter;

    //Runtime myRuntime;


    //public int BatteryLevel;

    public WheelchairData(long referencetime)
    {
        DailyReferenceTime = referencetime;
        myInertialData = new InertialData();
//        myMotorData = new MotorData();
//        myPowerData = new PowerData();
        myTempData = new TemperatureData(NUM_OF_TEMPERARURE_SAMPLES, 10000, 3600);
        myBatteryData = new BatteryData();
        myEventData = new EventData();
        //myRuntime = Runtime.getRuntime();
    }

    public String GetDailyLog()
    {
        return DailyLogFileName;
    }

    public String GetUploadedFileList() {
        return UploadedFileListString;
    }

    public void UpdateLightValue(float new_val)
    {

        if(CurrentLightValue < 0.1 && new_val > 0.1)
        {
            NumOfDailyLightTransitions++;
            NumOfHourlyLightTransitions++;
        }

        if (MinLightValue > new_val)
            MinLightValue = new_val;

        if (MaxLightValue < new_val)
            MaxLightValue = new_val;

        if (MaxDailyLightValue < MaxLightValue)
            MaxDailyLightValue = MaxLightValue;

        CurrentLightValue = new_val;

    }

    public void UpdateTemperatureValue(float new_val)
    {



    }


    //==========================================================================
    public void UpdateMemoryUsage()
    //==========================================================================
    {
        Runtime myRuntime = Runtime.getRuntime();
        float memused_KB = ((float)(myRuntime.totalMemory() - myRuntime.freeMemory())) / 1048576.0F;

        //if( MaxMemoryUsedFloat_KB < memused_KB)
        //    MaxMemoryUsedFloat_KB = memused_KB;

        //MemoryUsedFloat_KB += memused_KB;
        //MemoryUsedFloatSumCounter++;

        MemoryUsed = (myRuntime.totalMemory() - myRuntime.freeMemory()) / 1048576L;
        MemoryAvailable = myRuntime.maxMemory() / 1048576L;

        if (MinHourlyMemory > MemoryUsed)
            MinHourlyMemory = MemoryUsed;

        if (MinDailyMemory > MemoryUsed)
            MinDailyMemory = MemoryUsed;

        if (MaxHourlyMemory < MemoryUsed)
            MaxHourlyMemory = MemoryUsed;

        if (MaxDailyMemory < MemoryUsed)
            MaxDailyMemory = MemoryUsed;
    }

    //Metodo da chiamare quando si fa partire il programma, in modo che
    //verifichi se l'alimentazione è già presente, ed in questo caso non aspetta
    //l'evento PowerON

    public void AddPowerONEvent(long EventTime)
    {
        int deltatime = (int) ((EventTime - DailyReferenceTime) / 100000L);

        PowerStatusLocker.lock();
        try {

            PowerON = true;

            PowerONHourlyCounter++;
            PowerONDailyCounter++;

//        if( LastPowerOnDatetime == null)
//            LastPowerOnDatetime = Calendar.getInstance().getTime();
            if (LastPowerOnTime == 0) {
                LastPowerOnTime = EventTime;
            }

            myEventData.AddData(deltatime, EventData.POWER_ON);

        } finally {
            PowerStatusLocker.unlock();
        }
    }

    public void AddPowerOFFEvent(long EventTime)
    {
        int deltatime = (int) ((EventTime - DailyReferenceTime) / 100000L);

        PowerStatusLocker.lock();

        try {
            PowerOFFHourlyCounter++;
            PowerOFFDailyCounter++;

//        LastMotorOffDateTime =  Calendar.getInstance().getTime();
            LastPowerOffTime = EventTime;

            if (LastPowerOnTime != 0) {
                HourlyPowerOnTime += (float) (LastPowerOffTime - LastPowerOnTime) / 1000000000.0f;
                LastPowerOnTime = 0;
            }

            myEventData.AddData(deltatime, EventData.POWER_OFF);


            //Se il motore è ancora acceso,
            //significa che ci siamo persi un motor OFF e quindi lo segnalo come spento
            /*
            if (MotorON) {

                AddMotorOFFEvent(EventTime);
            }
            */
            PowerON = false;
        }
        finally{
            PowerStatusLocker.unlock();
        }
    }

    public void AddMotorONEvent(long EventTime) {
        int deltatime = (int) ((EventTime - DailyReferenceTime)/ 100000L);

        MotorStatusLocker.lock();
        try {

            //Check if Motor is OFF
            MotorON = true;

            // access the resource protected by this lock
            MotorONHourlyCounter++;
            MotorONDailyCounter++;

            //Date tempDate = Calendar.getInstance().getTime();

            //Se non ho ancora memorizzato il tempo di parteza delle acquisizioni,
            //lo faccio ora
            if (LastMotorOnTime == 0)
                LastMotorOnTime = EventTime;

            myEventData.AddData(deltatime, EventData.MOTOR_ON);
            //MotorOnMsDuringLastHour += tempDate.getTime() - LastMotorOnDatetime.getTime();
            //LastMotorOnDatetime = tempDate;

        } catch (Exception ex) {

        } finally {
            MotorStatusLocker.unlock();
        }
    }


    public void AddMotorOFFEvent(long EventTime)
    {
        //calcolo la differenza di tempo ed aggiorno la variabile del tempo
        int deltatime = (int) ((EventTime - DailyReferenceTime)/ 100000L);

        MotorStatusLocker.lock();
        try {
            // access the resource protected by this lock

            //Check if Motor is ON

            MotorOFFHourlyCounter++;
            MotorOFFDailyCounter++;

            LastMotorOffTime = EventTime;
            if( LastMotorOnTime != 0) {
                HourlyMotorOnTime += (float) (LastMotorOffTime - LastMotorOnTime) / 1000000000.0f;
                LastMotorOnTime = 0;
            }
            myEventData.AddData(deltatime, EventData.MOTOR_OFF);

            MotorON = false;

        }
        finally {
            MotorStatusLocker.unlock();
        }
    }


    public void AddBatteryValChangeEvent(long EventTime, int new_val)
    {
        int deltatime = (int) ((EventTime - DailyReferenceTime)/ 100000L);
        myBatteryData.AddData(deltatime, new_val);
    }

    public void AddEveBatteryLowEvent(Long EventTime)
    {
        int deltatime = (int) ((EventTime - DailyReferenceTime)/ 100000L);

    }


    public void ResetHourlyCounters()
    {
        PowerONHourlyCounter = 0;
        MotorONHourlyCounter = 0;
        PowerOFFHourlyCounter = 0;
        MotorOFFDailyCounter= 0;

        HourlyMotorOnTime = 0f;
        HourlyPowerOnTime = 0f;

        HourlyMotorOnTimePerc = 0;
        HourlyPowerOnTimePerc = 0;

        MemoryDataHourlyReset();

        LightDataHourlyReset();
    }

    public void LightDataHourlyReset()
    {
        MinLightValue = 100;
        MaxLightValue = 0;

        NumOfHourlyLightTransitions = 0;
    }


    public void ResetDailyCounters()
    {
        PowerONDailyCounter = 0;
        PowerOFFDailyCounter = 0;
        MotorONDailyCounter = 0;
        MotorOFFDailyCounter = 0;

        DailyMotorOnTime = 0f;
        DailyPowerOnTime = 0f;

        FwMetersCovered = 0; //Forward meters covered
        BwMetersCovered = 0; //backward meters covered

        DegreesCoveredTurningLeft = 0;
        DegreesCoveredTurningRight = 0;

        DailyUse = 0;

        MemoryDataDailyReset();

        LightDataDailyReset();
    }

    private void LightDataDailyReset()
    {
        MaxDailyLightValue = 0;
        NumOfDailyLightTransitions = 0;
    }

    public void DailyReset(long _dailyReferenceTime)
    {
        DailyReferenceTime = _dailyReferenceTime;
        myInertialData.ResetDailyData(_dailyReferenceTime);
        myTempData.DailyReset(_dailyReferenceTime);

        myEventData.Reset();
        //myMotorData.DailyReset();
        //myPowerData.DailyReset();
        myBatteryData.Reset();

        ResetDailyCounters();

        UploadedFileListString = "";
    }

    private void MemoryDataDailyReset()
    {
        //DailyReset Memory Usage indexes
        //MaxMemoryUsedFloat_KB = 0;
        //MemoryUsedFloat_KB = 0;
        //MemoryUsedFloatSumCounter = 0;

        MinDailyMemory = 100000000L;
        MaxDailyMemory = 0;
    }

    private void MemoryDataHourlyReset()
    {
        MinHourlyMemory = 100000000L;
        MaxHourlyMemory = 0;
    }


    public void updateHourlyUse(long hourly_ref_time, long now)
    {
        float delta_time = (float)(now - hourly_ref_time)  / 1000000000.0f;

        MotorStatusLocker.lock();
        try
        {
            if( MotorON )
            {
                //se il motore è acceso, allora aggiorno i dati relativiall'utilizzo orario
                //Date tempDate = Calendar.getInstance().getTime();

                if ( LastMotorOnTime != 0 ) {
                    HourlyMotorOnTime += (float) (now - LastMotorOnTime) / 1000000000.0f;
                }
                LastMotorOnTime = now;
            }

            HourlyMotorOnTimePerc = HourlyMotorOnTime / delta_time * 100.0f;

        } finally
        {
            MotorStatusLocker.unlock();
        }

        DailyMotorOnTime += HourlyMotorOnTime;

        PowerStatusLocker.lock();
        try {
            if (PowerON) {
                if (LastPowerOnTime != 0) {
                    HourlyPowerOnTime += (float) (now - LastPowerOnTime) / 1000000000.0f;
                }
                LastPowerOnTime = now;
            }

            HourlyPowerOnTimePerc = HourlyPowerOnTime / delta_time * 100.0f;

        }
        finally
        {
            PowerStatusLocker.unlock();
        }

        DailyPowerOnTime += HourlyPowerOnTime;

        UpdateMemoryUsage();
    }

/*
    public void updateDailyUse()
    {
        //TODO: verificare che l'utilizzo quotidiano sia dato lungo le 24 ore o rispetto alle ore di accensione della carrozzina
        DailyUse = DailyMotorOnTime / 864.0f;

    }
*/
    public void UpdateCalibrationData()
    {
        myInertialData.UpdateCalibrationInfo();

    }
}
