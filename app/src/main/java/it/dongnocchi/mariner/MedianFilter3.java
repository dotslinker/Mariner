package it.dongnocchi.mariner;

/**
 * Created by Paolo on 02/09/2016.
 */
public class MedianFilter3 {



    //public float dMAVal = 0.0f;
    //int dimension = 2; // = n -1 places
    boolean loading = true;
    float [] data;
    int counter = 0;

    public MedianFilter3()
    {
        data = new float[3];
    }


    public float UpdateValue(float val)
    {
        data[counter++] = val;
        if (counter >= 3)
            counter = 0;

        if(loading)
        {
            if (counter > 1)
                loading = false;

            return val;
        }
        else
        {
            if(data[0] <= data[1] && data[1]<= data[2])
                return data[1];

            if(data[2] <= data[1] && data[1]<= data[0])
                return data[1];

            if(data[1] <= data[2] && data[2]<= data[0])
                return data[2];

            if(data[0] <= data[2] && data[2]<= data[1])
                return data[2];

            if(data[1] <= data[0] && data[0]<= data[2])
                return data[0];

            if(data[2] <= data[0] && data[0]<= data[1])
                return data[0];

        }

        return -1;
    }
}

