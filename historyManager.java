package WebCrawler;


import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import Common.Console;
import Common.Convert;
import Common.XmlTextReader;
import Common.XmlTextWriter;
import Common.FileF;

public class historyManager
{
    //store the history in xml

    public String FilePrefix;

    public historyManager(String fileprefix)
    {
        FilePrefix = fileprefix;
        LoadHistory();

    }

    public Boolean isXmlFileValid(String filename)
    {
        Boolean valid = true;
        XmlTextReader xmlTester = new XmlTextReader(filename);

        try
        {
            while (xmlTester.Read())
            {


                if (xmlTester.IsStartElement())
                {
                }
            }
        }
        catch (Exception ex)
        {
            valid = false;
        }
        
        xmlTester = null;

        return valid;
    }

    public void LoadHistory()
    {
        storedHistoryMap = null;

        filename = FilePrefix + "history.xml";

        filenameBackup = filename + ".backup";
        filenameBackupOld = filenameBackup + ".old";

        storedHistoryMap = new HashMap<String, storedHistory>();

        if (FileF.Exists(filename))
        {
            if (isXmlFileValid(filename))
            {
                // use latest history
                xmlReader = new XmlTextReader(filename);
            }
            else if (isXmlFileValid(filenameBackup))
            {
                // use history backup
                xmlReader = new XmlTextReader(filenameBackup);
            }
            else
            {
                // use really old history
                xmlReader = new XmlTextReader(filenameBackupOld);
            }

            

            // populate stored page map from file

            while (xmlReader.Read())
            {

                if (xmlReader.IsStartElement())
                {
                    if (xmlReader.Name.equals("history:item"))
                    {
                    	Date now = new Date();
                		Date createDate = new Date();
                        String title = "";

                        while (xmlReader.MoveToNextAttribute())
                        {
                            if (xmlReader.Name.equals("createdDate")||xmlReader.Name.equals("history:createdDate"))
                            {
                                try
                                {
                                	DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH);
                                    createDate = dateFormat.parse(xmlReader.Value);
                                }
                                catch (Exception exception)
                                {
                                    Console.WriteLine("Error parsing history record " + xmlReader.Value);
                                }
                            }
                            else if (xmlReader.Name.equals("title")||xmlReader.Name.equals("history:title"))
                            {
                                title = xmlReader.Value;
                            }

                        }

                        if (createDate != now
                            && title != ""
                            )
                        {

                            AddToHistory(title, createDate);
                        }


                    }
                }//END IF


            }// END WHILE

            xmlReader.Close();
        }
        else
        {
            xmlReader = null;
        }
        
        xmlReader = null;
    }

    public void RemoveOldHistory(Date cutOffDate)
    {
        if (storedHistoryMap.size() != 0)
        {
            HashMap<String, storedHistory> newstoredHistoryMap = new HashMap<String, storedHistory>();

            for (String key : storedHistoryMap.keySet())
            {
            	storedHistory element = storedHistoryMap.get(key);

            	 
                if (element.createdDate.after(cutOffDate))
                {
                    newstoredHistoryMap.put(key, element);

                }
                else
                {
                    Console.WriteLine("remove from history " + key);
                }
            }

            storedHistoryMap = newstoredHistoryMap;

            SaveHistory();

            LoadHistory();
        }
    }

    public void AddToHistory(String link, Date date)
    {


        storedHistory temp = new storedHistory(link, date);


        storedHistoryMap.put(link, temp);


    }

    public Boolean Contains(String link)
    {
        
        return storedHistoryMap.containsKey(link);
    }

    public void Remove(String link)
    {
        storedHistoryMap.remove(link);
    }


    public Boolean SaveHistory()
    {
        Boolean saved = false;
        try
        {
            if (xmlWriter != null)
            {

                xmlWriter.Close();
                xmlWriter = null;
            }

            if (FileF.Exists(filenameBackupOld))
            {
                // remove this file
            	FileF.Delete(filenameBackupOld);
            }

            if (FileF.Exists(filenameBackup))
            {
                //save backup of the backup
            	FileF.Copy(filenameBackup, filenameBackupOld);
            	FileF.Delete(filenameBackup);
            }

            if (FileF.Exists(filename))
            {
                //save backup of the file
            	FileF.Copy(filename, filenameBackup);
                FileF.Delete(filename);
            }

            //textWriter = new TextWriter
            xmlWriter = new XmlTextWriter(filename, null);

            // write the pagemap to xml file
            //xmlWriter.Formatting = Formatting.Indented;

            // create root node
            xmlWriter.WriteStartElement("x", "root", "urn:1");


            for (String key : storedHistoryMap.keySet())
            {
            	storedHistory history = storedHistoryMap.get(key);
                
                xmlWriter.WriteStartElement("history", "item", "urn:1");

                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH);
                String datestring = dateFormat.format(history.createdDate); 
                xmlWriter.WriteAttributeString("createdDate", "urn:1", datestring);
                xmlWriter.WriteAttributeString("title", "urn:1", history.Title);
                xmlWriter.WriteEndElement();
            }

            //complete root node
            xmlWriter.WriteEndElement();

            xmlWriter.Flush();

            xmlWriter.Close();

            saved = true;
        }
        catch (Exception ex)
        {
            Console.WriteLine("Error saving history");

        }

        xmlWriter = null;
        
        return saved;

    }


    String filename;

    String filenameBackup;
    String filenameBackupOld;

    HashMap<String, storedHistory> storedHistoryMap;

    XmlTextReader xmlReader;
    XmlTextWriter xmlWriter;

}
