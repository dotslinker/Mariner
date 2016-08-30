package it.dongnocchi.mariner;

/**
 * Created by DianaM on 10/07/2015.
 */
public class MotorData {

    public final int MOTOR_ON = 1;
    public final int MOTOR_OFF = 0;

    protected long Timestamps[];
    protected short Conditions[];

    int data_counter;
    int data_size;

    final int NUM_OF_SAMPLES = 144000; // 10 Hz * 3600s * 4ore

    public MotorData(int dim)
    {
        data_size = dim;
        this.Timestamps=new long[dim];
        this.Conditions = new short[dim];
        data_counter = 0;
    }

    public MotorData()
    {
        data_size = NUM_OF_SAMPLES;
        this.Timestamps=new long[NUM_OF_SAMPLES];
        this.Conditions = new short[NUM_OF_SAMPLES];
        data_counter = 0;
    }


    public void AddData(long new_time, short new_condition)
    {
        Timestamps[data_counter] = new_time;
        Conditions[data_counter] = new_condition;

        if (++data_counter >= data_size)
            data_counter--;

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


    public void Reset()
    {
        for(int i= 0; i<data_size;i++)
        {
            Timestamps[i] = 0;
            Conditions[i] = 0;
        }
        data_counter = 0;

    }


}
