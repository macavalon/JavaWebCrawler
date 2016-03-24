JavaWebCrawler
=============

A simple java www crawler, within internal support for custom dumper thread

Example use
============================
```
import WebCrawler.webcrawler;
import WebCrawler.historyManager;
import Common.reporter;

profiler Profiler = new profiler();

historyManager mHistoryManager = new historyManager("/");

String fileName = "report.txt";

if (FileF.Exists(fileName))
{
    // remove
    FileF.Delete(fileName);
}

//create report file
reporter mReporter = new reporter(fileName);
mReporter.setUsername("admin");
mReporter.setType("wwwcrawler");

String rootUrl = "www.microsoft.com";

webCrawler webcrawler =  new webCrawler(mHistoryManager, 
                                        mReporter,
                                        rootUrl);
                                        
webCrawler.start();
```


