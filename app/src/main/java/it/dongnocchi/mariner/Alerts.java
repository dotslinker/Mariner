package it.dongnocchi.mariner;

/**
 * Created by Paolo on 18/05/2017.
 */

public class Alerts {
    public boolean TemperatureHigh;
    public boolean RAMLow;
    public boolean StorageSpaceLow;
    public boolean BatteryLow;

    public Alerts()
    {
        TemperatureHigh = false;
        RAMLow = false;
        StorageSpaceLow = false;
        BatteryLow = false;
    }

    public void Reset()
    {
        TemperatureHigh = false;
        RAMLow = false;
        StorageSpaceLow = false;
        BatteryLow = false;
    }

}


