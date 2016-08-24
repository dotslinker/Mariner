package it.dongnocchi.mariner;

import android.hardware.SensorEvent;

/**
 * Created by Paolo on 14/03/2016.
 */
public class TemperatureData {

    public int NumOfTemperatureSamples;
    public long[] TemperatureTimestampArray; // = new long[NUM_OF_TEMPERARURE_SAMPLES];
    public float [] TemperatureDataArray; // = new float [NUM_OF_TEMPERARURE_SAMPLES];
    int Temperature_data_array_index;
    //public int TotalNumOfSamples;
    MovingAverage TempMA;
    public float MaxTemperature;
    public float MinTemperature;

    public TemperatureData(int num_of_samples, int sampling_period_ms, int num_of_seconds_for_mean)
    {
        NumOfTemperatureSamples = num_of_samples;
        TemperatureTimestampArray = new long[num_of_samples];
        TemperatureDataArray = new float [num_of_samples];
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

        TemperatureDataArray[Temperature_data_array_index] = act_temp;
        TempMA.UpdateValue(act_temp);

        TemperatureTimestampArray[Temperature_data_array_index] = data.timestamp;

        //Se sforo il
        if(++Temperature_data_array_index >= NumOfTemperatureSamples)
            Temperature_data_array_index = 0;

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
