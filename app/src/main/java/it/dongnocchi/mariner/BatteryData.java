package it.dongnocchi.mariner;

/**
 * Created by Paolo on 30/08/2016.
 */
public class BatteryData {

    protected int Timestamps[];
    protected int Values[];

    int data_counter;
    int data_size;

    final int NUM_OF_SAMPLES = 1440; // 1 sample per minute * 24 hours

    public BatteryData(int dim)
    {
        data_size = dim;
        this.Timestamps=new int[dim];
        this.Values = new int[dim];
        data_counter = 0;
    }

    public BatteryData()
    {
        data_size = NUM_OF_SAMPLES;
        this.Timestamps=new int[NUM_OF_SAMPLES];
        this.Values = new int[NUM_OF_SAMPLES];
        data_counter = 0;
    }


    public void AddData(int new_elapsed_time, int new_value)
    {

        Timestamps[data_counter] = new_elapsed_time;
        Values[data_counter] = new_value;

        if (++data_counter >= data_size)
            data_counter--;

    }

    public int getCurrentTimestamp()
    {
        //if(data_counter > 0)
        return Timestamps[data_counter];
    }

    public int getCurrentValue()
    {
        return Values[data_counter];
    }


    public void Reset()
    {
        for(int i= 0; i<data_size;i++)
        {
            Timestamps[i] = 0;
            Values[i] = 0;
        }
        data_counter = 0;

    }




}
