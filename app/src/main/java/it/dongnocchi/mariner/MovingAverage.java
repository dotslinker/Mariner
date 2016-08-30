package it.dongnocchi.mariner;

/**
 * Created by Paolo on 10/03/2016.
 */
public class MovingAverage {

        public float [] data;
        int data_counter;
        public float MeanVal;
        int dimension = 0;
        double ratio;
        boolean firstval = true;

        public MovingAverage(int size)
        {
            dimension = size;
            data = new float[dimension];

            ratio = 1.0 / (float)dimension;
        }


        public float UpdateValue(float val)
        {
            if (firstval)
            {
                MeanVal = val;

                if (val != 0)
                {
                    //inizializzo larray con tutti i valori uguali
                    for (int k = 0; k < dimension; k++)
                    {
                        data[k] = val;
                    }

                    firstval = false;
                }
            }
            else
            {
                MeanVal -= data[data_counter] * ratio;
                data[data_counter] = val;
                MeanVal += val * ratio;

                if(++data_counter >= dimension)
                    data_counter = 0;
            }

            return MeanVal;

        }

        public void ResetData()
        {
            firstval = true;
            MeanVal = 0.0f;
        }

    }
