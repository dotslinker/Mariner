package it.dongnocchi.mariner;

import android.os.SystemClock;

/**
 * Created by Paolo on 05/04/2016.
 */
public class PowerData {
    public final int POWER_ON = 1;
    public final int POWER_OFF = 0;

    protected int Timestamps[];
    protected short Conditions[];

    int data_counter;
    int data_size;

    long reference_time;

    final int NUM_OF_SAMPLES = 14400; // 10 Hz * 3600s * 4ore

    public PowerData(int dim)
    {
        data_size = dim;
        this.Timestamps=new int[dim];
        this.Conditions = new short[dim];
        data_counter = 0;
    }

    public PowerData()
    {
        data_size = NUM_OF_SAMPLES;
        this.Timestamps=new int[NUM_OF_SAMPLES];
        this.Conditions = new short[NUM_OF_SAMPLES];
        data_counter = 0;
    }


    public void AddData(int new_time, short new_condition)
    {
        Timestamps[data_counter] = new_time;
        Conditions[data_counter] = new_condition;

        if (++data_counter >= data_size)
            data_counter--;
    }

    public void AddPowerONEvent()
    {

        Timestamps[data_counter] = (int) (SystemClock.elapsedRealtime() - reference_time);
        Conditions[data_counter] = POWER_ON;

        if (++data_counter >= data_size)
            data_counter--;
    }

    public void AddPowerOFFvent()
    {
        Timestamps[data_counter] = (int) (SystemClock.elapsedRealtime() - reference_time);
        Conditions[data_counter] = POWER_OFF;

        if (++data_counter >= data_size)
            data_counter--;
    }


    public void ResetData(long _reference_time)
    {
        data_counter = 0;
        reference_time = _reference_time;
    }



    public long getCurrentTimestamp()
    {
        //if(data_counter > 0)
        return Timestamps[data_counter];
    }

    public short getCurrentValue()
    {
        return Conditions[data_counter];
    }





}
