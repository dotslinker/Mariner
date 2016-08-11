package it.dongnocchi.mariner;

/**
 * Created by Paolo on 11/08/2016.
 */
public class DataArray {
    float [] data;
    int data_counter;
    public float mean;
    public float stdev;
    int size;

    public DataArray(int _size)
    {
        size = _size;
        data = new float[size];
        data_counter = 0;
    }

    public void reset_data()
    {
        data_counter = 0;
        for (int i = 0; i< size; i++)
            data[i] = 0;
        mean = 0.0f;
        stdev = 0.0f;
    }

    public void Add(float newval)
    {
        data[data_counter] = newval;
        if (++data_counter >= size)
            data_counter = size;
    }

    public void UpdateStats()
    {
        float squared_sum =0.0f;
        float d = 0.0f;
        for(int i = 0; i< data_counter; i++)
        {
            mean += data[i];
        }

        if(data_counter>0)
            mean /= (float)data_counter;

        for(int i = 0; i< data_counter; i++)
        {
            d = data[i] - mean;
            squared_sum += d * d;
        }

        if ( data_counter > 1)
            squared_sum /= (float)(data_counter - 1);

        stdev = (float)Math.sqrt((double)squared_sum);
    }



}
