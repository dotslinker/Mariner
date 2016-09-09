package it.dongnocchi.mariner;

/**
 * Created by Paolo on 30/08/2016.
 */
public class EventData {

    public static final int MOTOR_ON = 20;
    public static final int MOTOR_OFF = 21;

    public static final int POWER_ON = 10;
    public static final int POWER_OFF = 11;

    public static final int BATTERY_FULL = 30;
    public static final int BATTERY_LOW = 31;
    public static final int BATTERY_HALF = 32;

    public boolean EventArrayFull = false;

    public int Timestamps[];
    public int EventArray[];

    int data_counter;
    int data_size;

        final int NUM_OF_SAMPLES = 86400; // 2 Hz * 3600s * 12 ore

        public EventData(int dim)
        {
            data_size = dim;
            this.Timestamps = new int [dim];
            this.EventArray = new int [dim];
            data_counter = 0;
        }

        public EventData()
        {
            data_size = NUM_OF_SAMPLES;
            this.Timestamps=new int [NUM_OF_SAMPLES];
            this.EventArray = new int[NUM_OF_SAMPLES];
            data_counter = 0;
        }


        public void AddData(int new_time, int new_event)
        {
            Timestamps[data_counter] = new_time;
            EventArray[data_counter] = new_event;


            if (++data_counter >= data_size) {
                data_counter--;
                EventArrayFull = true;
            }
        }

        public long getCurrentTimestamp()
        {
            //if(data_counter > 0)
            return Timestamps[data_counter];
        }

        public int getCurrentValue()
        {
            return EventArray[data_counter];
        }


        public void Reset()
        {
            for(int i= 0; i<data_size;i++)
            {
                Timestamps[i] = 0;
                EventArray[i] = 0;
            }
            data_counter = 0;
            EventArrayFull = false;

        }

}
