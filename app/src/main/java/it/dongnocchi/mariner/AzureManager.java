package it.dongnocchi.mariner;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import java.util.LinkedList;
import java.util.Queue;

import java.io.*;
import java.util.Scanner;

import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;

/**
 * Created by DianaM on 27/08/2015.
 */

public class AzureManager {

    public static String storageConnectionString;
    public Queue<String> FilesToSend = new LinkedList<>(); //List of files not yet sent to the repository
    //public List<String> FilesToDownload = new ArrayList<>();
    Context mcontext;
    private boolean isBusy = false;

    Configuration myConfig;

    public boolean ConfigDownloaded = false;

    String ApkFileToUpdate = "";

    protected final int COMMAND_UPDATE_APK = -1;
    protected final int COMMAND_UPDATE_CONFIG = -3;
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
        FileLog.d("AzureManager", "AzureManager created");
    }

    //==========================================================================
    //protected void UploadFilesToBlobs(int battery){//String FileInternalPath, String FileName, String BlobContainer, int battery){
    protected void UploadBlobs_old() {//String FileInternalPath, String FileName, String BlobContainer, int battery){
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
        String File_FullPath = "";
        int Size_FilesToSend = 0;

        // Size_FilesToSend = 0 se stiamo caricando un nuovo file
        //                  = FilesToSend.size se stiamo caricando vecchi file dalla lista
        public UploadBlobs_Async(String FileInternalPath, String FileName, String in_BlobContainer, int IN_Size_FilesToSend) {
            FileInternalPath_local = FileInternalPath;
            FileName_local = FileName;
            BlobContainer = in_BlobContainer;
            Size_FilesToSend = IN_Size_FilesToSend;
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

                        FileLog.e("", "UploadBlobs_Async", e);
                        return File_FullPath;
                    }
                } else { // non online
                    return File_FullPath;
                }
            } else {
                return File_FullPath;
            }
        }

        @Override
        protected void onPostExecute(String res) { // post execute del UPLOAD BLOB
            isBusy = false;
            boolean isAllOK = false;

            if (res != null) {
                isAllOK = false;
                upload_delegate.processFinish(null);
               /* // qualcosa è andato storto, accoda file non inviati nella lista

                FilesToSend.add(FilesToSend.size(), FileInternalPath_local);
                FilesToSend.add(FilesToSend.size(), FileName_local);               //this will add string at the next index
                FilesToSend.add(FilesToSend.size(), BlobContainer);*/
            } else {
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
            if (isAllOK) {
                notSentFileHandler.DeleteLine(File_FullPath);
                //UploadFilesToBlobs(BatLev);
                UploadFilesToBlobs();
            }
        }
    }

    //==========================================================================
    protected void AppendFilesToUploadList() {//String FileInternalPath, String FileName, String BlobContainer, int battery){
        // ==========================================================================

        try {
            for (int i = 0; i < FilesToSend.size(); i++) {
                AppendSavedFileToFileListToUpoload(FilesToSend.poll());
            }

        } catch (Exception ex) {
            FileLog.e("AzureEventManager", "AppendFilesToUploadList :" + ex.toString());
        }

        //FileAsyncUploader FileUploader = new FileAsyncUploader();
        //FileUploader.execute();
    }


    public void AppendSavedFileToFileListToUpoload(String filename) {
        FileWriter f;
        try {

            f = new FileWriter(myConfig.get_Acquisition_Folder() +
                    myConfig.OfflineFilesToUploadListFilename, true);
            f.write(filename + System.getProperty("line.separator"));
            f.flush();
            f.close();
        } catch (Exception ex) {
            FileLog.e("AzureEventManager", "AppendDailyJsonReportToFile :" + ex.toString());
        }
    }

    public void UpdateFilesToSendList()
    {
        try {

            File f = new File(myConfig.get_Acquisition_Folder() +
                    myConfig.OfflineFilesToUploadListFilename);
            if (f.exists() && !f.isDirectory()) {
                // do something
                Scanner s = new Scanner(f);

                while (s.hasNextLine()) {
                    String str = s.nextLine();

                    if(!FilesToSend.contains(str))
                    {
                        FilesToSend.add(str);
                    }
                }
                s.close();

                f.delete();
            }
        } catch (Exception ex) {
            FileLog.e("AzureEventManager", "SendJsonHourlyEventListFromFile :" + ex.toString());
        }
    }



    //==========================================================================
    protected void UploadFilesToBlobs() {//String FileInternalPath, String FileName, String BlobContainer, int battery){
        // ==========================================================================

        FileAsyncUploader FileUploader = new FileAsyncUploader();
        FileUploader.execute();
    }

    //==========================================================================
    private class FileAsyncUploader extends AsyncTask<Void, Boolean, String> {
        //==========================================================================
        String LocalAcquisitionFolder = "";
        String FileName_local = "";
        String BlobContainer = "";
        String File_FullPath = "";

        // Size_FilesToSend = 0 se stiamo caricando un nuovo file
        //                  = FilesToSend.size se stiamo caricando vecchi file dalla lista
        public FileAsyncUploader() {
            LocalAcquisitionFolder = myConfig.get_Acquisition_Folder();
            BlobContainer = myConfig.get_Acquisition_Container();
        }

        @Override
        protected String doInBackground(Void... params) {
            if (FilesToSend.size() > 0) {
                FileName_local = FilesToSend.peek();
                File_FullPath = LocalAcquisitionFolder + FileName_local;

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
                            //container.createIfNotExists(null, null);

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
                } else {
                    return File_FullPath;
                }
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String res) { // post execute del UPLOAD BLOB
            isBusy = false;
            boolean isAllOK = false;

            if (res != null) {
                isAllOK = false;
                upload_delegate.processFinish(null);
               /* // qualcosa è andato storto, accoda file non inviati nella lista

                FilesToSend.add(FilesToSend.size(), FileInternalPath_local);
                FilesToSend.add(FilesToSend.size(), FileName_local);               //this will add string at the next index
                FilesToSend.add(FilesToSend.size(), BlobContainer);*/
            } else {
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

            if (isAllOK) {
                FilesToSend.remove();
                //notSentFileHandler.DeleteLine(File_FullPath);
                //UploadFilesToBlobs(BatLev);

                if (FilesToSend.size() > 0)
                    UploadFilesToBlobs();
            }
        }
    }

/*
    //==========================================================================
    public void UploadSingleBlob(String folder, String name, String container, int battery){//String FileInternalPath, String FileName, String BlobContainer, int battery){
        //==========================================================================
        //BatLev = battery;
        UploadSingleBlob_Async UploadOneBlob = new UploadSingleBlob_Async(folder, name, container);
        UploadOneBlob.execute();
    }
*/

    //==========================================================================
    private class UploadSingleBlob_Async extends AsyncTask<Void, Boolean, String> {
        //==========================================================================
        String FileInternalPath_local = "";
        String FileName_local = "";
        String BlobContainerName = "";
        String File_FullPath = "";
        //int Size_FilesToSend = 0;

        // Size_FilesToSend = 0 se stiamo caricando un nuovo file
        //                  = FilesToSend.size se stiamo caricando vecchi file dalla lista
        public UploadSingleBlob_Async(String FileInternalPath, String FileName, String in_BlobContainer) {
            FileInternalPath_local = FileInternalPath;
            FileName_local = FileName;
            BlobContainerName = in_BlobContainer;
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
                        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();// get the reference to the container to be used

                        // Get a reference to the container where you want the blob to be saved
                        // Container name must be lower case. ( or it will turn BAD ERROR, error code 400)
                        CloudBlobContainer myBlobContainer = blobClient.getContainerReference(BlobContainerName);
                        myBlobContainer.createIfNotExists();

                        // Upload a file.
                        CloudBlockBlob blob = myBlobContainer.getBlockBlobReference(FileName_local);// i.e. "foto.png"
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
    /*
        //==========================================================================
        protected void Upload_NotSentFiles(){
        //==========================================================================
            //if (FilesToSend.size() != 0) {

                UploadBlobs_Async new_upload = new UploadBlobs_Async(FilesToSend.get(FilesToSend.size() - 2), FilesToSend.get(FilesToSend.size() - 1), FilesToSend.get(FilesToSend.size()), FilesToSend.size());
                new_upload.execute();
            //}
        }
    */
    // scarica il blob BlobName da un container (BlobContainer)
/*
    //==========================================================================
    protected Boolean DownloadBlob(String WhereToSaveIt, String BlobContainer, String BlobName, int battery) {
        //==========================================================================
        // WhereToSaveIt = local destination path
        // FileContainer = azure folder containing file to be downloaded
        // BlobName      = name of the blob to be downloaded

        BatLev = battery;
        // Size_FilesToSend = 0 se stiamo caricando un nuovo file
        //                  = FilesToSend.size se stiamo caricando vecchi file dalla lista
        BlobAsyncDownloader dwl = new BlobAsyncDownloader(BlobContainer, WhereToSaveIt, BlobName, COMMAND_SIMPLE_TRANSFER);
        dwl.execute();
        return null;
    }
*/

    //==========================================================================
    private class BlobAsyncDownloader extends AsyncTask<Void, Boolean, Boolean> {
        //==========================================================================
        String BlobContainerName = "";
        String WhereToSaveIt_local = "";
        String BlobName_local = "";
        String BlobName_remote = "";
        int Size_FilesToDownload = 0;

        public BlobAsyncDownloader(String _BlobContainerName, String WhereToSaveIt, String BlobName, int IN_Size_FilesToDownload) {
            Size_FilesToDownload = IN_Size_FilesToDownload;
            BlobContainerName = _BlobContainerName;
            WhereToSaveIt_local = WhereToSaveIt;
            BlobName_local = BlobName;
            BlobName_remote = BlobName;
        }

        public BlobAsyncDownloader(String _BlobContainerName, String WhereToSaveIt, String BlobNameRemote, String BlobNameLocal, int IN_Size_FilesToDownload) {
            Size_FilesToDownload = IN_Size_FilesToDownload;
            BlobContainerName = _BlobContainerName;
            WhereToSaveIt_local = WhereToSaveIt;
            BlobName_local = BlobNameLocal;
            BlobName_remote = BlobNameRemote;
        }


        @Override
        protected Boolean doInBackground(Void... params) {
            if (!isBusy) {
                if (isNetworkOnline()) {
                    try {
                        isBusy = true;
                        // Retrieve storage account from connection-string.
                        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

                        // Create the blob client.
                        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

                        // Retrieve reference to a previously created container.
                        CloudBlobContainer container = blobClient.getContainerReference(BlobContainerName);

                        if (false) {
                            CloudBlob blob = container.getBlockBlobReference(BlobName_remote);
                            if (blob.exists()) {
                                blob.download(new FileOutputStream(WhereToSaveIt_local + BlobName_local));

                                //Erase the config blob to avoid continuous download
                                if (Size_FilesToDownload == COMMAND_UPDATE_CONFIG)
                                    blob.deleteIfExists();
                            }
                        } else {
                            // Loop through each blob item in the container.
                            for (ListBlobItem blobItem : container.listBlobs()) {
                                // If the item is a blob, not a virtual directory.
                                if (blobItem instanceof CloudBlob) {
                                    // Download the item and save it to a file with the same name.
                                    CloudBlob blob = (CloudBlob) blobItem;
                                    if (blob.getName().equals(BlobName_remote)) {
                                        blob.download(new FileOutputStream(WhereToSaveIt_local + BlobName_local));
                                        //blob.download(new FileOutputStream(WhereToSaveIt_local + blob.getName()));

                                        if (Size_FilesToDownload == COMMAND_UPDATE_CONFIG)
                                            blob.deleteIfExists();
                                    }
                                }
                            }
                        }
                        return true;
                    } catch (Exception e) {
                        // Output the stack trace.
                        FileLog.e("", "BlobAsyncDownloader", e);
                        e.printStackTrace();
                        return false;
                    }
                } else { // non online
                    return false;
                }
            } else {
                return false;
            }
        }

        @Override
        //==========================================================================
        protected void onPostExecute(Boolean res) { //on post execute del DOWNLOAD BLOB
            //==========================================================================
            isBusy = false;
            if (!res) {
                // qualcosa è andato storto
                //FilesToDownload.add(FilesToDownload.size(), BlobContainer_local);
                //FilesToDownload.add(FilesToDownload.size(), WhereToSaveIt_local);       //this will add string at the next index
                //FilesToDownload.add(FilesToDownload.size(), BlobName_local);
            } else {
                if (Size_FilesToDownload > 0) {
                    //FilesToDownload.remove(Size_FilesToDownload-1);
                    //FilesToDownload.remove(Size_FilesToDownload-2);
                    //FilesToDownload.remove(Size_FilesToDownload-3);

                } else if (Size_FilesToDownload == COMMAND_UPDATE_APK) {
                    AppUpdater new_updater = new AppUpdater(myConfig.get_WhereToSaveAPK_LocalPath() + BlobName_local);
                    new_updater.setContext(mcontext);
                    new_updater.execute();
                } else if (Size_FilesToDownload == COMMAND_UPDATE_CONFIG) {
                    ConfigDownloaded = true;
                }

                //Download_NotDownloadedFiles();
            }
        }
    }

    //TODO: verificare che si possa togliere del tutto
/*
    //==========================================================================
    protected void Download_NotDownloadedFiles(){
        //==========================================================================
        if (FilesToDownload.size() != 0) {
            BlobAsyncDownloader new_downl= new BlobAsyncDownloader(FilesToDownload.get(FilesToDownload.size() - 3), FilesToDownload.get(FilesToDownload.size() - 2), FilesToDownload.get(FilesToDownload.size() - 1), FilesToDownload.size());
            new_downl.execute();
        }
    }
*/
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
        AppCheckerAndUpdater new_version_checker_and_updater = new AppCheckerAndUpdater();
        new_version_checker_and_updater.execute();
    }

    //==========================================================================
    //==========================================================================
    public void CheckAndUpdateConfig()
    //==========================================================================
    //==========================================================================
    {
        String RemoteBlobName = myConfig.WheelchairID + "-config.xml";
        String LocalBlobName = "config.xml";
        BlobAsyncDownloader config_dwl = new BlobAsyncDownloader(myConfig.get_Config_Container(),
                myConfig.get_WhereToSaveConfig_LocalPath(),
                RemoteBlobName, LocalBlobName, COMMAND_UPDATE_CONFIG);
        config_dwl.execute();
    }



    /*
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
*/

    //==========================================================================
    private class AppCheckerAndUpdater extends AsyncTask<Void, Boolean, Boolean> {
        //==========================================================================
        //String BlobContainer_local = "";
        //String WhereToSaveIt_local = "";
        //String BlobName_local = "";

        int NewRelease;

        CloudBlob blobToDownload;

        String ApkFileToUpdate = "";

        //TODO: verificare la procedura per l'aggiornamento
        public AppCheckerAndUpdater() {
            //myConfig = new Configuration();
            //BlobContainer_local = myConfig.get_APK_Container();
            //WhereToSaveIt_local = myConfig.get_WhereToSaveXML_LocalPath();
            //BlobName_local = myConfig.get_XML_FileName();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (!isBusy) {
                if (isNetworkOnline()) {
                    try {
                        isBusy = true;
                        // Retrieve storage account from connection-string.
                        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

                        // Create the blob client.
                        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

                        // Retrieve reference to a previously created container.
                        CloudBlobContainer container = blobClient.getContainerReference(myConfig.get_APK_Container());

                        // Loop through each blob item in the container.
                        for (ListBlobItem blobItem : container.listBlobs()) {
                            // If the item is a blob, not a virtual directory.
                            if (blobItem instanceof CloudBlob) {
                                // Download the item and save it to a file with the same name.
                                CloudBlob blob = (CloudBlob) blobItem;

                                String BlobName = blob.getName();//.replaceFirst("[.][^.]+$", "");

                                //Let's check if there is a specific version of the software for my ID
                                if (BlobName.contains(myConfig.WheelchairID))
                                    BlobName = BlobName.replace(myConfig.WheelchairID, "mariner");

                                if (BlobName.contains("mariner")) {
                                    String[] parts = BlobName.split("[-\\.]");
                                    int tempNewRelease = Integer.parseInt(parts[1]);

                                    if (tempNewRelease > myConfig.currentBuild && tempNewRelease > NewRelease) {
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
                    } catch (Exception e) {
                        FileLog.e("", "AppCheckerAndUpdater", e);
                        // Output the stack trace.
                        e.printStackTrace();
                        return false;
                    }
                } else { // non online
                    return false;
                }
            } else {
                return false;
            }
        }

        @Override
        //==========================================================================
        protected void onPostExecute(Boolean res) { // post execute of CHECK VERSION
            //==========================================================================
            isBusy = false;
            if (!res) {
                // qualcosa è andato storto
            } else {
                if (ApkFileToUpdate != "") {
                    // aggiorna apk
                    BlobAsyncDownloader apk_dwl = new BlobAsyncDownloader(myConfig.get_APK_Container(), myConfig.get_WhereToSaveAPK_LocalPath(), ApkFileToUpdate, COMMAND_UPDATE_APK);
                    apk_dwl.execute();
                }
            }
        }
    }

    //==========================================================================
    public void AddFileToSavedFileList(String _FileName) {
        //==========================================================================
        //FilesToSend.add(FilesToSend.size(), _FileName);
        FilesToSend.add(_FileName);
    }


} // end class myAzureManager
