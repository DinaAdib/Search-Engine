package SearchEngine;

import static SearchEngine.Main.Database;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;

public class Crawler implements Runnable {

    public static List<Thread> threads = new LinkedList<Thread>();
 org.joda.time.format.DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
       
    //Queue crawlFrontier (list containing URLs to pages to be visited)
    static Queue<String> Crawl_Frontier = new LinkedList<String>();
    static Map<String, AtomicInteger> In_Links = new HashMap<String, AtomicInteger>();
    static Map<String, AtomicInteger> Out_Links = new HashMap<String, AtomicInteger>();
    static Set<String> Visited_Links = new HashSet<String>();
    static Set<String> Restricted_Links = new HashSet<String>();
    static Set<String> Restricted_Sources = new HashSet<String>();
    static int Crawled_Count;

    //initialize crawlFrontier with seeds list
    static String[] Seed_List = {
        "https://www.wikipedia.com", "http://www.fifa.com/"};

    public static void readFiles() throws FileNotFoundException, IOException, SQLException {
        String sql = "";
        ResultSet rs;
        try {
            sql = "Select Url from Visited where isCrawled = 0;";
            rs = Database.runSql(sql);

            while (rs.next()) {
               Crawl_Frontier.add(rs.getString("Url"));
            }
        } catch (NullPointerException e1) {
            System.out.println("Error during inserting frontier");
        }
        try {
            sql = "Select Url from Visited where isCrawled = 1;";
            System.out.println("Inserting into visited");
            System.out.println("rs value is ");
            rs = Database.runSql(sql);

            while (rs.next()) {
                System.out.println("Inserting " + rs.getString("Url"));
                Visited_Links.add(rs.getString("Url"));

            }

        } catch (NullPointerException e2) {
            System.out.println("Error during inserting Visited");
        }
        try (BufferedReader br = new BufferedReader(new FileReader("Crawl_Frontier.txt"))) {
            String line = "";
            while ((line = br.readLine()) != null && line!= " ") {
                Crawl_Frontier.add(line);
            }
        }
        try {
            int Success = 0;
            sql = "delete from Visited where isCrawled = 0;";
            Success = Database.updateSql(sql);
            if (Success == 0) {
                System.out.println("Error during removing url");
            }
        } catch (NullPointerException e3) {
            System.out.println("Error during deleting notCrawled");
        }

        try {
            sql = "Select restrictedSource from Restricted;";
            rs = Database.runSql(sql);

            while (rs.next()) {
                Restricted_Sources.add(rs.getString("restrictedSource"));
            }

        } catch (NullPointerException e4) {
            System.out.println("Error during inserting restrictedSourses");
        }
        try {
            sql = "Select restrictedLink from Restricted;";
            rs = Database.runSql(sql);

            while (rs.next()) {
                Restricted_Links.add(rs.getString("restrictedLink"));
            }

        } catch (NullPointerException e5) {
            System.out.println("Error during inserting restrictedLinks");
        }
    }

    Crawler() throws InterruptedException, IOException, SQLException, ClassNotFoundException {
       
        PrintWriter FinishedWriter = new PrintWriter(new FileWriter(("Finished.txt"), true));
        //Create files to read from, boolean true for appending (doesn't overwrite old file)
        PrintWriter FrontierWriter = new PrintWriter(new FileWriter(("Crawl_Frontier.txt"), true));

        //Read files
       
    }
   public boolean recrawlNow() throws SQLException{String line = "";
       
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("Finished.txt"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, null, ex);
        }

        DateTime start = null;
        try {
            while ((line = br.readLine()) != null) {
                start = formatter.parseDateTime(line);
            }
        } catch (IOException ex) {
            Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, null, ex);
        }
        DateTime end = new org.joda.time.DateTime();
        
        //System.out.println(Days.daysBetween(start.withTimeAtStartOfDay(), end.withTimeAtStartOfDay()).getDays());
        if (Days.daysBetween(start.withTimeAtStartOfDay(), end.withTimeAtStartOfDay()).getDays() >= 0) {
             return true;
        }
        else return false;
       
   }
public void Crawl() throws SQLException{
 System.out.println("Initial size of visited links is " + Visited_Links.size());
 Crawled_Count = Visited_Links.size();
            for (int i = 0; i < Main.Threads_Count; i++) {
                Thread t = new Thread(new PageCrawler());
                if (t != null) {
                    threads.add(t);
                }
                t.start();
            }
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            System.out.println("returned");
            try {
            int Success = 0;
            String sql = "delete from Visited where isCrawled = 0;";
            Success = Database.updateSql(sql);
            if (Success == 0) {
                System.out.println("Error during removing url");
            }
           
           
        } catch (NullPointerException e3) {
            System.out.println("Error during deleting notCrawled");
        }
            PrintWriter FinishedWriter = null;
            try {
                FinishedWriter = new PrintWriter(new FileWriter(("Finished.txt")));
            } catch (IOException ex) {
                Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, null, ex);
            }
            	FinishedWriter.println(new org.joda.time.DateTime().toString(formatter));
               FinishedWriter.close();
}
    public void run() { 
        String sql;
        ResultSet rs;
        int VisitedCount = 0;
        try {
            sql = "Select COUNT(*)  AS count from Visited where isCrawled=1;"; //Must add where isCrawled = 1 (we must make sure that 5000 pages are crawled)
            rs = Database.runSql(sql);

            while (rs.next()) {
               VisitedCount = rs.getInt("count");
            }

        } catch (NullPointerException e5) {
            System.out.println("Error when getting the count");
        } catch (SQLException ex) {
            Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Visited count is "+ VisitedCount);
        if(VisitedCount >= Main.Max_Crawled){
            try {
                if(recrawlNow()){
                     try {
                        sql = "Select Url from Visited;";
                        rs = Database.runSql(sql);

                        while (rs.next()) {
                            Crawl_Frontier.add(rs.getString("Url"));
                        }
                          int Success = 0;
                        sql = "Update Visited set isCrawled = 0;";
                       Success = Database.updateSql(sql);
                       if (Success == 0) {
                           System.out.println("Error during removing url");
                       }
                       Crawl();
            
                    } catch (NullPointerException e1) {
                        System.out.println("Error during inserting frontier");
                    }
                }
                else System.out.println("Still not time to recrawl!");
            } catch (SQLException ex) {
                Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else {
            try {
                readFiles();
            } catch (IOException ex) {
                Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SQLException ex) {
                Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, null, ex);
            }
//            for (int i = 0; i < Seed_List.length; i++) {
//                Crawl_Frontier.add(Seed_List[i]);
//            }
         //   System.out.println("Crawl Frontier: "+ Crawl_Frontier);
         //   System.out.println("Crawl Visited: "+ Visited_Links);
            try {
                Crawl();
            } catch (SQLException ex) {
                Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, null, ex);
            }
           
        }

        //FrontierWriter.close();
        return;
    }
}

