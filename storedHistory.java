package WebCrawler;

import java.util.Date;

public class storedHistory
{


    // create from article
    public storedHistory(String linkTitle, Date date)
    {
        createdDate = date;
        Title = linkTitle;
        
    }

    //create from saved xml file
    public storedHistory( Date _DateTime,  String _Title)
    {
        createdDate = _DateTime;
        Title = _Title;
    }

    public Date createdDate;
    public String Title;
}
