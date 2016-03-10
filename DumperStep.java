package WebCrawler;

import java.awt.List;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Pattern;


import Common.Console;
import Common.reporter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class DumperStep  implements Runnable
{

    private Object spellingMutex = new Object();

    public DumperStep()
    { 
    	
    }
    
    public void Init(
                  historyManager _history, 
                  reporter ReportFile,
                  CrawlInfo _CrawlInfo
                    )
    {
    	crawlInfo = _CrawlInfo;
        history = _history;
        reportFile = ReportFile;

    }
    
    public void run()
    {
    	try {
    		Process();
    	}
    	catch (Exception e)
    	{
    		reportFile.report("exception in dumper processing", true);
    	}
    	Shutdown();
    }
    
    public void Shutdown()
    {
    	
    }
    
    public void SetPage(Page _page)
    {
    	page = _page;
    }

    
    public void Process()
    {
    	if(page==null)
    	{
    		reportFile.report("DUMPER ERROR url not set ", true);
    		return;
    	}
    	
    	String url = page.Url;
        System.out.println("URL: " + url);
    	
    
        String text = "";
        try {
            text = page.Text;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        Elements links =   page.Links;
            
        System.out.println("Text length: " + text.length());

        System.out.println("Number of outgoing links: " + links.size());

        // Under certain conditions you cuold decide to signal to crawler that is should stop
        // from this thread
        /*if(true)
        {
            crawlInfo.crawlStop = true;
            return;
        }*/
 

        Date articleDate = DateForPage(rootURL);

        // have we crawled this url before ?
        if (history.Contains(originalUrl))
        {
            
        }
        else
        {
            history.AddToHistory(originalUrl, new Date());
        }

        //ignore pages that don't exist
        StringBuilder goodUrl = new StringBuilder();
        if (!RemoteFileExists(articleURL, goodUrl))
        {
            return;
        }

        
        //
        // the rest of code you can write !
        // maybe use nlp to analyse the text content 
        //
    }


    private Date DateForPage(String url)
    {
    	Date pageDate = new Date();
    	
    	try
        {
        	URL iurl = new URL(url);
    		HttpURLConnection con = (HttpURLConnection)iurl.openConnection();

    		
    		con.setRequestMethod("HEAD");
    		con.setConnectTimeout(60000); //set timeout to 60 seconds
    		con.setReadTimeout(60000);
    		con.connect();

    		long time = con.getDate();
    		pageDate = new Date(time);


            return (pageDate);
        }
        catch (Exception exception)
        {
            //Any exception will returns false.
            return pageDate;
        }
    
    }
    
    private Boolean RemoteFileExists(String url, StringBuilder goodUrl)
    {
        try
        {
        	URL iurl = new URL(url);
    		HttpURLConnection con = (HttpURLConnection)iurl.openConnection();

    		con.setRequestMethod("HEAD");
    		con.setConnectTimeout(60000); //set timeout to 60 seconds
    		con.setReadTimeout(60000);
    		con.connect();

    		int code = con.getResponseCode();

    		Boolean failed = false;

            Boolean returnval = (code == HttpURLConnection.HTTP_OK);
           

            if (returnval)
            {
            	goodUrl.append(url);
            }


            return (returnval);
        }
        catch (InterruptedIOException e)
        {
        	Console.WriteLine("thread interrupted");
        	return false;
        }
        catch (Exception exception)
        {
            //Any exception will returns false.
            return false;
        }
    }


    reporter reportFile;
    historyManager history;
    
    Page page;
    CrawlInfo crawlInfo;
   
}

    


