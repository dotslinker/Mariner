package it.dongnocchi.mariner;

import android.util.Xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static android.os.Environment.getExternalStorageDirectory;

/**
 * Created by DianaM on 02/09/2015.
 */
public class Configuration {

    private String storageConnectionString;
    private String SAS;
    private String EventHub_url;

    public String Events_SAS;
    public String Events_EventHub_url;

    public String DailyUpdate_SAS;
    public String DailyUpdate_EventHub_url;

    public String HourlyUpdate_SAS;
    public String HourlyUpdate_EventHub_url;

    public String Events_EventHub_connstring;
    public String DailyUpdate_EventHub_connstring;
    public String HourlyUpdate_EventHub_connstring;

    private String APK_FileName;
    private String XML_FileName;

    private String Azure_Acquisition_Container;
    private String APK_Container;

    private String AcquisitionFolderPath;
    private String WhereToSaveAPK_LocalPath;
    private String WhereToSaveXML_LocalPath;
    private String Wheelchair_path;

    private String UploadedFiles_XmlName;
    private String NotUploadedFiles_XmlName;

    private String Today;

    private boolean UseGPSLocalization = false;

    private int DailyUpdateHour;

    //private XML_handler Xml_handle;
    //private String ChildName;
    //private String ChildSurname;

    public String WheelchairID;
    //private String DeviceType;

    public String LocalAcquisitionFolder;

    public String serviceNamespace;// = "mariner";
    public int NumOfmsMotorOn2SaveFiles = 60000; // Soglia di 60 sec per salvare i file tra accensione e spegnimento della carrozzina


    //==========================================================================
    public Configuration(){
    //==========================================================================
        //Xml_handle = new XML_handler();
        //User ThisChild = Xml_handle.read();
        //ChildName = ThisChild.getName().toLowerCase();
        //ChildSurname = ThisChild.getSurname().toLowerCase();

        //LoadConfig();
        //2016-0219 - proviamo il caricamento ibrido
        if (true)
        {
            LoadConfig();

            storageConnectionString = "DefaultEndpointsProtocol=http;"
                    + "AccountName=marinerblobs;"
                    + "AccountKey=2goGmxgHQEk/6iWtf2xyNBW2I+uNwqmE83VANtnnWsPmiZTJ2Q1m2g7aR474wFGdxc7YVa3FvHCHflLSXG3aLg==";

            Events_SAS = "SharedAccessSignature sr=https%3a%2f%2fmariner.servicebus.windows.net%2fasyncevents%2fpublishers%2fwheelchair%2fmessages&sig=%2frZeAPKOwUSQd5ZpXt%2fFuRNTYNmXO6Jt7onVmYbH7g4%3d&se=1455886181&skn=sender";
            HourlyUpdate_SAS = "SharedAccessSignature sr=https%3a%2f%2fmariner.servicebus.windows.net%2fhourlyupdate%2fpublishers%2fwheelchair%2fmessages&sig=RRd0hF5DBwZ0m1Uvol9y2GrzMQxC00D1HoKctJVVoRE%3d&se=1455886289&skn=sender";
            DailyUpdate_SAS = "SharedAccessSignature sr=https%3a%2f%2fmariner.servicebus.windows.net%2fdailyupdate%2fpublishers%2fwheelchair%2fmessages&sig=ydmfVTGmz5w0lYYL00Q2du6tJZLhcEPFJeRIFXfST7U%3d&se=1455886378&skn=sender";

        }
        else
        {

            if (true) {
                //WheelchairID = "TESTS4";
                //DeviceType = "wheelchair";
                // FOR BLOBS
                serviceNamespace = "mariner";
                WheelchairID = "TESTS4-0219";

                storageConnectionString = "DefaultEndpointsProtocol=http;"
                        + "AccountName=marinerblobs;"
                        + "AccountKey=2goGmxgHQEk/6iWtf2xyNBW2I+uNwqmE83VANtnnWsPmiZTJ2Q1m2g7aR474wFGdxc7YVa3FvHCHflLSXG3aLg==";

                // FOR EVENTS
                // SAS: to be generated with the script
                // this SAS end on 14/09/2016
                //SAS =           "SharedAccessSignature sr=https%3a%2f%2fmariner.servicebus.windows.net%2fmariner.eventhub%2fpublishers%2fabcd%2fmessages&sig=k7ph62dZJ5HTv19OGqnOkGF4iaeKHlsL5d0OtUxRdmI%3d&se=1473329371&skn=write_policy";
                //EventHub_url =  "https://mariner.servicebus.windows.net/mariner.eventhub/publishers/abcd/messages";

                //Questi sono stati inseriti da pm - 2016-0129
                //SAS = "Endpoint=sb://mariner.servicebus.windows.net/;SharedAccessKeyName=write_policy;SharedAccessKey=pKSZO9PP6jFtVd72h5jvxksiy+ldZetJNFD4C9qtKRQ=";
                //EventHub_url = "https://mariner.servicebus.windows.net/mariner.eventhub";

                //Events_SAS =    "SharedAccessSignature sr=https%3a%2f%2fmariner.servicebus.windows.net%2fmariner.events_test.eventhub%2fpublishers%2fwheelchair%2fmessages&sig=s%2fyy2rJn8J0N89M6%2bZGqhh44D1gx%2fz9F%2bata%2bfwkYGs%3d&se=1454431282&skn=send_policy";
                //sostituito il 2016-0219 //Events_SAS = "SharedAccessSignature sr=https%3a%2f%2fmariner.servicebus.windows.net%2fasyncevents%2fpublishers%2fwheelchair%2fmessages&sig=Ry%2fHjPj8JLBjESPKSiRXdl0aZUOIJyfRCasWY8W99MI%3d&se=1455799184&skn=sender";

                Events_SAS = "SharedAccessSignature sr=https%3a%2f%2fmariner.servicebus.windows.net%2fasyncevents%2fpublishers%2fwheelchair%2fmessages&sig=%2frZeAPKOwUSQd5ZpXt%2fFuRNTYNmXO6Jt7onVmYbH7g4%3d&se=1455886181&skn=sender";
                Events_EventHub_url = GetEventHubUrl("asyncevents", "wheelchair");

                //sostituito il 2016-0219 HourlyUpdate_SAS = "SharedAccessSignature sr=https%3a%2f%2fmariner.servicebus.windows.net%2fhourlyupdate%2fpublishers%2fwheelchair%2fmessages&sig=tCiO%2bArPI6uTiI7Xv6yXKKv%2bbAtD6Z0o2OXbtOHXzps%3d&se=1455799247&skn=sender";
                HourlyUpdate_SAS = "SharedAccessSignature sr=https%3a%2f%2fmariner.servicebus.windows.net%2fhourlyupdate%2fpublishers%2fwheelchair%2fmessages&sig=RRd0hF5DBwZ0m1Uvol9y2GrzMQxC00D1HoKctJVVoRE%3d&se=1455886289&skn=sender";
                HourlyUpdate_EventHub_url = GetEventHubUrl("hourlyupdate", "wheelchair");

                //sostituito il 2016-0219  DailyUpdate_SAS = "SharedAccessSignature sr=https%3a%2f%2fmariner.servicebus.windows.net%2fdailyupdate%2fpublishers%2fwheelchair%2fmessages&sig=jlULxQjm0vPLwFpSHYzlZ8MR4wxV1neGp3LESUQhcNA%3d&se=1455187127&skn=sender";
                DailyUpdate_SAS = "SharedAccessSignature sr=https%3a%2f%2fmariner.servicebus.windows.net%2fdailyupdate%2fpublishers%2fwheelchair%2fmessages&sig=ydmfVTGmz5w0lYYL00Q2du6tJZLhcEPFJeRIFXfST7U%3d&se=1455886378&skn=sender";
                DailyUpdate_EventHub_url = GetEventHubUrl("dailyupdate", "wheelchair");

                //carico questa parte solo se non sono convinto di quello che è stato caricato da
                //file XML
                if (true) {
                    //serviceNamespace = "mariner";

                    APK_Container = "apk-container";
                    APK_FileName = "WheelchairUpdate.apk";
                    XML_FileName = "LastRelease.xml";
                    //Azure_Acquisition_Container =     ChildName + ChildSurname + "-acquisitions";

                    //TODO: Perchè il file XML è sempre lo stesso e non cambia ?
                    UploadedFiles_XmlName = "Azure_UploadedFiles.xml";
                    NotUploadedFiles_XmlName = "Azure_NotUploadedFiles.xml";
                }
            } else {
                LoadConfig();
            }
        }

        //TODO: Definire un meccanismo per aggiornare i riferimenti in Azure per le autorizzazioni e su Blob e gli Event HUB

        //Impostiamo le ultime variabili

        Azure_Acquisition_Container =     WheelchairID+ "-acquisitions";// nei nomi dei containers: NO maiuscole, NO _

        WhereToSaveXML_LocalPath =  getExternalStorageDirectory().getAbsolutePath() + "/Wheelchair/" + "XMLFiles/";
        WhereToSaveAPK_LocalPath =  getExternalStorageDirectory().getAbsolutePath() + "/Wheelchair/" + "APK/";
        Wheelchair_path =           getExternalStorageDirectory().getAbsolutePath() + "/Wheelchair/";
        AcquisitionFolderPath =     getExternalStorageDirectory().getAbsolutePath() + "/Wheelchair/" + "Data/";



    }

    //==========================================================================
    public String get_storageConnectionString()    {return storageConnectionString;}
    public String get_SAS()                        {return Events_SAS;}
    public String get_EventHub_url()               {return EventHub_url;}

    public String get_Wheelchair_path()            {return Wheelchair_path;}
    public String get_WhereToSaveAPK_LocalPath()   {return WhereToSaveAPK_LocalPath;}
    public String get_WhereToSaveXML_LocalPath()   {return WhereToSaveXML_LocalPath;}
    public String get_Acquisition_Folder()         {return AcquisitionFolderPath;}

    public String get_APK_Container()              {return APK_Container;}
    public String get_Acquisition_Container()      {return Azure_Acquisition_Container;}

    public String get_APK_FileName()               {return APK_FileName;}
    //TODO: verificare a cosa serve questo file qui XML_FileName
    public String get_XML_FileName()               {return XML_FileName;}
    public String get_UploadedFiles_XmlName()      {return UploadedFiles_XmlName;}
    public String get_NotUploadedFiles_XmlName()   {return NotUploadedFiles_XmlName;}

    public String get_WheelchairID()                  {return WheelchairID;}

    public boolean get_UseGPSLocalization()         {return UseGPSLocalization;}
    public int get_DailyUpdateHour()                {return DailyUpdateHour;}

    //    public String tell_ChildName()                  {return ChildName;}
    //    public String tell_ChildSurame()                {return ChildSurname;}

    public String tell_today_date() {
        // set today date as string
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd");
        Date today_date = new Date();
        Today = formatter.format(today_date);
        return Today;
    }
    public int tell_today_date_int() {
        // set today date as string
        SimpleDateFormat formatter = new SimpleDateFormat("dd");
        Date today_date = new Date();
        Today = formatter.format(today_date);
        int Today_Int = Integer.parseInt(Today);
        return Today_Int;
    }

    private String GetEventHubUrl(String hubName, String device)
    {
        String s;
        s = "https://" + serviceNamespace +
            ".servicebus.windows.net/" + hubName +
                "/publishers/" + device + "/messages";
        return s;
    }

    public String getAcquisitionsFolder() {return LocalAcquisitionFolder;}


    public void LoadConfig()
    {
        String TagName;

        File folder = new File(getExternalStorageDirectory().getAbsolutePath() + "/Wheelchair");

        String pathToUserMetadataXML = folder.getAbsolutePath() + "/config.xml";
        File path = new File(pathToUserMetadataXML);

        //File XMLConfigFile = new File(getExternalStorageDirectory().getAbsolutePath() + "/Wheelchair/config.xml");
        //String Filename = getExternalStorageDirectory().getAbsolutePath() + "/Wheelchair/config.xml";

            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(false);
                factory.setValidating(false);
                DocumentBuilder builder = factory.newDocumentBuilder();

                Document doc = builder.parse(path);

                NodeList myNL = doc.getFirstChild().getChildNodes();

                for (int i = 0; i < myNL.getLength(); ++i) {
                    Node myNode = myNL.item(i);
                    if( myNode.getNodeType()== Node.ELEMENT_NODE) {
                        String NodeType = myNode.getNodeName();
                        String s = myNode.getTextContent();
                        //NodeList children = myNL.item(i).getChildNodes();
                        //String s = children.item(0).getNodeValue();

                        switch (NodeType) {
                            case "device_id":
                                WheelchairID = s;
                                break;
                            case "acquisition_folder":
                                LocalAcquisitionFolder = s;
                                break;
                            case "azure_service_namespace":
                                serviceNamespace = s;
                                break;
                            case "blob_connectionstring":
                                storageConnectionString = s;
                                break;
                            case "eventhub_events_url":
                                Events_EventHub_url = s;
                                break;
                            case "eventhub_events_connstring":
                                Events_EventHub_connstring = s;
                                break;
                            case "eventhub_hourlyupdate_url":
                                HourlyUpdate_EventHub_url = s;
                                break;
                            case "eventhub_hourlyupdate_connstring":
                                HourlyUpdate_EventHub_connstring = s;
                                break;
                            case "eventhub_dailyupdate_url":
                                DailyUpdate_EventHub_url = s;
                                break;
                            case "eventhub_dailyupdate_connstring":
                                DailyUpdate_EventHub_connstring = s;
                                break;
                            case "apk_container":
                                APK_Container = s;
                                break;
                            case "apk_filename":
                                APK_FileName = s;
                                break;
                            case "xml_filename":
                                XML_FileName = s;
                                break;
                            case "uploadedfiles_xml_filename":
                                UploadedFiles_XmlName = s;
                                break;
                            case "notuploadedfiles_xml_filename":
                                NotUploadedFiles_XmlName = s;
                                break;
                            case "daily_update_hour":
                                DailyUpdateHour = Integer.parseInt(s);
                                break;
                            case "use_gps_localization":
                                UseGPSLocalization = Boolean.parseBoolean(s);
                                break;
                        }
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

    }


    private String ReadNextText(XmlPullParser MyParser)
    {
        String ret_s = "";

        try {
            ret_s = MyParser.nextText();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            return ret_s;
        }
    }
}
