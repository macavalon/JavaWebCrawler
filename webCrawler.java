package WebCrawler;

import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import Common.reporter;

public class webCrawler {

	public webCrawler(historyManager _history, 
			            reporter _ReportFile,
			            String _rootUrl)
	{
		
		
		crawlInfo = new CrawlInfo();
		dumper = new DumperStep();
		dumper.Init(_history, _ReportFile, crawlInfo);
		runDumperThread = null;
		processedHistory = _history;
        
        ReportFile =_ReportFile;
		rootUrl = _rootUrl;
		domain = "";
		try {
			domain = getDomainName(rootUrl);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		stop = false;
		maxDepth = 2;           // SET DEPTH OF CRAWLING
		timeoutSeconds = 5;
		
		history = new TreeSet<String>();
        
        //init memory use variables

		maxMemUse = 0;
		currentMemUse = 0;
	}
	
	long currentMemUse;
	long maxMemUse;
	
	public void memoryUsed()
	{
		
		//check memory use
		Runtime rt = Runtime.getRuntime();
		long usedMB = (rt.totalMemory() - rt.freeMemory());
	    
		if(usedMB > maxMemUse)
		{
			maxMemUse = usedMB;
		}
		if(currentMemUse!=usedMB)
		{
			currentMemUse = usedMB;
			ReportFile.report("memory usage : " + usedMB + "(" +maxMemUse + ")  bytes",true);
		}
	
	}
	
	public void processPage(String URL, Integer currentDepth) {
		
		ReportFile.report("Seaching : Depth " + currentDepth, false);
		
		
		
		if(stop)
		{
			ReportFile.report(URL + " stopping", true);
			return;
			
		}

		Integer nextdepth = currentDepth + 1;
		
		if(!history.contains(URL))
		{
			history.add(URL);
		}
		else
		{
			ReportFile.report(URL + " already crawled", false);
			return;
		}
		
		//n.b. history manager is cleaned up every 7 days
		// but only stop leaf nodes
		if(!processedHistory.Contains(URL))
		{
			// not yet processed...
		}
		else if(currentDepth==maxDepth)
		{
			ReportFile.report(URL + " already processed", false);
			return;
		}
		
		
		if(!ShouldCrawl(URL))
		{
			ReportFile.report(URL + " ignoring", false);

			return;
		}
		
		//get useful information
		Document doc = null;
		
		Boolean gotDoc  = false;
		int tries = 0;
		
		while(!gotDoc && tries < 10)
		{
			try {
				
				// for politeness... only one every Xms
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					
					ReportFile.report(URL + " interrupted returning", false);
					return;
				}
				
				Response response = Jsoup.connect(URL).userAgent("Mozilla").timeout(timeoutSeconds*1000).execute();
				int statusCode  = response.statusCode();
				String statusMsg = response.statusMessage();
				if(statusCode==HttpURLConnection.HTTP_OK)
				{
					ReportFile.report(URL + " reading", false);
					doc = response.parse();
					doc.select("iframe").remove();
					gotDoc = true;
				}
				else
				{
					ReportFile.report(URL + " invalid", false);
					return;
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				ReportFile.report(URL + " broken link", true);
				
				if(currentDepth==0)
				{
					//root node must try several times to achieve
					tries++;
				}
				else
				{
					return;
				}
			}
		}
		
		if(!gotDoc)
		{
			return;
		}

		//get all links and recursively call the processPage method
		Elements links = doc.select("a[href],iframe[src]");
		
		Elements descriptions = doc.select("meta[name=description],meta[name=Description");
		String description = "";
		if(descriptions.size()!=0)
		{
			description = descriptions.first().attr("content");
		}
		
		//
		// fb meta tags
		//
		
		//fb title

		String title = doc.title();
		
		Elements fbtitles = doc.select("meta[property=og:title]");
		StringBuilder fbtitle = new StringBuilder();
		if(fbtitles.size()!=0)
		{
			fbtitle.append(fbtitles.first().attr("content"));
			removeExcessWhiteSpace(fbtitle);
			
			if(fbtitle.toString().equals(""))
			{
				ReportFile.report("ignore bad fb title ", false);
			}
			else
			{
				title = fbtitle.toString();
				ReportFile.report("found fb title " + title, false);
			}
		}
		
		
		//fb description
		Elements fbdescriptions = doc.select("meta[property=og:description]");
		StringBuilder fbdescription = new StringBuilder();
		if(fbdescriptions.size()!=0)
		{
			fbdescription.append(fbdescriptions.first().attr("content"));
			removeExcessWhiteSpace(fbdescription);
			
			if(fbdescription.toString().equals(""))
			{
				ReportFile.report("ignore bad fb desc ", false);
			}
			else
			{
				description = fbdescription.toString();
				ReportFile.report("found fb desc " + description, false);
			}
		}
		
		//fb image
		Elements fbimages = doc.select("meta[property=og:image]");
		StringBuilder fbimage = new StringBuilder();
		if(fbimages.size()!=0)
		{
			fbimage.append(fbimages.first().attr("content"));
			removeExcessWhiteSpace(fbimage);
			
			if(fbimage.toString().equals(""))
			{
				ReportFile.report("ignore bad fb image ", false);
			}
			else
			{
				ReportFile.report("found fb image " + fbimage.toString(), false);
			}
		}
		
		if(fbimage.length()==0)
		{
			fbimage.append("");
		}
		
		//pass this page content to callback
		Page page = new Page(URL,doc.text(),links, title,description,fbimage.toString());
		
		crawlInfo.crawlStop = false;
		dumper.SetPage(page);
		runDumperThread = new Thread(dumper);

		runDumperThread.start();
		
		try {
			runDumperThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        //measure and report current memory use
		memoryUsed();
		
		if(crawlInfo.crawlStop)
		{
			stop = true;
			return;
		}
		
		if(nextdepth > maxDepth)
		{
			return;	
		}

    	int LinkCount = links.size();
    	int currentLink = 0;
    	
		// recursively traverse all links on page
		for(Element link: links){
			currentLink++;
			
			
			String linkStr = link.attr("abs:href");
			if(linkStr.equals(""))
			{
				linkStr = link.attr("abs:src");
			}
			
			if(stop)
			{
				ReportFile.report(URL + " break loop", false);
				break;
			}
			
			if (linkStr.endsWith("/")) {
				linkStr= linkStr.substring(0, linkStr.length() - 1);
			}
			if(URL.equals(linkStr))
			{
				//ignore links to self
				continue;
			}
			else if(linkStr.contains(domain))
			{
				ReportFile.report("Seaching : link " + currentLink + " of " + LinkCount, false);
				
				//recursively call self
				processPage(linkStr,nextdepth);
			}
		}
	}
	
	void removeExcessWhiteSpace(StringBuilder wordsFinal)
    {
    	String words = wordsFinal.toString();
        words = words.trim();
        
        words = words.replaceAll("\\s+", " ");
        words = words.trim();
        
        wordsFinal.setLength(0);
        wordsFinal.append(words);
    }
	
	public static String getDomainName(String url) throws URISyntaxException {
	    URI uri = new URI(url);
	    String domain = "";
	    domain = uri.getHost();
	    
	    if(domain==null)
	    {
	    	domain="";
	    }
	    else if(domain.equals(""))
	    {
	    	
	    }
	    else
	    {
	    	domain = domain.startsWith("www.") ? domain.substring(4) : domain;
	    }
	    return domain;
	}
	
	private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g" 
            + "|png|tiff?|mid|mp2|mp3|mp4"
            + "|wav|avi|mov|mpeg|ram|m4v|pdf" 
            + "|doc|ico" 
            + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
	
	public Boolean ShouldCrawl(String Url)
	{
		
		String href = Url.toLowerCase();
		
		//
		// check on same domain
		//
		String currentDomain = "";
		try {
			currentDomain = getDomainName(Url);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	Boolean sameSite = currentDomain.contains(domain);
    	
    	
    	//
    	// check file is html only
    	//
        Boolean suffixGood = !FILTERS.matcher(href).matches();
        
        
        //
        // check real link not #link
        //
        
        Boolean goodLink = !href.contains("#");
        
        //
        // final decision
        //
        Boolean crawl = sameSite && suffixGood && goodLink;
		
        return crawl;
	}
	
	public void start()
	{
		stop = false;
		
		try {
			processPage(rootUrl,0);
		}
		catch (Exception e)
		{
			ReportFile.report("Exception in process page", true);
		}
		
		//get to here and we're finished
		crawlInfo = null;
		runDumperThread = null;
		dumper = null;
		history = null;
	}
	
	public void stop()
	{
		
		if(runDumperThread!=null)
		{
			try
			{
				runDumperThread.interrupt();
			}
			catch (Exception ex)
			{
				ReportFile.report("couldn't stop dumper", true);
				
			}
			
		}
		//	}
		//}
		
		stop = true;
		
		
	}
	
	String rootUrl;
	String domain;
	Boolean stop;
	public Integer maxDepth;
	public Integer timeoutSeconds;
	TreeSet<String> history;
	historyManager processedHistory;
	DumperStep dumper;
	reporter ReportFile;
	Thread runDumperThread;
	CrawlInfo crawlInfo;
}