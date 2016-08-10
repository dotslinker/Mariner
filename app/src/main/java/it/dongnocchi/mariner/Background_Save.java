package it.dongnocchi.mariner;

import android.os.AsyncTask;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

/**
 * Created by DianaM on 10/07/2015.
 */
//==========================================================================
public class Background_Save extends AsyncTask<Void, Boolean, Boolean> {
    //==========================================================================
    // buffers dimensions ( amount of samples to be saved each time)
    static final short buffer_dim_inert =       1000;
    static final short buffer_dim_batt_motor =  1;

    TriaxialData acc_data =          new TriaxialData(buffer_dim_inert);
    TriaxialData gyro_data =         new TriaxialData(buffer_dim_inert);
    MotorData motor_data =        new MotorData(buffer_dim_batt_motor);
    BatteryData battery_data =    new BatteryData(buffer_dim_batt_motor);
    MotorData wheelchair_data =   new MotorData(buffer_dim_batt_motor);

    String FilePath ="";
    public static final short Motor_ID      = 0;
    public static final short Acc_ID        = 1;
    public static final short Gyro_ID       = 2;
    public static final short Battery_ID    = 3;

    byte[] EightBytes = new byte[8];
    byte[] FourBytes = new byte[4];
    byte[] TwoBytes = new byte[2];


    // constructor
    //==========================================================================
    public Background_Save(MotorData motor_in_data,
                           TriaxialData acc_in_data,
                           TriaxialData gyro_in_data,
                           BatteryData battery_in_data,
                           MotorData wheelchair_in_data,
                           String           inFilePath) {
        //==========================================================================
        motor_data =        motor_in_data;
        acc_data =          acc_in_data;
        gyro_data =         gyro_in_data;
        battery_data =      battery_in_data;
        wheelchair_data =   wheelchair_in_data;
        FilePath =          inFilePath;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }
    @Override
    //==========================================================================
    protected Boolean doInBackground(Void... params) {
        //==========================================================================
        String StringToSave="";
        String BinaryFilePath = FilePath.replace( "txt", "bin"); // changes file extension by replacing part of the string
        FileOutputStream outputStream;
        FileOutputStream BinaryOutputStream;

        // open binary file
        try {
            BinaryOutputStream = new FileOutputStream(BinaryFilePath, true); //true: append to file
            BufferedOutputStream out = new BufferedOutputStream(BinaryOutputStream);//,numOfBytesInTheBuffer);

            // fulfill string
            if (acc_data!=null) {
                for(int i=0; i<acc_data.Timestamps.length; i++){
                    StringToSave += acc_data.Timestamps[i] + "\t" + acc_data.X[i] + "\t" + acc_data.Y[i] + "\t" + acc_data.Z[i] + "\n";

                    EightBytes = getBytesFromAny(acc_data.Timestamps[i], "long");
                    out.write(EightBytes, 0, 8);
                    FourBytes = getBytesFromAny((long)acc_data.X[i],"float");
                    out.write(FourBytes, 0, 4);
                    FourBytes = getBytesFromAny((long)acc_data.Y[i],"float");
                    out.write(FourBytes, 0, 4);
                    FourBytes = getBytesFromAny((long)acc_data.Z[i],"float");
                    out.write(FourBytes, 0, 4);
                    out.flush();
                }

            }else if(gyro_data!=null){
                for(int i=0; i<gyro_data.Timestamps.length; i++){
                    StringToSave += gyro_data.Timestamps[i] + "\t" + gyro_data.X[i] + "\t" + gyro_data.Y[i] + "\t" + gyro_data.Z[i] + "\n";

                    EightBytes = getBytesFromAny(gyro_data.Timestamps[i], "long");
                    out.write(EightBytes, 0, 8);
                    FourBytes = getBytesFromAny((long)gyro_data.X[i],"float");
                    out.write(FourBytes, 0, 4);
                    FourBytes = getBytesFromAny((long)gyro_data.Y[i],"float");
                    out.write(FourBytes, 0, 4);
                    FourBytes = getBytesFromAny((long)gyro_data.Z[i],"float");
                    out.write(FourBytes, 0, 4);
                    out.flush();
                }

            }else if(motor_data!=null){
                /*
                for(int i=0; i<motor_data.Time.length; i++){
                    StringToSave += motor_data.Time[i] + "\t" + motor_data.Status[i] + "\n";

                    EightBytes = getBytesFromAny(motor_data.Time[i], "long");
                    out.write(EightBytes, 0, 8);
                    TwoBytes = getBytesFromAny(motor_data.Status[i], "short");
                    out.write(TwoBytes, 0, 2);
                    out.flush();
                }
                */
            }else if(battery_data!=null){
                for(int i=0; i<battery_data.Time.length; i++){
                    StringToSave += battery_data.Time[i] + "\t" + battery_data.BatLev[i] + "\n";

                    EightBytes = getBytesFromAny(battery_data.Time[i], "long");
                    out.write(EightBytes, 0, 8);
                    FourBytes = getBytesFromAny(battery_data.BatLev[i], "int");
                    out.write(FourBytes, 0, 4);
                    out.flush();
                }
            }
            else if(wheelchair_data!=null){
                /*
                for(int i=0; i<wheelchair_data.Time.length; i++){
                    StringToSave += wheelchair_data.Time[i] + "\t" + wheelchair_data.Status[i] + "\n";

                    EightBytes = getBytesFromAny(wheelchair_data.Time[i], "long");
                    out.write(EightBytes, 0, 8);
                    TwoBytes = getBytesFromAny(wheelchair_data.Status[i], "short");
                    out.write(TwoBytes, 0, 2);
                    out.flush();
                }
                */

            }

            BinaryOutputStream.close(); //close binary file
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        // append string in FilePath
        try {
            outputStream = new FileOutputStream(FilePath, true); //true: append string to file (non-binary file)
            outputStream.write(StringToSave.getBytes());
            outputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

     return true;
    }

    // FROM 64, 32, 16 BIT TO 8 BIT
    // IMPORTANT: store returned array in a correct sized bytes array
    //==========================================================================
    private byte[] getBytesFromAny(long value, String DataType){
    //==========================================================================
        byte i = 0;
        byte shift = 0;
        byte dim = 0;
        switch (DataType) {
            case "long": // 64 bit
                shift = 56;
                dim = 8;
                break;
            case "int": // 32 bit
                shift = 24;
                dim = 4;
                break;
            case "float": // 32 bit
                shift = 24;
                dim = 4;
                break;
            case "short": // 16 bit
                shift = 8;
                dim = 2;
                break;
            default:
                dim = -1;
        }

        if (dim > 0) { // no errori di stringa
            byte[] ByteArray = new byte[dim];
            for (i = 0; i < dim; i++) {
                ByteArray[i] = (byte) ((value >> shift) & 0xff);
                shift -= 8;
            }
            return ByteArray; // must be 8, 4 or 2 element buffer
        } else
            return null;
    }


}
