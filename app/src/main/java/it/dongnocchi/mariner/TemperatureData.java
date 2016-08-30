package it.dongnocchi.mariner;

import android.hardware.SensorEvent;

/**
 * Created by Paolo on 14/03/2016.
 */
public class TemperatureData {

    public int data_size;
    public long[] TimestampArray; // = new long[NUM_OF_TEMPERARURE_SAMPLES];
    public float [] DataArray; // = new float [NUM_OF_TEMPERARURE_SAMPLES];
    int data_counter;
    //public int TotalNumOfSamples;

    MovingAverage TempMA;
    public float MaxTemperature;
    public float MinTemperature;

    public TemperatureData(int num_of_samples, int sampling_period_ms, int num_of_seconds_for_mean)
    {
        data_size = num_of_samples;
        TimestampArray = new long[num_of_samples];
        DataArray = new float [num_of_samples];
        int ma_size;

        if (sampling_period_ms != 0 && num_of_seconds_for_mean != 0)
            ma_size = num_of_seconds_for_mean * 1000 / sampling_period_ms;
        else
            ma_size = 100;

        TempMA = new MovingAverage(ma_size);

        ResetMinMax();
    }

    public void AppendData(SensorEvent data)
    {
        float act_temp = data.values[0];

        DataArray[data_counter] = act_temp;
        TempMA.UpdateValue(act_temp);

        TimestampArray[data_counter] = data.timestamp;

        //Se sforo il
        if(++data_counter >= data_size)
            data_counter = 0;

        //TotalNumOfSamples++;

        if (act_temp > MaxTemperature)
            MaxTemperature = act_temp;

        if (act_temp < MinTemperature)
            MinTemperature = act_temp;

    }

    public void ResetMinMax()
    {
        MaxTemperature = 0.0f;
        MinTemperature = 100.0f;
    }

    public void Reset()
    {
        for(int i= 0; i<data_size;i++)
        {
            TimestampArray[i] = 0;
            DataArray[i] = 0;
        }
        data_counter = 0;

        ResetMinMax();

        TempMA.ResetData();
    }

    public float GetMaxTemperature()
    {
        return MaxTemperature;
    }

    public float GetMinTemperature()
    {
        return MinTemperature;
    }

    public float GetMeanTemperature()
    {
        return TempMA.MeanVal;
    }

}
