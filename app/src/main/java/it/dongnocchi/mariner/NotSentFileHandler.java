package it.dongnocchi.mariner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by DianaM on 06/10/2015.
 */
public class NotSentFileHandler {
    private String FileName;
    private String Temp_FileName;
    private String FilePath;
    private String TempFilePath;
    FileOutputStream outputStream;
    //Configuration keys;
    private List<String> records;

    public NotSentFileHandler(String path){
        //keys = new Configuration();
        FileName = "FilesToSend.txt";
        Temp_FileName = "FilesToSend_tmp.txt";
        FilePath = path + FileName;
        TempFilePath = path + Temp_FileName;
        records = new ArrayList<>();
    }

    //==========================================================================
    public void AppendNewLine(String newLine){
    //==========================================================================
    // newfilename è il nome del file con il percorso
    // aggiunge una vuova riga con il nome dell'ultimo   file non inviato
        newLine += "\n";

        try {
            outputStream = new FileOutputStream(FilePath, true);
            outputStream.write(newLine.getBytes());
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //==========================================================================
    public String LoadLastLine(){
    //==========================================================================
        // ritorna l'ultima linea del file contenente i nomi dei file non inviati
        String currentLine;
        String trimmedLine = "";

        try {
            BufferedReader reader = new BufferedReader(new FileReader(FilePath));

            while((currentLine = reader.readLine()) != null) {
                trimmedLine = currentLine.trim();
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return trimmedLine;
    }

    //==========================================================================
    public String LoadLineNumber( int LineNumber){
    //==========================================================================
        // FIRST LINE = 0
        String currentLine;
        String trimmedLine = "";

        try {
            BufferedReader reader = new BufferedReader(new FileReader(FilePath));
            int i = 0;
            for ( i=0; i<=LineNumber; i++){
                if ( (currentLine = reader.readLine()) != null ){
                    trimmedLine = currentLine.trim();
                }
            }
            reader.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return trimmedLine;
    }

    //==========================================================================
    public boolean DeleteLine(String textToRemove){
    //==========================================================================
    // cerca e elimina la linea da rimuovere all'interno di tutto il file
        File inputFile = new File(FilePath);
        File tempFile = new File(TempFilePath);

        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(inputFile));
            writer = new BufferedWriter(new FileWriter(tempFile));

            if (inputFile.exists()) {
                String currentLine;

                while ((currentLine = reader.readLine()) != null) {
                    File FileToDelete = new File(currentLine);
                    if (FileToDelete.exists()) {
                        // trim newline when comparing with lineToRemove
                        String trimmedLine = currentLine.trim();
                        if (trimmedLine.equals(textToRemove)) continue; // se è ugule al testo da rimuovere non lo copio
                        writer.write(currentLine + System.getProperty("line.separator")); // altrimenti lo copio
                    }
                }
                writer.close();
                reader.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean successful = tempFile.renameTo(inputFile);
        return successful;
    }

    //==========================================================================
    public List<String> ReadTheWholeFile(){
    //==========================================================================
        BufferedReader bufferedReader;
        String line;
        records = new ArrayList<>();
        try {
            bufferedReader = new BufferedReader(new FileReader(FilePath));
            // the readLine method returns null when there is nothing else to read.
            while ((line = bufferedReader.readLine()) != null) {
                //legge dalla prima all'ultima riga
                records.add(line);
            }
            bufferedReader.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }
}
