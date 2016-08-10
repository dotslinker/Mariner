package it.dongnocchi.mariner;

import android.hardware.SensorEvent;
import android.os.SystemClock;

/**
 * Created by P.Meriggi on 12/04/2016.
 */
public class TriaxialData {

    //TODO: verificare le seguenti

    //static final int NUM_OF_SMPLES = 720000; //= 4 ore * 50 Hz * 3600 secondi
    //static final float CONV_COEF = 1.0f / 50000000.0f;

    public int Timestamps[]; //expressed in 50us (1ms / 20)
    public float X[];
    public float Y[];
    public float Z[];

    float bias_x, bias_y, bias_z;

    public float min_x, max_x;
    public float min_y, max_y;
    public float min_z, max_z;

    public int data_counter;
    public int data_array_size;
    public int data_acquired;

    public long reference_time;
    //public long last_timestamp;

    float dt;

    public TriaxialData(int dim, long _new_ref_time)
    {
        data_array_size = dim;
        this.Timestamps=new int[dim];
        this.X=new float[dim];
        this.Y=new float[dim];
        this.Z=new float[dim];

        min_x = 1000.0f;
        min_y = 1000.0f;
        min_z = 1000.0f;

        max_x = -1000.0f;
        max_y = -1000.0f;
        max_z = -1000.0f;

        //reference_time = _new_ref_time;
    }

    public TriaxialData(int dim)
    {
        data_array_size = dim;
        this.Timestamps=new int[dim];
        this.X=new float[dim];
        this.Y=new float[dim];
        this.Z=new float[dim];

        min_x = 1000.0f;
        min_y = 1000.0f;
        min_z = 1000.0f;

        max_x = -1000.0f;
        max_y = -1000.0f;
        max_z = -1000.0f;

        reference_time = SystemClock.elapsedRealtime();
    }


    public TriaxialData(long _new_ref_time, int num_of_samples)
    {
        data_array_size = num_of_samples;
        this.Timestamps=new int[num_of_samples];
        this.X=new float[num_of_samples];
        this.Y=new float[num_of_samples];
        this.Z=new float[num_of_samples];

        min_x = 1000.0f;
        min_y = 1000.0f;
        min_z = 1000.0f;

        max_x = -1000.0f;
        max_y = -1000.0f;
        max_z = -1000.0f;

        reference_time = _new_ref_time;
    }



    public void ResetData(long _new_ref_time)
    {
        data_counter = 0;
        //reference_time = _new_ref_time;
        data_acquired = 0;

        min_x = 1000.0f;
        min_y = 1000.0f;
        min_z = 1000.0f;

        max_x = -1000.0f;
        max_y = -1000.0f;
        max_z = -1000.0f;

        reference_time = _new_ref_time;

        //TODO: verify the need to actually clear array data
    }

    public void ResetData()
    {

        data_counter = 0;
        //reference_time = _new_ref_time;
        data_acquired = 0;

        min_x = 1000.0f;
        min_y = 1000.0f;
        min_z = 1000.0f;

        max_x = -1000.0f;
        max_y = -1000.0f;
        max_z = -1000.0f;

        reference_time = SystemClock.elapsedRealtime();

        //TODO: verify the need to actually clear array data
    }




    public void AddData(SensorEvent event)
    {




    }


    public void AddData(float x, float y, float z)
    {
        //TODO: verificare la ragione del *20 nella formula sottostante
        Timestamps[data_counter] = (int) ((SystemClock.elapsedRealtime() - reference_time)*20);
        X[data_counter] = x - bias_x;
        Y[data_counter] = y - bias_y;
        Z[data_counter] = z - bias_z;

        //aggiorno massimo e minimo
        if( min_x > x)
            min_x = x;

        if( min_y > y)
            min_y = y;

        if( min_z > z)
            min_z = z;

        if( max_x < x)
            max_x = x;

        if( max_y < y)
            max_y = y;

        if( max_z < z)
            max_z = z;

        if(++data_counter >= data_array_size)
            data_counter = data_array_size -1;

        data_acquired++;
    }

    //==========================================================================
    float getMean(int axis){
        //==========================================================================
        float sum = 0;
        float mean = 0;
        if (data_counter >0 ) {
            switch (axis) {
                case 0:
                    for (int i = 0; i < data_counter; i++)
                        sum += X[i];

                    break;

                case 1:
                    for (int i = 0; i < data_counter; i++)
                        sum += Y[i];

                    break;

                case 2:
                    for (int i = 0; i < data_counter; i++)
                        sum += Z[i];

                    break;
            }
            mean = sum / (float) data_counter;
        }
        return mean;
    }

    //==========================================================================
    float getStdDev(int axis) {
        //==========================================================================

        float temp = 0;
        float mean = getMean(axis);
        float variance = 0;
        float StdDev = 0;
        if (data_counter > 0) {
            switch (axis) {
                case 0:
                    for (int i = 0; i < data_counter; i++)
                        temp += (mean - X[i]) * (mean - X[i]);

                    break;

                case 1:
                    for (int i = 0; i < data_counter; i++)
                        temp += (mean - Y[i]) * (mean - Y[i]);

                    break;

                case 2:
                    for (int i = 0; i < data_counter; i++)
                        temp += (mean - Z[i]) * (mean - Z[i]);

                    break;
            }

            variance = temp / (float)(data_counter);
            StdDev = (float) Math.sqrt(variance);
        }
        return StdDev;
    }

    public void UpdateBias()
    {
        bias_x = getMean(0);
        bias_y = getMean(1);
        bias_z = getMean(2);
    }

}
