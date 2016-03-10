package WebCrawler;

import org.jsoup.select.Elements;

public class Page {
	Page(String url, String text, Elements links, String title, String description, String imageLink)
	{
		Url = url;
		Text = text;
		Links = links;
		Title = title;
		Description = description;
		ImageLink = imageLink;
	}
	
	public String Url;
	public String Text;
	public Elements Links;
	public String Title;
	public String Description;
	public String ImageLink;
}
