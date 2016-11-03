package it.dongnocchi.mariner;

import android.hardware.SensorEvent;

/**
 * Created by Paolo on 10/03/2016.
 */
public class InertialData {

    static final int MA_SIZE = 10;
    static final float NS2S = 1.0f / 1000000000.0f;
    static final float THRESHOLD_ACC = 0.01f;
    static final float THRESHOLD_GYRO = 0.001f;
    final int NUM_OF_SMPLES = 720000; //= 4 ore * 50 Hz * 3600 secondi
    public float[] AccXDataArray;
    public float[] AccYDataArray;
    public float[] AccZDataArray;
    public int[] AccTimestampArray; //expressed in 100us (1ms / 10)
    public float[] GyroXDataArray;
    public float[] GyroYDataArray;
    public float[] GyroZDataArray;
    public int[] GyroTimestampArray;
    public int acc_data_counter;
    public int gyro_data_counter;
    public float m_acc_x, m_acc_y, m_acc_z;
    public float[] min_acc_values;
    public float[] max_acc_values;
    public float[] min_gyro_values;
    public float[] max_gyro_values;
    public float[] acc_offset;
    public float[] gyro_offset;
    public float velocity_x;

    //float[] acc_bias;
    //float[] gyro_bias;
    public float DailyDistanceCoveredFw;
    public float DailyDistanceCoveredBw;
    public float DailyAngleCoveredR;
    public float DailyAngleCoveredL;
    public float HourlyDistanceCovered;
    public float HourlyAngleCovered;
    //TODO: verificare che 50 sia la dimensione corretta per trenta secondi (da correggere)
    //public TriaxialData Acc_30s_data = new TriaxialData(50);       // 1500 samples = 30 s of 50Hz acquisition
    public float Acc_Mean_X = 0;
    public float Acc_Mean_Y = 0;
    public float Acc_Mean_Z = 0;
    public float Acc_DevStd_X = 0;
    public float Acc_DevStd_Y = 0;
    public float Acc_DevStd_Z = 0;
    MovingAverage ma_acc_x, ma_acc_y, ma_acc_z;
    MedianFilter3 mf_acc_x, mf_acc_y, mf_acc_z;
    MedianFilter3 mf_gyro_x, mf_gyro_y, mf_gyro_z;
    float last_acc_x;
    float delta_x;
    long last_acc_timestamp;
    float last_gyro_x, last_gyro_y, last_gyro_z;
    float delta_omega_z;
    float angle_z;
    long last_gyro_timestamp;
    private long reference_time;

    //
    public InertialData() {
        ma_acc_x = new MovingAverage(MA_SIZE);
        ma_acc_y = new MovingAverage(MA_SIZE);
        ma_acc_z = new MovingAverage(MA_SIZE);

        mf_acc_x = new MedianFilter3();
        mf_acc_y = new MedianFilter3();
        mf_acc_z = new MedianFilter3();

        mf_gyro_x = new MedianFilter3();
        mf_gyro_y = new MedianFilter3();
        mf_gyro_z = new MedianFilter3();

        AccXDataArray = new float[NUM_OF_SMPLES];
        AccYDataArray = new float[NUM_OF_SMPLES];
        AccZDataArray = new float[NUM_OF_SMPLES];

        GyroXDataArray = new float[NUM_OF_SMPLES];
        GyroYDataArray = new float[NUM_OF_SMPLES];
        GyroZDataArray = new float[NUM_OF_SMPLES];

        AccTimestampArray = new int[NUM_OF_SMPLES];
        GyroTimestampArray = new int[NUM_OF_SMPLES];

        min_acc_values = new float[3];
        max_acc_values = new float[3];

        min_gyro_values = new float[3];
        max_gyro_values = new float[3];

        acc_offset = new float[3];
        gyro_offset = new float[3];

    }

    public void UpdateAccData(SensorEvent event) {
        float new_acc_x, new_acc_y, new_acc_z;
        float mf_ax, mf_ay, mf_az;

        //evito che il primo delta t sia enorme
        if (last_acc_timestamp == 0)
            last_acc_timestamp = event.timestamp;

        float dt = (float) (event.timestamp - last_acc_timestamp) * NS2S;

        //TODO: verificare che questo sia il migliore compromesso tra velocità
        //      e precisione, e che non sia necessario usare una rotazione matriciale
        new_acc_x = event.values[0] - acc_offset[0];
        new_acc_y = event.values[1] - acc_offset[1];
        new_acc_z = event.values[2] - acc_offset[2];

        mf_ax = mf_acc_x.UpdateValue(new_acc_x);
        mf_ay = mf_acc_y.UpdateValue(new_acc_y);
        mf_az = mf_acc_z.UpdateValue(new_acc_z);

        m_acc_x = ma_acc_x.UpdateValue(mf_ax);
        m_acc_y = ma_acc_y.UpdateValue(mf_ay);
        m_acc_z = ma_acc_z.UpdateValue(mf_az);

        //testo massimo e minimo
        if (min_acc_values[0] > new_acc_x)
            min_acc_values[0] = new_acc_x;

        if (min_acc_values[1] > new_acc_y)
            min_acc_values[1] = new_acc_y;

        if (min_acc_values[2] > new_acc_z)
            min_acc_values[2] = new_acc_z;

        if (max_acc_values[0] < new_acc_x)
            max_acc_values[0] = new_acc_x;

        if (max_acc_values[1] < new_acc_y)
            max_acc_values[1] = new_acc_y;

        if (max_acc_values[2] < new_acc_z)
            max_acc_values[2] = new_acc_z;

        //TODO: verificare se sia il acso di memorizzare i dati con o senza offset
        AccXDataArray[acc_data_counter] = new_acc_x;
        AccYDataArray[acc_data_counter] = new_acc_y;
        AccZDataArray[acc_data_counter] = new_acc_z;

        AccTimestampArray[acc_data_counter] = (int) ((event.timestamp - reference_time) / 100000L);

        if (++acc_data_counter >= NUM_OF_SMPLES)
            acc_data_counter = NUM_OF_SMPLES - 1;

        //Start the integration only if above threshold, using the MA value
        if (Math.abs(m_acc_x) < THRESHOLD_ACC)
            m_acc_x = 0;

        if (dt > 0) {
            velocity_x += (m_acc_x + last_acc_x) / 2.0f * dt;
            delta_x = velocity_x * dt;
        } else {
            velocity_x = 0;
            delta_x = 0;
        }

        if (delta_x > 0) {
            DailyDistanceCoveredFw += delta_x;
        } else {
            DailyDistanceCoveredBw += Math.abs(delta_x);
        }

        //TODO: verificare se ci sia da inserire anche un movimento complessivo
        // che valuti il modulo dello spostamento
        HourlyDistanceCovered += delta_x;

        last_acc_x = m_acc_x;
        last_acc_timestamp = event.timestamp;
    }


    public void UpdateGyroData(SensorEvent event) {
        float new_gyro_x, new_gyro_y, new_gyro_z;

        //evito che il primo delta t sia enorme
        if (last_gyro_timestamp == 0)
            last_gyro_timestamp = event.timestamp;

        float dt = (float) (event.timestamp - last_gyro_timestamp) * NS2S;

        //TODO: da verificare ed eventualmente modificare
        new_gyro_x = event.values[0] - gyro_offset[0];
        new_gyro_y = event.values[1] - gyro_offset[1];
        new_gyro_z = event.values[2] - gyro_offset[2];

        //testo massimo e minimo
        if (min_gyro_values[0] > new_gyro_x)
            min_gyro_values[0] = new_gyro_x;

        if (min_gyro_values[1] > new_gyro_y)
            min_gyro_values[1] = new_gyro_y;

        if (min_gyro_values[2] > new_gyro_z)
            min_gyro_values[2] = new_gyro_z;

        if (max_gyro_values[0] < new_gyro_x)
            max_gyro_values[0] = new_gyro_x;

        if (max_gyro_values[1] < new_gyro_y)
            max_gyro_values[1] = new_gyro_y;

        if (max_gyro_values[2] < new_gyro_z)
            max_gyro_values[2] = new_gyro_z;

        AccXDataArray[gyro_data_counter] = new_gyro_x;
        AccYDataArray[gyro_data_counter] = new_gyro_y;
        AccZDataArray[gyro_data_counter] = new_gyro_z;

        AccTimestampArray[gyro_data_counter] = (int) ((event.timestamp - reference_time) / 100000L);

        if (++gyro_data_counter >= NUM_OF_SMPLES)
            gyro_data_counter = NUM_OF_SMPLES - 1;

        //verifico se l'accelerazione è sopra soglia
        //altrimenti l'azzero
        if (Math.abs(new_gyro_z) < THRESHOLD_GYRO)
            new_gyro_z = 0;

        if (dt > 0) {
            //velocity_x += (new_gyro_x + last_gyro_x) / 2.0f * dt;
            delta_omega_z = (new_gyro_z + last_gyro_z) / 2.0f * dt * 180 / 3.14159265359f;
        } else {
            //velocity_x = 0;
            delta_omega_z = 0;
        }

        angle_z += delta_omega_z;

        if (delta_omega_z > 0) {
            DailyAngleCoveredL += delta_omega_z;
        } else {
            DailyAngleCoveredR += Math.abs(delta_omega_z);
        }

        HourlyAngleCovered += delta_omega_z;

        last_gyro_x = new_gyro_x;
        last_gyro_y = new_gyro_y;
        last_gyro_z = new_gyro_z;

        last_gyro_timestamp = event.timestamp;

    }

    public void ResetIntegral() {
        velocity_x = 0;
        last_acc_x = 0;

        last_gyro_z = 0;
    }


    public void UpdateAccDataCalibration(SensorEvent event) {


    }

    public void ResetDailyData(long _new_ref_time) {
        //TODO: verificare se ci siano altre grandezze da resettare
        reference_time = _new_ref_time;

        last_acc_timestamp = 0;
        last_gyro_timestamp = 0;

        last_acc_x = 0;
        last_gyro_x = 0;

        velocity_x = 0;
        angle_z = 0;

        DailyDistanceCoveredFw = 0;
        DailyDistanceCoveredBw = 0;
        DailyAngleCoveredR = 0;
        DailyAngleCoveredL = 0;

        acc_data_counter = 0;
        gyro_data_counter = 0;

        //TODO: verificare se serve resettare anche questi
        HourlyAngleCovered = 0;
        HourlyDistanceCovered = 0;


        //TODO: verificare se sia eventualmente necessario azzerare il tutto
        if (false) {
            for (int i = 0; i < NUM_OF_SMPLES; i++) {
                AccXDataArray[i] = 0;
                AccYDataArray[i] = 0;
                AccZDataArray[i] = 0;

                GyroXDataArray[i] = 0;
                GyroYDataArray[i] = 0;
                GyroZDataArray[i] = 0;

                AccTimestampArray[i] = 0;
                GyroTimestampArray[i] = 0;
            }
        }
    }

    public void ResetHourlyData() {
        HourlyDistanceCovered = 0;
        HourlyAngleCovered = 0;
    }

    public void ResetMinMaxValues() {
        for (int i = 0; i < 3; i++) {
            min_acc_values[i] = 0;
            max_acc_values[i] = 0;

            min_gyro_values[i] = 0;
            max_gyro_values[i] = 0;
        }
    }

    public void UpdateBias(float ax, float ay, float az, float gx, float gy, float gz) {
        acc_offset[0] = ax;
        acc_offset[1] = ay;
        acc_offset[2] = az;

        gyro_offset[0] = gx;
        gyro_offset[1] = gy;
        gyro_offset[2] = gz;
    }


/*
    //==========================================================================
    //private void Phone_CheckAxes(){
    public void SetAccBias(){
    //==========================================================================
        // checks means and std dev then returns to init activity after having sent message to azure
        Acc_Mean_X = getMean(Acc_30s_data.X);
        Acc_Mean_Y = getMean(Acc_30s_data.Y);
        Acc_Mean_Z = getMean(Acc_30s_data.Z);

        Acc_DevStd_X = getStdDev(Acc_Mean_X, Acc_30s_data.X);
        Acc_DevStd_Y = getStdDev(Acc_Mean_Y, Acc_30s_data.Y);
        Acc_DevStd_Z = getStdDev(Acc_Mean_Z, Acc_30s_data.Z);

        //acc_bias[0] = Acc_Mean_X;
        //acc_bias[1] = Acc_Mean_Y;
        //acc_bias[2] = 9.81f + Acc_Mean_Z;

        //TODO: verificare che non ci sia nulla da salvsare
        /*
        if(NumOfWCActivation_Current == NumOfWCActivation) {
            String MsgTSend = "Mean_X" + ":" + Acc_Mean_X + ";" + "Mean_Y" + ":" + Acc_Mean_Y + ";" + "Mean_Z" + ":" + Acc_Mean_Z + ";" +
                    "StdDev_X" + ":" + Acc_DevStd_X + ";" + "StdDev_Y" + ":" + Acc_DevStd_Y + ";" + "StdDev_Z" + ":" + Acc_DevStd_Z + ";";
            //myEventManager.SendEvent(MsgTSend, 0);
        } else{
            String MsgTSend = "calibration failed";
            //myEventManager.SendEvent(MsgTSend, 0);
        }
        */

//    }

    //==========================================================================
    float getMean(float[] data) {
        //==========================================================================
        float sum = 0;
        int i;
        for (i = 0; i < data.length; i++) {
            sum += data[i];
        }
        float mean = sum / data.length;
        return mean;
    }

    //==========================================================================
    float getStdDev(float mean, float[] data) {
        //==========================================================================
        double temp = 0;
        for (double a : data)
            temp += (mean - a) * (mean - a);

        float variance = (float) temp / (data.length);
        float StdDev = (float) Math.sqrt(variance);
        return StdDev;
    }

}
