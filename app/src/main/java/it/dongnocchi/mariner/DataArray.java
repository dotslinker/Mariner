package it.dongnocchi.mariner;

/**
 * Created by Paolo on 11/08/2016.
 */
public class DataArray {
    float [] data;
    int data_counter;
    public int data_stored;
    public float mean;
    public float stdev;
    int size;
    int size_1;

    public DataArray(int _size)
    {
        size = _size;
        if (size > 0)
            size_1 = size -1;

        data = new float[size];
        data_counter = 0;
    }

    public void reset_data()
    {
        data_counter = 0;
        data_stored = 0;
        for (int i = 0; i< size; i++)
            data[i] = 0;
        mean = 0.0f;
        stdev = 0.0f;
    }

    public void Add(float newval)
    {
        data[data_counter] = newval;
        if (++data_counter >= size)
            data_counter = size_1;

        data_stored++;
    }

    public void UpdateStats()
    {
        float squared_sum =0.0f;
        float d = 0.0f;

        int dimension;

        if( data_counter < size)
            dimension = data_counter;
        else
            dimension = size;

        for(int i = 0; i< dimension; i++)
        {
            mean += data[i];
        }

        if(dimension>0)
            mean /= (float)dimension;

        for(int i = 0; i< dimension; i++)
        {
            d = data[i] - mean;
            squared_sum += d * d;
        }

        if ( dimension > 1)
            squared_sum /= (float)(dimension - 1);

        stdev = (float)Math.sqrt((double)squared_sum);
    }



}
