package SearchEngine;

import static SearchEngine.Crawler.Crawled_Count;
import static SearchEngine.Main.Database;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.lang.ProcessBuilder.Redirect.Type;
import java.net.ConnectException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.DateTime;
import static org.joda.time.format.ISODateTimeFormat.date;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PageCrawler implements Runnable {
    //Queue crawlFrontier (list containing URLs to pages to be visited)

    private Queue<String> Crawl_Frontier;
    private Set<String> Visited_Links;
    private static Set<String> Restricted_Links;
    private Set<String> Restricted_Sources;
    DB Crawler_Database;
    private Document HTML_Document;
    private static final int Max_Links_Per_Page = 15;
    private static final String User_Agent = "Mozilla/5.0 (Windows NT 6.1;WOW64) AppleWebKit/535.1 (KHTML, like Gecko)Chrome/13.0.782.112 Safari/535.1";

    public PageCrawler() {
        Crawl_Frontier = Crawler.Crawl_Frontier;
        Visited_Links = Crawler.Visited_Links;
        this.Crawler_Database = Main.Database;
        Restricted_Links = Crawler.Restricted_Links;
        Restricted_Sources = Crawler.Restricted_Sources;
    }

    public void run() {
        try {
            crawl();
        } catch (IOException e) {
            // TODO Auto-generated catch block
        } catch (SQLException ex) {
            Logger.getLogger(PageCrawler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //Main crawl function. Crawl until stopping criteria
    public void crawl() throws IOException, SQLException {
        String url;
        String sql;
        ResultSet rs;
        

        while (Crawler.Crawled_Count < Main.Max_Crawled) {
            url = getNextURL();
            crawlPage(url);
            sql = "Select COUNT(*)  AS count from Visited where isCrawled=1;"; 
            rs = Database.runSql(sql);

            while (rs.next()) {
               Crawler.Crawled_Count = rs.getInt("count");
            }
        }

        System.out.println("Reached maximum");
        return;

    }

    //Function to get next url to crawl
    public String getNextURL() throws IOException, SQLException {
        String url = "";
        System.out.println(Visited_Links.size());
        boolean dontcrawlthis = true;
        while (dontcrawlthis) {
            synchronized (Crawl_Frontier) {

                if (!Crawl_Frontier.isEmpty()) {
                    try {
                        url = Crawl_Frontier.remove();
                    } catch (NullPointerException npe) {
                    } catch (NoSuchElementException nse) {
                    }
                }
            }
            int Success;
            synchronized (Visited_Links) {
                if (!Visited_Links.contains(url) && Crawler.Crawled_Count < Main.Max_Crawled && url != "") {
                    if (!isRestricted(url)) {
                        dontcrawlthis = false;
                        System.out.println("Inserting into visited " + url);
                        Visited_Links.add(url);
                        String sql = "insert into Visited (Url, isCrawled) Values ('" + url + "',0) ;";
                        Success = Crawler_Database.updateSql(sql);
                        if (Success == 0) {
                            System.out.println("Error during visiting url:" + url);
                        }

                    }

                }
            }
        }
        AtomicInteger inLinks = new AtomicInteger();
        synchronized(Crawler.In_Links) {
        	Crawler.In_Links.put(url, inLinks);
        }
        return url;

    }

//Function to crawl page. Extracts links and examine robots of a given url
    public void crawlPage(String url) {
        if (url == "") {
            return;
        }
        try {
            extractLinks(url);
            examineRobot(url);
        } catch (SocketTimeoutException ste) {
            System.out.println("Timed out");
            return;
        } catch (UnknownHostException | SocketException | MalformedURLException | UncheckedIOException | URISyntaxException ce) {
            return;
        } catch (IOException ioe) {
            return;
        } catch (Exception e) {
            return;
        }
        return;
    }

    //Function to download page document and get urls inside a page
    public void extractLinks(String url) throws IOException, URISyntaxException, UncheckedIOException, SocketTimeoutException, SQLException {

        int Found_Links_Count = 0;
        //System.out.println("Thread " + Thread.currentThread().getId() + " is crawling " + url);
        HashSet<String> Links = new HashSet<String>();
        URL Examined_URL = new URL(url);
        try {
            Connection connection = Jsoup.connect(url).userAgent("Mozilla");
            Connection.Response response = connection.url(url).timeout(0).execute();
            //Get links in page
            if (response.statusCode() == 200 && response.contentType().contains("text/html")) {

                //System.out.println(();
                //    if (httpCon.getResponseCode()== 200 && httpCon.getContentType().contains("text/html")) {
                HTML_Document = connection.get();
                System.out.println("Thread " + Thread.currentThread().getId() + " is crawling " + url + "with response code " + response.statusCode());

                Elements Links_To_Visit = HTML_Document.select("a[href]");
                
                //Add link to our queue
                for (Element link : Links_To_Visit) {
                    if (Found_Links_Count >= Max_Links_Per_Page) {
                        break;
                    }
                    String Abs_Link = link.absUrl("href").toLowerCase();

                    Abs_Link = normalize(Abs_Link);
                    if(Visited_Links.contains(Abs_Link)) {
                    	synchronized(Crawler.In_Links) {
                    		int inLinks = Crawler.In_Links.get(Abs_Link).incrementAndGet();
                    		 String sql = "Update Visited set inLinks = " + inLinks + " where Url = '" + Abs_Link + "';";

                     	//	System.out.println("New inlinks value for url " + Abs_Link + " is " + inLinks);
                             int Success = Crawler_Database.updateSql(sql);
                             if (Success == 0) {
                                // System.out.println("Error during inserting the inlinks of Url:" + url);
                             }
                    	}
                    }
                    if (!Visited_Links.contains(Abs_Link) && !Crawl_Frontier.contains(Abs_Link) && (Abs_Link).toString() != "") {
                        if (!isRestricted(Abs_Link)) {
                            Links.add(Abs_Link);
                        }
                        Found_Links_Count++;
                    }

                }

                synchronized (Crawl_Frontier) {
                    Crawl_Frontier.addAll(Links);
                    for (String myurl : Links) {
                        PrintWriter FrontierWriter = new PrintWriter(new FileWriter(("Crawl_Frontier.txt"), true));
                        FrontierWriter.println(myurl);
                        FrontierWriter.close();
                    }
                }
                insertDocument(url, HTML_Document);
                // System.out.println("Ignored. Invalid type. url = " + url);

            }
        } catch (HttpStatusException e) {
                String sql = "delete from Visited where Url = '" + url + "';";
                int Success = Crawler_Database.updateSql(sql);
                if (Success == 0) {
                    System.out.println("Error during removing Url:" + url);
                }
            synchronized (Crawler.Visited_Links) {
                Visited_Links.remove(url);
            }
            synchronized (Restricted_Links) {
                Restricted_Links.add(url);
            }
            return;
        } catch (IOException | IllegalArgumentException | NullPointerException ex) {
                String sql = "delete from Visited where Url = '" + url + "';";
                int Success = Crawler_Database.updateSql(sql);
                if (Success == 0) {
                    System.out.println("Error during removing Url:" + url);
                }
            synchronized (Visited_Links) {
                Visited_Links.remove(url);
            }
            return;
        }
        return;
    }

    //Function to normalize given url to detect similar urls
    public String normalize(String Abs_Link) throws URISyntaxException {
        //Truncate words after # since links to the same page
        if (Abs_Link.contains("#")) {
            int Hash_Index = Abs_Link.indexOf('#');
            Abs_Link = Abs_Link.substring(0, Hash_Index - 1);
        }

        if (!Abs_Link.endsWith("/")) {
            Abs_Link += "/";
        }
        URI uri = new URI(Abs_Link);
        return Abs_Link = uri.normalize().toString();
    }

    //Function to insert document into database
    public void insertDocument(String url, Document HTML_Document) throws SQLException {
    	 
        String text= "";

        String sql = "";
        ResultSet rs;
        if(HTML_Document.body() != null )
            text=HTML_Document.body().text().toLowerCase();
    	if (!"".equals(text) && !HTML_Document.title().contains("??")) {
        String title = HTML_Document.select("title").text();
        Elements ogTags = HTML_Document.select("meta[property^=og:]");
        if (ogTags.size() <= 0) {
            return;
        }

        // Set OGTags you want
        String metaTitle = null;
        String metaDesc = null;
        String desc;
        for (int i = 0; i < ogTags.size(); i++) {
            Element tag = ogTags.get(i);

            String meta = tag.attr("property");
             if ("og:description".equals(meta)) {
            	 metaDesc = tag.attr("content");
            } else if ("og:title".equals(meta)) {
                metaTitle = tag.attr("content");
            }
        }         
        
        List<String> paragraphs = HTML_Document.select("p").eachText();
        String paragraph = "";
	     // For each selected <p> String, print out its text
	     for (String e : paragraphs) {
	    	 paragraph = paragraph.concat(e + " () ");
	     }
	     
	     List<String> h1s = HTML_Document.select("h1").eachText();
	        String h1 = "";
		     for (String e : h1s) {
		    	 h1 = h1.concat(e +" () ");
		     }
		     
		     List<String> h2s = HTML_Document.select("h2").eachText();
	        String h2 = "";
		     for (String e : h2s) {
		    	 h2 = h2.concat(e +" () ");
		     }
		     
		     List<String> h3s = HTML_Document.select("h3").eachText();
		        String h3 = "";
			     for (String e : h3s) {
			    	 h3 = h3.concat(e + " () ");
			     }
			     
		     List<String> h4s = HTML_Document.select("h4").eachText();
		        String h4 = "";
			     for (String e : h4s) {
			    	 h4 = h4.concat(e + " () ");
					     }

        title = title.replaceAll("'", "''");
        metaTitle = metaTitle.replaceAll("'", "''");
        metaDesc = metaDesc.replaceAll("'", "''");
        paragraph = paragraph.replaceAll("'", "''");
        h1 = h1.replaceAll("'", "''");
        h2 = h2.replaceAll("'", "''");
        h3 = h3.replaceAll("'", "''");
        h4 = h4.replaceAll("'", "''");
        Elements Links_To_Visit = HTML_Document.select("a[href]");
        int OutLinksSize = Links_To_Visit.size();
            sql = "Update Visited set title= '" + title + "',metaTitle ='" + metaTitle + "', metaDescription = '" + metaDesc + "',paragraph ='" + paragraph + "',h1 ='" + h1 + "',h2 ='" + h2 + "',h3 ='" + h3 + "',h4 ='" + h4 + "', outLinks= "+ OutLinksSize+ ", isIndexed =0, isCrawled=1 where Url = '" + url + "';";
            int Success = Crawler_Database.updateSql(sql);

            if (Success == 0) {
                System.out.println("Error during inserting the document of Url:" + url);
            }
//            sql = "Select * from Visited where Url = '" + url + "';";
//
//            rs = Database.runSql(sql);
//            if (rs.next()) {
//            	System.out.println("is crawled is " + rs.getInt("isCrawled"));
//                if (rs.getInt("isCrawled") == 0) {
//
//                    sql = "delete from Visited where Url = '" + url + "';";
//                    Visited_Links.remove(url);
//                    Success = Crawler_Database.updateSql(sql);
//                    if (Success == 0) {
//                        System.out.println("Error during removing Url:" + url);
//                    }
//                } 
 //           }
           

            return;
    	}
    	else {
    		sql = "delete from Visited where Url = '" + url + "';";
            int Success = Crawler_Database.updateSql(sql);
            if (Success == 0) {
                System.out.println("Error during removing Url:" + url);
            }
            Visited_Links.remove(url);
            Restricted_Links.add(url);
    	}
    	return;
        
    }

    //----------------Robots.txt functions---------------
    //Function used to check if a given URL has Robots.txt file
    public boolean examineRobot(String url) throws IOException, SocketTimeoutException, SQLException {
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        String url_robots;
        boolean isExaminedBefore = false;
        //get URL form of given url
        URL Examined_URL = new URL(url);
        int i = url.indexOf("://");
        url_robots = url.substring(0, i + 3);
        url_robots += Examined_URL.getHost();

        //url_robots -> host form only of website since crawler deals with root directory
        URL Robots_URL = new URL(url_robots + "/robots.txt");
        synchronized (Restricted_Sources) {
            if (Restricted_Sources.contains(url_robots)) {
                isExaminedBefore = true;
            } else {
                Restricted_Sources.add(url_robots);

            }
        }
        if (!isExaminedBefore) {
            HttpURLConnection Response = (HttpURLConnection) Examined_URL.openConnection();
            Response.setRequestMethod("HEAD");
            Response.connect();
            if (Response.getResponseCode() == 200) {
                getRestricted(url_robots, Robots_URL);
                return true;
            } else {
                synchronized (Restricted_Sources) {
                    Restricted_Sources.remove(url_robots);
                }
            }
        }
        return false;

    }

    //Function to get list of restricted links
    public void getRestricted(String url, URL Examined_URL) throws UnsupportedEncodingException, IOException, SQLException {
        String Line, Restricted_Link;
        List<String> RestrictedLinks = new LinkedList<String>();

        BufferedReader in = new BufferedReader(new InputStreamReader(Examined_URL.openStream(), "UTF-8"));
        while ((Line = in.readLine()) != null && !Line.startsWith("User-agent: *")) {
        }
        //Reached end of page or User-agent: *
        while ((Line = in.readLine()) != null && !Line.startsWith("User-agent: ")) {
            //Examine disallowed urls for all user-agents
            if (Line.startsWith("Disallow")) {
                if (Line.length() > Line.lastIndexOf("Disallow") + 10) {
                    Restricted_Link = url + Line.substring(Line.lastIndexOf("Disallow") + 10);
                    if (Restricted_Link.endsWith("/")) {
                        Restricted_Link += "*";
                    } else if (Restricted_Link.endsWith("*")) {
                        Restricted_Link += "/*";
                    } else {
                        Restricted_Link += "*/*";
                    }
                    Restricted_Link = Restricted_Link.replaceAll("\\*", ".*");
                    RestrictedLinks.add(Restricted_Link);
                    //System.out.println("Inserting restricted into db");
                        String sql = "Insert into Restricted (restrictedSource, restrictedLink) Values ('" + url + "','" + Restricted_Link + "');";
                        int Success = Crawler_Database.updateSql(sql);
                        if (Success == 0) {
                            System.out.println("Error during inserting the robots of Url:" + url);
                        }
                }
            }
        }
        synchronized (Restricted_Links) {
            Restricted_Links.addAll(RestrictedLinks);
        }
        in.close();
        return;

    }

    //Function to check if a given url is in the restricted list  
    public boolean isRestricted(String url) {
        synchronized (Restricted_Links) {
            for (String Restricted_Link : Restricted_Links) {
                if (Pattern.compile(Restricted_Link).matcher(url).find()) {
                    return true;
                }
            }
            return false;
        }
    }

}
