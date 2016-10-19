package it.dongnocchi.mariner;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Xml;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;

import org.xmlpull.v1.XmlSerializer;

/**
 * Created by DianaM on 27/08/2015.
 */

public class AzureManager {

    public static String storageConnectionString;
    public List<String> FilesToSend = new ArrayList<>(); //not sent files memory
    public List<String> FilesToDownload = new ArrayList<>();
    Context mcontext;
    //TODO: verificare l'utilizzo del livello di batteria
    int BatLev;
    private final int BATTERY_LOW_THRESHOLD = 10;  //espresso in percentuale
    private boolean isBusy = false;

    Configuration myConfig;

    String ApkFileToUpdate = "";

    protected final int COMMAND_UPDATE_APK = -1;
    protected final int COMMAND_SIMPLE_TRANSFER = 0;

    public AsyncResponse upload_delegate;

    NotSentFileHandler notSentFileHandler;

    // costruttore*/
    public AzureManager(Context AppContext, AsyncResponse in_upload_delegate, Configuration mc) {

        upload_delegate = in_upload_delegate;
        myConfig = mc;
        storageConnectionString = myConfig.get_storageConnectionString();
        mcontext = AppContext;
        notSentFileHandler = new NotSentFileHandler(myConfig.get_Wheelchair_path());
    }

    //==========================================================================
    //protected void UploadBlobs(int battery){//String FileInternalPath, String FileName, String BlobContainer, int battery){
    protected void UploadBlobs(){//String FileInternalPath, String FileName, String BlobContainer, int battery){
    // ==========================================================================
        // FileInternalPath = local folder where the file is located
        // FileName: name of the file, like "foto.png"
        //BatLev = battery;

        String LastLine = notSentFileHandler.LoadLastLine();
        if (!LastLine.equals("")) {
            String container = myConfig.get_Acquisition_Container();
            String folder = myConfig.get_Acquisition_Folder();
            String name = LastLine.substring(folder.length(), LastLine.length());

            // Size_FilesToSend = 0 se stiamo caricando un nuovo file
            //                  = FilesToSend.size se stiamo caricando vecchi file dalla lista
            //UploadBlobs_Async prova = new UploadBlobs_Async(FileInternalPath, FileName, BlobContainer, COMMAND_SIMPLE_TRANSFER);
            UploadBlobs_Async UploadBlobs = new UploadBlobs_Async(folder, name, container, COMMAND_SIMPLE_TRANSFER);
            UploadBlobs.execute();
        }
    }

    //==========================================================================
    private class UploadBlobs_Async extends AsyncTask<Void, Boolean, String> {
    //==========================================================================
        String FileInternalPath_local = "";
        String FileName_local = "";
        String BlobContainer = "";
        String File_FullPath="";
        int Size_FilesToSend=0;

        // Size_FilesToSend = 0 se stiamo caricando un nuovo file
        //                  = FilesToSend.size se stiamo caricando vecchi file dalla lista
        public UploadBlobs_Async(String FileInternalPath, String FileName, String in_BlobContainer, int IN_Size_FilesToSend){
            FileInternalPath_local = FileInternalPath;
            FileName_local = FileName;
            BlobContainer = in_BlobContainer;
            Size_FilesToSend = IN_Size_FilesToSend;
        }

        @Override
        protected String doInBackground(Void... params) {
            File_FullPath = FileInternalPath_local + FileName_local;

            if(!isBusy) {
                if (isNetworkOnline()) {
                    try {
                        isBusy = true;

                        // Retrieve storage account from connection-string.
                        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
                        // Create the blob client.
                        CloudBlobClient serviceClient = storageAccount.createCloudBlobClient();// get the reference to the container to be used

                        // Get a reference to the container where you want the blob to be saved
                        // Container name must be lower case. ( or it will turn BAD ERROR, error code 400)
                        CloudBlobContainer container = serviceClient.getContainerReference(BlobContainer);
                        container.createIfNotExists();

                        // Upload a file.
                        CloudBlockBlob blob = container.getBlockBlobReference(FileName_local);// i.e. "foto.png"
                        File sourceFile = new File(File_FullPath);// "C:\\Users\\Desktop\\foto.png");
                        blob.upload(new FileInputStream(sourceFile), sourceFile.length());
                        return null;

                    } catch (Exception e) {
                        //System.out.print("Exception encountered: ");
                        //System.out.println(e.getMessage());

                        FileLog.e("", "UploadBlobs_Async", e);
                        return File_FullPath;
                    }
                } else { // non online
                    return File_FullPath;
                }
            }
            else{
                return File_FullPath;
            }
        }
        @Override
        protected void onPostExecute(String res){ // post execute del UPLOAD BLOB
            isBusy = false;
            boolean isAllOK = false;

            if (res!=null){
                isAllOK = false;
                upload_delegate.processFinish(null);
               /* // qualcosa è andato storto, accoda file non inviati nella lista

                FilesToSend.add(FilesToSend.size(), FileInternalPath_local);
                FilesToSend.add(FilesToSend.size(), FileName_local);               //this will add string at the next index
                FilesToSend.add(FilesToSend.size(), BlobContainer);*/
            } else{
                /*if (Size_FilesToSend > 0) {
                    // qui si entra quando sono stati caricati correttamente vecchi file in attesa
                    FilesToSend.remove(Size_FilesToSend-1);
                    FilesToSend.remove(Size_FilesToSend-2);
                    FilesToSend.remove(Size_FilesToSend-3);
                }*/

                // operazioni da fare se caricamento avvenuto con successo
                isAllOK = true;
                upload_delegate.processFinish(FileName_local);
                //Upload_NotSentFiles();


            }
            if (isAllOK){
                notSentFileHandler.DeleteLine(File_FullPath);
                //UploadBlobs(BatLev);
                UploadBlobs();
            }
        }
    }



    //==========================================================================
    public void UploadSingleBlob(String folder, String name, String container, int battery){//String FileInternalPath, String FileName, String BlobContainer, int battery){
        //==========================================================================
        BatLev = battery;
        UploadSingleBlob_Async UploadOneBlob = new UploadSingleBlob_Async(folder, name, container);
        UploadOneBlob.execute();
    }

    //==========================================================================
    private class UploadSingleBlob_Async extends AsyncTask<Void, Boolean, String> {
        //==========================================================================
        String FileInternalPath_local = "";
        String FileName_local = "";
        String BlobContainer = "";
        String File_FullPath = "";
        //int Size_FilesToSend = 0;

        // Size_FilesToSend = 0 se stiamo caricando un nuovo file
        //                  = FilesToSend.size se stiamo caricando vecchi file dalla lista
        public UploadSingleBlob_Async(String FileInternalPath, String FileName, String in_BlobContainer) {
            FileInternalPath_local = FileInternalPath;
            FileName_local = FileName;
            BlobContainer = in_BlobContainer;
        }

        @Override
        protected String doInBackground(Void... params) {
            File_FullPath = FileInternalPath_local + FileName_local;

            if (!isBusy) {
                if (isNetworkOnline()) {
                    try {
                        isBusy = true;

                        // Retrieve storage account from connection-string.
                        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
                        // Create the blob client.
                        CloudBlobClient serviceClient = storageAccount.createCloudBlobClient();// get the reference to the container to be used

                        // Get a reference to the container where you want the blob to be saved
                        // Container name must be lower case. ( or it will turn BAD ERROR, error code 400)
                        CloudBlobContainer container = serviceClient.getContainerReference(BlobContainer);
                        container.createIfNotExists();

                        // Upload a file.
                        CloudBlockBlob blob = container.getBlockBlobReference(FileName_local);// i.e. "foto.png"
                        File sourceFile = new File(File_FullPath);// "C:\\Users\\Desktop\\foto.png");
                        blob.upload(new FileInputStream(sourceFile), sourceFile.length());
                        return null;

                    } catch (Exception e) {
                        //System.out.print("Exception encountered: ");
                        //System.out.println(e.getMessage());
                        FileLog.e("", "UploadSingleBlob_Async", e);
                        return File_FullPath;
                    }
                } else { // non online
                    return File_FullPath;
                }
            } else {
                return File_FullPath;
            }
        }
    }

    //==========================================================================
    protected void Upload_NotSentFiles(){
    //==========================================================================
        //if (FilesToSend.size() != 0) {


            UploadBlobs_Async new_upload = new UploadBlobs_Async(FilesToSend.get(FilesToSend.size() - 2), FilesToSend.get(FilesToSend.size() - 1), FilesToSend.get(FilesToSend.size()), FilesToSend.size());
            new_upload.execute();
        //}
    }

    // scarica il blob BlobName da un container (BlobContainer)
    //==========================================================================
    protected Boolean DownloadBlob(String WhereToSaveIt, String BlobContainer, String BlobName, int battery) {
    //==========================================================================
        // WhereToSaveIt = local destination path
        // FileContainer = azure folder containing file to be downloaded
        // BlobName      = name of the blob to be downloaded

        BatLev = battery;
        // Size_FilesToSend = 0 se stiamo caricando un nuovo file
        //                  = FilesToSend.size se stiamo caricando vecchi file dalla lista
        DownloadBlob_Async dwl = new DownloadBlob_Async(BlobContainer, WhereToSaveIt, BlobName, COMMAND_SIMPLE_TRANSFER);
        dwl.execute();
        return null;
    }

    //==========================================================================
    private class DownloadBlob_Async extends AsyncTask<Void, Boolean, Boolean> {
    //==========================================================================
        String BlobContainer_local = "";
        String WhereToSaveIt_local = "";
        String BlobName_local = "";
        int Size_FilesToDownload = 0;

        public DownloadBlob_Async(String BlobContainer, String WhereToSaveIt, String BlobName, int IN_Size_FilesToDownload )
        {
            Size_FilesToDownload = IN_Size_FilesToDownload;
            BlobContainer_local = BlobContainer;
            WhereToSaveIt_local = WhereToSaveIt;
            BlobName_local = BlobName;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if(!isBusy) {
                if (isNetworkOnline()) {
                    try {
                        isBusy = true;
                        // Retrieve storage account from connection-string.
                        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

                        // Create the blob client.
                        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

                        // Retrieve reference to a previously created container.
                        CloudBlobContainer container = blobClient.getContainerReference(BlobContainer_local);

                        // Loop through each blob item in the container.
                        for (ListBlobItem blobItem : container.listBlobs()) {
                            // If the item is a blob, not a virtual directory.
                            if (blobItem instanceof CloudBlob) {
                                // Download the item and save it to a file with the same name.
                                CloudBlob blob = (CloudBlob) blobItem;
                                if (blob.getName().equals(BlobName_local))
                                    blob.download(new FileOutputStream(WhereToSaveIt_local + blob.getName()) );
                            }
                        }
                        return true;
                    }
            catch (Exception e) {
                // Output the stack trace.
                FileLog.e("", "DownloadBlob_Async", e);
                e.printStackTrace();
                return false;
            }
                } else { // non online
                    return false;
                }
            }
            else{
                return false;
            }
        }

        @Override
        //==========================================================================
        protected void onPostExecute(Boolean res){ //on post execute del DOWNLOAD BLOB
        //==========================================================================
            isBusy = false;
            if (!res){
                // qualcosa è andato storto
                FilesToDownload.add(FilesToDownload.size(), BlobContainer_local);
                FilesToDownload.add(FilesToDownload.size(), WhereToSaveIt_local);       //this will add string at the next index
                FilesToDownload.add(FilesToDownload.size(), BlobName_local);
            } else{
                if (Size_FilesToDownload > 0) {
                    FilesToDownload.remove(Size_FilesToDownload-1);
                    FilesToDownload.remove(Size_FilesToDownload-2);
                    FilesToDownload.remove(Size_FilesToDownload-3);

                } else if (Size_FilesToDownload == COMMAND_UPDATE_APK) {
                    UpdateApp new_update = new UpdateApp(myConfig.get_WhereToSaveAPK_LocalPath() + BlobName_local);
                    new_update.setContext(mcontext);
                    new_update.execute();
                }
                Download_NotDownloadedFiles();
            }
        }
    }

    //==========================================================================
    protected void Download_NotDownloadedFiles(){
    //==========================================================================
        if (FilesToDownload.size() != 0) {
            DownloadBlob_Async new_downl= new DownloadBlob_Async(FilesToDownload.get(FilesToDownload.size() - 3), FilesToDownload.get(FilesToDownload.size() - 2), FilesToDownload.get(FilesToDownload.size() - 1), FilesToDownload.size());
            new_downl.execute();
        }
    }

    //==========================================================================
    public boolean isNetworkOnline() {
        //==========================================================================
        boolean online_status = false;

            try {
                ConnectivityManager cm = (ConnectivityManager) mcontext.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = cm.getNetworkInfo(0); // mobile
                if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED) {
                    online_status = true;
                } else {
                    netInfo = cm.getNetworkInfo(1); // wi-fi
                    if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED)
                        online_status = true;
                }
            } catch (Exception e) {
                FileLog.e("", "isNetworkOnline", e);
                e.printStackTrace();
                return false;
            }
            return online_status;
    }

    //==========================================================================
    //==========================================================================
    public void CheckAndUpdateAPK()
    //==========================================================================
    //==========================================================================
    {
        VersionCheckerAndUpdater new_version_checker_and_updater = new VersionCheckerAndUpdater();
        new_version_checker_and_updater.execute();

    }

    //==========================================================================
    //==========================================================================
    public void CheckNewUpdates(int battery){
    //==========================================================================
    //==========================================================================
        //BatLev = battery;
        // set local folder to save xml file
        //String storageConnectionString = myConfig.get_storageConnectionString();
        File WhereToSaveXml = new File(myConfig.get_WhereToSaveXML_LocalPath());
        if (!WhereToSaveXml.exists()) {
            WhereToSaveXml.mkdir();
        }

        // download xml file from cloud
        VersionChecker new_version_checker = new VersionChecker();
        new_version_checker.execute();
   }


    //==========================================================================
    private class VersionCheckerAndUpdater extends AsyncTask<Void, Boolean, Boolean> {
        //==========================================================================
        //String BlobContainer_local = "";
        String WhereToSaveIt_local = "";
        //String BlobName_local = "";

        int NewRelease;

        CloudBlob blobToDownload;

        String ApkFileToUpdate = "";

        //TODO: verificare la procedura per l'aggiornamento
        public VersionCheckerAndUpdater(){
            //myConfig = new Configuration();
            //BlobContainer_local = myConfig.get_APK_Container();
            WhereToSaveIt_local = myConfig.get_WhereToSaveXML_LocalPath();
            //BlobName_local = myConfig.get_XML_FileName();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if(!isBusy) {
                if (isNetworkOnline()) {
                    try {
                        isBusy = true;
                        // Retrieve storage account from connection-string.
                        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

                        // Create the blob client.
                        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

                        // Retrieve reference to a previously created container.
                        CloudBlobContainer container = blobClient.getContainerReference( myConfig.get_APK_Container());

                        // Loop through each blob item in the container.
                        for (ListBlobItem blobItem : container.listBlobs()) {
                            // If the item is a blob, not a virtual directory.
                            if (blobItem instanceof CloudBlob) {
                                // Download the item and save it to a file with the same name.
                                CloudBlob blob = (CloudBlob) blobItem;

                                String BlobName = blob.getName();//.replaceFirst("[.][^.]+$", "");

                                if( BlobName.contains("mariner"))
                                {
                                    String[] parts = BlobName.split("[-\\.]");
                                    int tempNewRelease = Integer.parseInt(parts[1]);

                                    if( tempNewRelease > myConfig.currentBuild && tempNewRelease > NewRelease )
                                    {
                                        NewRelease = tempNewRelease;
                                        ApkFileToUpdate = blob.getName();
                                        blobToDownload = blob;
                                    }
                                }

                                //if (blob.getName().equals(BlobName_local))
                                //  blob.download(new FileOutputStream(WhereToSaveIt_local + blob.getName()) );
                            }
                        }

                        return true;
                    }
                    catch (Exception e) {
                        FileLog.e("", "VersionCheckerAndUpdater", e);
                        // Output the stack trace.
                        e.printStackTrace();
                        return false;
                    }
                } else { // non online
                    return false;
                }
            }
            else{
                return false;
            }
        }
        @Override
        //==========================================================================
        protected void onPostExecute(Boolean res){ // post execute of CHECK VERSION
            //==========================================================================
            isBusy = false;
            if (!res){
                // qualcosa è andato storto
            } else{
                if (ApkFileToUpdate != "")
                {
                    // aggiorna apk
                    DownloadBlob_Async apk_dwl= new DownloadBlob_Async(myConfig.get_APK_Container(), myConfig.get_WhereToSaveAPK_LocalPath(),ApkFileToUpdate, COMMAND_UPDATE_APK);
                    apk_dwl.execute();
                }
            }
        }
    }




















    //==========================================================================
    private class VersionChecker extends AsyncTask<Void, Boolean, Boolean> {
    //==========================================================================
        //String BlobContainer_local = "";
        String WhereToSaveIt_local = "";
        //String BlobName_local = "";

        String ApkFileToUpdate = "";
        int NewRelease;

        CloudBlob blobToDownload;

        //TODO: verificare la procedura per l'aggiornamento
        public VersionChecker(){
            //myConfig = new Configuration();
            //BlobContainer_local = myConfig.get_APK_Container();
            WhereToSaveIt_local = myConfig.get_WhereToSaveXML_LocalPath();
            //BlobName_local = myConfig.get_XML_FileName();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if(!isBusy) {
                if (isNetworkOnline()) {
                    try {
                        isBusy = true;
                        // Retrieve storage account from connection-string.
                        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

                        // Create the blob client.
                        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

                        // Retrieve reference to a previously created container.
                        CloudBlobContainer container = blobClient.getContainerReference( myConfig.get_APK_Container());

                        // Loop through each blob item in the container.
                        for (ListBlobItem blobItem : container.listBlobs()) {
                            // If the item is a blob, not a virtual directory.
                            if (blobItem instanceof CloudBlob) {
                                // Download the item and save it to a file with the same name.
                                CloudBlob blob = (CloudBlob) blobItem;

                                String BlobName = blob.getName();//.replaceFirst("[.][^.]+$", "");

                                if( BlobName.contains("mariner"))
                                {
                                    String[] parts = BlobName.split("[-\\.]");
                                    int tempNewRelease = Integer.parseInt(parts[1]);

                                    if( tempNewRelease > myConfig.currentBuild && tempNewRelease > NewRelease )
                                    {
                                        NewRelease = tempNewRelease;
                                        ApkFileToUpdate = blob.getName();
                                        blobToDownload = blob;
                                    }

                                }

                                //if (blob.getName().equals(BlobName_local))
                                //  blob.download(new FileOutputStream(WhereToSaveIt_local + blob.getName()) );
                            }
                        }

                        if (ApkFileToUpdate != "")
                            blobToDownload.download(new FileOutputStream(WhereToSaveIt_local + ApkFileToUpdate) );


                        return true;
                    }
                    catch (Exception e) {
                        // Output the stack trace.
                        FileLog.e("", "VersionChecker", e);
                        e.printStackTrace();
                        return false;
                    }
                } else { // non online
                    return false;
                }
            }
            else{
                return false;
            }
        }
        @Override
        //==========================================================================
        protected void onPostExecute(Boolean res){ // post execute of CHECK VERSION
        //==========================================================================
            isBusy = false;
            if (!res){
                // qualcosa è andato storto
            } else{
                // check version
                //TODO: da verificare se e cosa mettere qui dentro

                /*
                String NewRelease = get_ApkRelease(true);
                String OldRelease = get_ApkRelease(false);
                int NewRelease_int =  Integer.parseInt(NewRelease);
                int OldRelease_int =  Integer.parseInt(OldRelease);
                if (NewRelease_int > OldRelease_int){
                    // aggiorna apk
                    DownloadBlob_Async apk_dwl= new DownloadBlob_Async(myConfig.get_APK_Container(), myConfig.get_WhereToSaveAPK_LocalPath(), myConfig.get_APK_FileName(), COMMAND_UPDATE_APK);
                    apk_dwl.execute();
                    set_new_ApkRelease(NewRelease);
                }
                */
            }
        }
    }


    private static final short MAX_NUM_OF_FILES_TO_BE_SENT = 5; // FILES: ACC, GYRO, MOTOR, BATTERY, WHEELCH
    String XmlString = "";
    boolean Xml_Uploaded_StartOfDoc = true;
    boolean Xml_NotUploaded_StartOfDoc = true;
    XmlSerializer serializer;
    StringWriter writer;
    private short NumberOfFiles = 0;
    private short NumberOfCorrectlyUploadedFiles = 0;

    //==========================================================================
    public void AppendNew_UploadedFileName(String text){
    //==========================================================================
        if ( text != null ) { // upload successful
            NumberOfFiles++;
            NumberOfCorrectlyUploadedFiles++;
            String TagName = text.substring(0,3); // these are the first 3 letters of filename, they will be used for tags
            try {
                // if it is the start of the document, init strings and put the start of the document
                if (Xml_Uploaded_StartOfDoc) {
                    serializer = Xml.newSerializer();
                    writer = new StringWriter();
                    String today = myConfig.tell_today_date();

                    serializer.setOutput(writer);
                    serializer.startDocument("UTF-8", true);

                    serializer.startTag("", "Azure_uploaded_files");
                    serializer.startTag("", "date");
                    serializer.text(today);
                    serializer.endTag("", "date");

                    Xml_Uploaded_StartOfDoc = false;
                }
                // then add uploaded filename to string
                serializer.startTag("", TagName);
                serializer.text(text);
                serializer.endTag("", TagName);

            } catch (Exception e) {
                FileLog.e("", "AppendNew_UploadedFileName", e);
                throw new RuntimeException(e);
            }
        }else { // upload failed
            NumberOfFiles++;
        }

        if (NumberOfFiles == MAX_NUM_OF_FILES_TO_BE_SENT){
            // we have tried to upload all the files, so close xml file,  save it and send it to azure
            NumberOfFiles = 0;
            Xml_Uploaded_StartOfDoc = true; // ready for next day acquisition file
            if(NumberOfCorrectlyUploadedFiles!=0) { // at least one tag is written on string
                NumberOfCorrectlyUploadedFiles = 0;
                try {
                    serializer.endTag("", "Azure_uploaded_files");
                    serializer.endDocument();
                    XmlString = writer.toString();

                    FileOutputStream outputStream;
                    String pathToUserMetadataXML = myConfig.get_WhereToSaveXML_LocalPath() + myConfig.get_UploadedFiles_XmlName();
                    File MyXml = new File(pathToUserMetadataXML);

                    outputStream = new FileOutputStream(MyXml);
                    outputStream.write(XmlString.getBytes());
                    outputStream.close();
                } catch (Exception e) {
                    FileLog.e("", "AppendNew_UploadedFileName", e);
                    e.printStackTrace();
                }
                UploadBlobs_Async UploadNewXml = new UploadBlobs_Async(myConfig.get_WhereToSaveXML_LocalPath(), myConfig.get_UploadedFiles_XmlName(), myConfig.get_APK_Container(), 0);
                UploadNewXml.execute();
            }
        }
    } // end of AppendNew_UploadedFileName

    //==========================================================================
    public void AddFileToSaveList(String WhereIsIt, String ItsName, String WhereDoesItGo){
    //==========================================================================
        FilesToSend.add(FilesToSend.size(), WhereIsIt);
        FilesToSend.add(FilesToSend.size(), ItsName);
        FilesToSend.add(FilesToSend.size(), WhereDoesItGo);
    }

    //==========================================================================
    public void AddFileToSaveList(String _FileName){
        //==========================================================================
        FilesToSend.add(FilesToSend.size(), _FileName);
    }

    //==========================================================================
    public void AppendNew_NotUploadedFileName(String text){
    //==========================================================================
        // da finire
        if ( text != null ) { // string has some kinf of sense
            String TagName = text.substring(0,3); // these are the first 3 letters of filename, they will be used for tags
            try {
                // if it is the start of the document, init strings and put the start of the document
                if (Xml_NotUploaded_StartOfDoc) {
                    serializer = Xml.newSerializer();
                    writer = new StringWriter();
                    String today = myConfig.tell_today_date();

                    serializer.setOutput(writer);
                    serializer.startDocument("UTF-8", true);

                    serializer.startTag("", "Azure_NOT_uploaded_files");
                    serializer.startTag("", "date");
                    serializer.text(today);
                    serializer.endTag("", "date");

                    Xml_NotUploaded_StartOfDoc = false;
                }
                // then add uploaded filename to string
                serializer.startTag("", TagName);
                serializer.text(text);
                serializer.endTag("", TagName);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        try {
            serializer.endTag("", "Azure_NOT_uploaded_files");
            serializer.endDocument();
            XmlString = writer.toString();

            FileOutputStream outputStream;
            String pathToUserMetadataXML = myConfig.get_WhereToSaveXML_LocalPath() + myConfig.get_UploadedFiles_XmlName();
            File MyXml = new File(pathToUserMetadataXML);

            outputStream = new FileOutputStream(MyXml);
            outputStream.write(XmlString.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // end of AppendNew_UploadedFileName






} // end class myAzureManager
