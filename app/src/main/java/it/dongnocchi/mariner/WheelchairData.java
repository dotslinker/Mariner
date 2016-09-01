package it.dongnocchi.mariner;

import java.util.Calendar;
import java.util.Date;
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

    // Amount of seconds the power is on
    public float DailyPowerOnTime;

    //Amount of seconds the motor is on
    public float DailyMotorOnTime;

    public float HourlyUse;
    public float DailyUse;
    public int Status;
    public String HourlyNote;
    public String GPSPosition;

    private Lock MotorStatusLocker = new ReentrantLock();

    public InertialData myInertialData;
//    public MotorData myMotorData;
//    public PowerData myPowerData;
    public TemperatureData myTempData;
    public BatteryData myBatteryData;

    //TODO: da sistemare il periodo di aggiornamento del dato relativo alla potenza del segnale
    public int SignalStrength;

    public EventData myEventData;

    long LastPowerOnTime, LastPowerOffTime, LastMotorOnTime, LastMotorOffTime;

    long DailyReferenceTime;


    private static final int NUM_OF_TEMPERARURE_SAMPLES = 8650; //8640 sarebbe il numero corretto

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
    }


    //Metodo da chiamare quando si fa partire il programma, in modo che
    //verifichi se l'alimentazione è già presente, ed in questo caso non aspetta
    //l'evento PowerON

    public void AddPowerONEvent(long EventTime)
    {
        PowerONHourlyCounter++;
        PowerONDailyCounter++;

        if( LastPowerOnTime == 0)
            LastPowerOnTime = EventTime;

        int deltatime = (int) ((EventTime - DailyReferenceTime)/ 100000);

//        if( LastPowerOnDatetime == null)
//            LastPowerOnDatetime = Calendar.getInstance().getTime();

        myEventData.AddData(deltatime, EventData.POWER_ON);

        PowerON = true;
    }


    public void AddPowerOFFEvent(long EventTime)
    {
        PowerOFFHourlyCounter++;
        PowerOFFDailyCounter++;

        int deltatime = (int) ((EventTime - DailyReferenceTime)/ 100000);

//        LastMotorOffDateTime =  Calendar.getInstance().getTime();
        LastPowerOffTime = EventTime;
        if( LastPowerOnTime != 0) {
            HourlyMotorOnTime += (float) (LastMotorOffTime - LastMotorOnTime) / 1000000000.0f;

        }

        myEventData.AddData(deltatime, EventData.POWER_OFF);

        LastMotorOnTime = 0;

        //Se il motore è ancora acceso,
        //significa che ci siamo persi un motor OFF e quindi lo segnalo come spento
        if(MotorON)
        {
            AddMotorOFFEvent(EventTime);
        }

        PowerON = false;
    }

    public void AddMotorONEvent(long EventTime) {
        int deltatime = (int) ((EventTime - DailyReferenceTime)/ 100000);


        MotorStatusLocker.lock();
        try {
            //Check if Motor is OFF
            if (!MotorON)

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

        }
        catch(Exception ex) {

        }
        finally {
            MotorStatusLocker.unlock();
        }
    }


    public void AddMotorOFFEvent(long EventTime)
    {
        //calcolo la differenza di tempo ed aggiorno la variabile del tempo
        int deltatime = (int) ((EventTime - DailyReferenceTime)/ 100000);


        MotorStatusLocker.lock();
        try {
            // access the resource protected by this lock

            //Check if Motor is ON
            if(MotorON )
            MotorON = false;

            MotorOFFHourlyCounter++;
            MotorOFFDailyCounter++;

            LastMotorOffTime = EventTime;
            if( LastMotorOnTime != 0)
                HourlyMotorOnTime += (float)(LastMotorOffTime - LastMotorOnTime)/1000000000.0f;

            myEventData.AddData(deltatime, EventData.MOTOR_OFF);

            LastMotorOnTime = 0;
        }
        finally {
            MotorStatusLocker.unlock();
        }
    }

    public void AddBatteryValChangeEvent(long EventTime, int new_val)
    {
        int deltatime = (int) ((EventTime - DailyReferenceTime)/ 100000);
        myBatteryData.AddData(deltatime, new_val);
    }

    public void ResetHourlyCounters()
    {
        PowerONHourlyCounter = 0;
        MotorONHourlyCounter = 0;

        HourlyMotorOnTime = 0f;
        HourlyPowerOnTime = 0f;

        HourlyUse = 0;
    }

    public void ResetDailyCounters()
    {
        PowerONDailyCounter = 0;
        MotorONDailyCounter = 0;

        DailyMotorOnTime = 0f;
        DailyPowerOnTime = 0f;

        FwMetersCovered = 0; //Forward meters covered
        BwMetersCovered = 0; //backward meters covered

        DegreesCoveredTurningLeft = 0;
        DegreesCoveredTurningRight = 0;

        DailyUse = 0;
    }

    public void DailyReset(long _dailyReferenceTime)
    {
        DailyReferenceTime = _dailyReferenceTime;
        myInertialData.ResetDailyData(_dailyReferenceTime);
        myTempData.Reset();
        myEventData.Reset();
        //myMotorData.Reset();
        //myPowerData.Reset();
        myBatteryData.Reset();

        ResetDailyCounters();
    }

    public void updateHourlyUse()
    {
        MotorStatusLocker.lock();
        try {

            if( MotorON )
            {
                //se il motore è acceso, allora aggiorno i dati relativiall'utilizzo orario
                long tempTime = System.nanoTime();
                //Date tempDate = Calendar.getInstance().getTime();

                if ( LastMotorOnTime != 0 ) {
                    HourlyMotorOnTime += (float) (tempTime - LastMotorOnTime) / 1000000000.0f;
                }
                LastMotorOnTime = tempTime;
            }

            HourlyUse = HourlyMotorOnTime / 3600.0f;

        } finally {
            MotorStatusLocker.unlock();
        }


        DailyMotorOnTime += HourlyMotorOnTime;

    }


        public void updateDailyUse()
    {
        //TODO: verificare che l'utilizzo quotidiano sia dato lungo le 24 ore o rispetto alle ore di accensione della carrozzina
        DailyUse = DailyMotorOnTime / 86400.0f;

    }


}
