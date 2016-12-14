package it.dongnocchi.mariner;

import android.hardware.SensorEvent;

/**
 * Created by Paolo on 14/03/2016.
 */
public class TemperatureData {

    public int data_size;
    public int[] TimestampArray; // = new long[NUM_OF_TEMPERARURE_SAMPLES];
    public float [] DataArray; // = new float [NUM_OF_TEMPERARURE_SAMPLES];
    int data_counter;
    //public int TotalNumOfSamples;

    long reference_time;

    MovingAverage TempMA;
    public float MaxTemperature = 0.0f;
    public float MinTemperature = 100.0f;
    public float CurrentTemperature = 0;

    public float MinHourlyTemperature = 100.0f;
    public float MaxHourlyTemperature = 0.0f;
    public float MinDailyTemperature = 100.0f;
    public float MeanDailyTemperature = 100.0f;
    public float MaxDailyTemperature = 0.0f;

    public TemperatureData(int num_of_samples, int sampling_period_ms, int num_of_seconds_for_mean)
    {
        data_size = num_of_samples;
        TimestampArray = new int[num_of_samples];
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

        CurrentTemperature = act_temp;

        DataArray[data_counter] = act_temp;
        TempMA.UpdateValue(act_temp);

        TimestampArray[data_counter] = (int)((data.timestamp - reference_time)/100000L);

        //Se sforo il
        if(++data_counter >= data_size)
            data_counter = 0;

        if (act_temp > MaxTemperature)
            MaxTemperature = act_temp;

        if (act_temp < MinTemperature)
            MinTemperature = act_temp;

        if (act_temp > MaxHourlyTemperature)
            MaxHourlyTemperature= act_temp;

        if (act_temp < MinHourlyTemperature)
            MinHourlyTemperature= act_temp;

        if (act_temp > MaxDailyTemperature)
            MaxDailyTemperature= act_temp;

        if (act_temp < MinDailyTemperature)
            MinDailyTemperature= act_temp;

    }

    public void ResetMinMax()
    {
        MaxTemperature = 0.0f;
        MinTemperature = 100.0f;
    }

    public void DailyReset(long _new_ref_time)
    {
        for(int i= 0; i<data_size;i++)
        {
            TimestampArray[i] = 0;
            DataArray[i] = 0.0f;
        }
        data_counter = 0;

        ResetMinMax();

        TempMA.ResetData();

        MinDailyTemperature = 100.0f;
        MaxDailyTemperature = 0.0f;

        reference_time = _new_ref_time;
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
