package it.dongnocchi.mariner;

import android.hardware.SensorEvent;

/**
 * Created by Paolo on 12/04/2016.
 */
public class AccelerometerData extends TriaxialData {

    float last_acc_x;
    public float velocity_x;
    float delta_x;
    long last_acc_timestamp;


    public AccelerometerData(int dim, long _new_ref_time) {
        super(dim, _new_ref_time);

    }

    public void UpdateAccData(SensorEvent event) {

        float new_acc_x, new_acc_y, new_acc_z;

        //evito che il primo delta t sia enorme
        if( last_acc_timestamp == 0 )
            last_acc_timestamp = event.timestamp;

        //float dt = (float)(event.timestamp - last_acc_timestamp) * NS2S;

        new_acc_x = event.values[0] - bias_x;
        new_acc_y = event.values[1] - bias_y;
        new_acc_z = event.values[2] - bias_z;



    }
}