package it.dongnocchi.mariner;

/**
 * Created by DianaM on 10/07/2015.
 */
public class BatteryData {
    protected long Time[];
    protected int BatLev[];

    public BatteryData(int dim){
        this.Time = new long[dim];
        this.BatLev = new int[dim];
    }

}
