package it.dongnocchi.mariner;

/**
 * Created by Paolo on 11/08/2016.
 */
public class TimestampedDataArray extends DataArray
{
    long [] tstamp;
    public float mean_deltatime;
    public float stdev_deltatime;

    public TimestampedDataArray(int _size)
    {
        super(_size);
        tstamp = new long[size];
    }

    @Override
    public void reset_data()
    {
        super.reset_data();
        for (int i = 0; i < size; i++)
            tstamp[i] = 0L;

    }

    public void Add(float newval, long new_timestamp)
    {
        tstamp[data_counter] = new_timestamp;
        super.Add(newval);
    }

    @Override
    public void UpdateStats()
    {
        double mean_deltatime_d = 0.0;
        double sum_squared_deltatime_d = 0.0;
        int data_counter_1 = data_counter - 1;
        double temp_d;

        super.UpdateStats();

        for (int i = 0; i < data_counter_1; i++)
        {
            mean_deltatime_d += (double) (tstamp[i+1] - tstamp[i]);
        }

        if( data_counter > 0)
        mean_deltatime_d /= (double)data_counter;

        for (int i = 0; i < data_counter_1; i++)
        {
            temp_d = ((double) (tstamp[i+1] - tstamp[i])) - mean_deltatime_d;
            sum_squared_deltatime_d += temp_d * temp_d;
        }

        if ( data_counter_1 > 1)
            sum_squared_deltatime_d /= (float)(data_counter_1 - 1);

        //Event time is expressed in nanoseconds, and we need milliseconds
        mean_deltatime_d /= 1000000.0;
        mean_deltatime = (float) mean_deltatime_d;

        stdev_deltatime = (float)(Math.sqrt(sum_squared_deltatime_d)/1000000.0);
    }




}
