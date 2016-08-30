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
    public MotorData myMotorData;
    public PowerData myPowerData;
    public TemperatureData myTempData;
    public BatteryData myBatteryData;

    Date LastPowerOnDatetime; //serve per rilevare il tempo in cui la carrozzina è stato acceso nell'ultima ora
    Date LastPowerOffDateTime;

    Date LastMotorOnDatetime; //serve per rilevare il tempo in cui il motore è stato acceso nell'ultima ora
    Date LastMotorOffDateTime;

    private static final int NUM_OF_TEMPERARURE_SAMPLES = 8650; //8640 sarebbe il numero corretto

    //Long MotorOnMsDuringLastHour = 0L;  //Tempo di accensione del motore nell'ultima ora
    //Long MotorOnMsDuringLastDay = 0L;  //Tempo di accensione del motore nell'ultimo giorno

    //Long PowerONMsDuringLastHour = 0L;
    //Long PowerONMsDuringLastDay = 0L;

    public int BatteryLevel;

    public WheelchairData()
    {
        myInertialData = new InertialData();
        myMotorData = new MotorData();
        myPowerData = new PowerData();
        myTempData = new TemperatureData(NUM_OF_TEMPERARURE_SAMPLES, 10000, 3600);
        myBatteryData = new BatteryData();
    }

    //Metodo da chiamare quando si fa partire il programma, in modo che
    //verifichi se l'alimentazione è già presente, ed in questo caso non aspetta
    //l'evento PowerON

    public void AddPowerONEvent()
    {
        PowerONHourlyCounter++;
        PowerONDailyCounter++;

        if( LastPowerOnDatetime == null)
            LastPowerOnDatetime = Calendar.getInstance().getTime();

        PowerON = true;
    }


    public void AddPowerOFFEvent()
    {
        PowerOFFHourlyCounter++;
        PowerOFFDailyCounter++;

        LastMotorOffDateTime =  Calendar.getInstance().getTime();
        if( LastPowerOnDatetime != null)
            HourlyMotorOnTime += (float)(LastMotorOffDateTime.getTime() - LastMotorOnDatetime.getTime())*1000f;

        //TODO: verificare che sia corretto (mirimetto in attesa di un evento MotorON
        LastMotorOnDatetime = null;

        //Se il motore è ancora acceso,
        //significa che ci siamo persi un motor OFF e quindi lo segnalo come spento
        if(MotorON)
        {
            AddMotorOFFEvent();
        }

        PowerON = false;
    }

    public void AddMotorONEvent()
    {
        //TODO: aggiungere anche l'inserimento in una lista delle accensioni del motore, comprensiva di timestamp

        MotorStatusLocker.lock();
        try {
            MotorON = true;

            // access the resource protected by this lock
            MotorONHourlyCounter++;
            MotorONDailyCounter++;

            //Date tempDate = Calendar.getInstance().getTime();

            //Se non ho ancora memorizzato il tempo di parteza delle acquisizioni,
            //lo faccio ora
            if ( LastMotorOnDatetime == null )
                LastMotorOnDatetime = Calendar.getInstance().getTime();
                //MotorOnMsDuringLastHour += tempDate.getTime() - LastMotorOnDatetime.getTime();
                //LastMotorOnDatetime = tempDate;

        } finally {
            MotorStatusLocker.unlock();
        }
    }


    public void AddMotorOFFEvent()
    {
        //calcolo la differenza di tempo ed aggiorno la variabile del tempo
        MotorStatusLocker.lock();
        try {
            MotorON = false;

            // access the resource protected by this lock
            MotorOFFHourlyCounter++;
            MotorOFFDailyCounter++;

            LastMotorOffDateTime =  Calendar.getInstance().getTime();
            if( LastMotorOnDatetime != null)
                HourlyMotorOnTime += (float)(LastMotorOffDateTime.getTime() - LastMotorOnDatetime.getTime())*1000f;

            //TODO: verificare che sia corretto (mirimetto in attesa di un evento MotorON
            LastMotorOnDatetime = null;
        }
        finally {
            MotorStatusLocker.unlock();
        }
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
        myInertialData.ResetDailyData(_dailyReferenceTime);
        myTempData.Reset();
        myEventData.Reset();
        myMotorData.Reset();
        myPowerData.Reset();
        myBatteryData.Reset();

    }

    public void updateHourlyUse()
    {
        MotorStatusLocker.lock();
        try {

            if( MotorON )
            {
                //se il motore è acceso, allora aggiorno i dati relativiall'utilizzo orario
                Date tempDate = Calendar.getInstance().getTime();

                if ( LastMotorOnDatetime != null ) {
                    HourlyMotorOnTime += (float) (tempDate.getTime() - LastMotorOnDatetime.getTime()) * 1000f;
                }
                LastMotorOnDatetime = tempDate;
            }

            HourlyUse = HourlyMotorOnTime / 3600000f;

        } finally {
            MotorStatusLocker.unlock();
        }


        DailyMotorOnTime += HourlyMotorOnTime;

    }


        public void updateDailyUse()
    {
        //TODO: verificare che l'utilizzo quotidiano sia dato lungo le 24 ore o rispetto alle ore di accensione della carrozzina
        DailyUse = DailyMotorOnTime / 86400000f;

    }


}
