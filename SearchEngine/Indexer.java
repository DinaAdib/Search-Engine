/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SearchEngine;

import static SearchEngine.Main.Database;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.nodes.*;   // for documents 
import org.jsoup.Jsoup;     // connecting internet
import org.tartarus.snowball.ext.EnglishStemmer;

/**
 *
 * @author pc
 */
public class Indexer implements Runnable {

    public static DB Database;
    private String Url;
    private EnglishStemmer stemmer;
    private String[] Stopping_Words;
//    public static AtomicInteger numberIndexed = new AtomicInteger(823);
//    public static HashSet<String> UrlsIndexed = new HashSet<String>();

    public Indexer() throws SQLException, ClassNotFoundException, IOException {
        this.Database = Main.Database;
        stemmer = new EnglishStemmer();
        String[] Stopping = {"a", "about", "above", "across", "after", "all", "along", "also", "am", "an", "and",
            "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being", "both",
            "between", "but", "by", "can", "can't", "cant", "cannot", "could", "couldn't", "couldnt",
            "did", "didn't", "didnt", "do", "does", "doesn't", "doesnt", "doing", "done", "don't",
            "dont", "during", "each", "either", "for", "from", "given", "had", "has", "have", "having",
            "he", "he'd", "hed", "he'll", "her", "here", "hers", "him", "himself", "his", "how", "how's",
            "hows", "however", "i", "i'd", "i'll", "i'm", "im", "i've", "ive", "if", "in", "instead",
            "into", "is", "isn't", "isnt", "it", "it'll", "itll", "it's", "its", "let's", "lets", "may",
            "me", "more", "most", "much", "must", "my", "no", "not", "now", "of", "on", "one", "only",
            "or", "other", "our", "out", "over", "said", "says", "see", "she", "she'd", "shed", "she'll",
            "shell", "should", "since", "so", "some", "such", "than", "that", "the", "their", "them",
            "then", "there", "therefore", "these", "they", "this", "those", "through", "to", "too",
            "towards", "under", "untill", "us", "use", "used", "uses", "using", "very", "want", "was",
            "wasn't", "wasnt", "we", "we'd", "we'll", "we're", "we've", "weve", "were", "what", "what's",
            "whats", "when", "when's", "whens", "where", "whether", "which", "while", "who", "who'll",
            "wholl", "who's", "whos", "who've", "whove", "will", "with", "with", "within", "without",
            "won't", "would", "wouldn't", "you", "you'd", "youd", "you'll", "you're", "youll", "youre",
            "you've", "youve", "your"};
        Stopping_Words = Stopping;
    }

    @Override
    public void run() {
        try {
            startIndexing();
        } catch (SQLException | IOException ex) {
            Logger.getLogger(Indexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startIndexing() throws SQLException, IOException {
        String sql = "Select COUNT(*)  AS count from Visited where isIndexed=1;"; //Must add where isCrawled = 1 (we must make sure that 5000 pages are crawled)
        ResultSet rs = Database.runSql(sql);
        boolean indexUrl = false;
        int noIndexed = 0;
        while (rs.next()) {
        	noIndexed = rs.getInt("count");
        //	numberIndexed = new AtomicInteger(noIndexed);
        }
        while (noIndexed < Main.Max_Crawled) {
            ResultSet urls = null;
            sql = "Select Url from Visited where isCrawled=1 and isIndexed=0;";
                urls = Database.runSql(sql);
            
            while (urls.next()) {
            	indexUrl = false;
                String url = urls.getString("Url");
//                synchronized(UrlsIndexed) { 
//                	if(!UrlsIndexed.contains(url)) {
//                        UrlsIndexed.add(url);
//                        indexUrl = true;
//                	}
//                }
//                if(indexUrl) {
                sql = "Select Url from Indexer where Url='" + url + "';";
                rs = Database.runSql(sql);
                if (rs.next()) {
                    sql = "delete from Indexer where Url='" + url + "';";
                    int Success = Database.updateSql(sql);
                    if (Success == 0) {
                        System.out.println("Error in deleting from indexer");
                    }
                    // numberIndexed--;  //as it is not a new Document
                }
                Index_Url(url);
                sql = "Update Visited set isIndexed=1 where Url='" + url + "';";
                    int Success = Database.updateSql(sql);
                    if (Success == 0) {
                        System.out.println("error during updating isIndexed of url: " + url);
                }
                noIndexed++;
//                	noIndexed= numberIndexed.incrementAndGet();
                
                System.out.println("                " + noIndexed);
                if (noIndexed == Main.Max_Crawled) {
                    break;
                }
             //   }
            }
        }
        System.out.println("Indexer Finished");
    }

    private void Index_Url(String url) throws SQLException, IOException {
        this.Url = url;
        try {
            String title = "", metaTitle = "", metaDescription = "", p = "", h1 = "", h2 = "", h3 = "", h4 = "";
            String sql = "Select title,metaTitle, metaDescription, paragraph,h1,h2,h3,h4 from Visited where Url='" + url + "';";
            ResultSet rs = null;
                rs = Database.runSql(sql);
            
            if (rs.next()) {
                System.out.println(" Indexing Url:" + url);
                title = rs.getString("title");
                metaTitle = rs.getString("metaTitle");
                metaDescription = rs.getString("metaDescription");
                p = rs.getString("paragraph");
                h1 = rs.getString("h1");
                h2 = rs.getString("h2");
                h3 = rs.getString("h3");
                h4 = rs.getString("h4");
            }
            //String All_Words = title + " " + p + " " + h1 + " " + h2 + " " + h3 + " "
            // + " " + h4;
            String begin = "Begin \n";
            String end = "End";
            //Indexing the words and url
            Indexing(title, "title");
            Indexing(metaTitle, "metaTitle");
            Indexing(metaDescription, "metaDescription");
            Indexing(p, "p");
            Indexing(h1, "h1");
            Indexing(h2, "h2");
            Indexing(h3, "h3");
            Indexing(h4, "h4");
            //int r = Database.updateSql(begin + q1 + q2 + q3 + q4 + q5 + q6 + q7 + end);
//            if (r == 0) {
//                System.out.println("Error during Indexing the words of Url:" + this.Url);
//            }

            //insertFrequency(title, meta, p, h1, h2, h3, h4);
        } catch (SQLException e) {
            System.out.println("Error in Indexer " + e);
        }
    }

    public void Indexing(String str, String Type) throws SQLException {
        String[] Array_Of_Words = str.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+"); // taking the words only 
        ArrayList<String> Indixing_Words = new ArrayList<String>();
        ArrayList<String> Original_Words = new ArrayList<String>();

        for (int i = 0; i < Array_Of_Words.length; i++) {
            boolean Word_Existence = false;
            for (String Stoping_Word : Stopping_Words) {
                if (Array_Of_Words[i] == null ? Stoping_Word == null : Array_Of_Words[i].equals(Stoping_Word)) {
                    Word_Existence = true;
                    break;
                }
            }
            if ((Array_Of_Words[i].length() > 1) && (!Word_Existence)) {
                Original_Words.add(Array_Of_Words[i]);
                stemmer.setCurrent(Array_Of_Words[i]);
                stemmer.stem();
                Array_Of_Words[i] = stemmer.getCurrent();
                Indixing_Words.add(Array_Of_Words[i]);

            }
        }
        String[] Stemmed_String = Indixing_Words.toArray(new String[Indixing_Words.size()]);
        String[] Original_String = Original_Words.toArray(new String[Original_Words.size()]);
        int Position = 1;
        int r;
        String begin = "Begin \n";
        String end = "End";
        String Query = "";
        if (Type.equals("title")) {

            for (int i = 0; i < Stemmed_String.length; i++) {
                Query += "INSERT INTO Indexer (Url , StemmerWord ,OriginalWord, Position , Priority)" + " VALUES('" + this.Url + "','" + Stemmed_String[i] + "','" + Original_String[i] + "','" + Position + "','" + Type + "');\n";
                Position++;
            }

        }
        if (Type.equals("h1") || Type.equals("h2") || Type.equals("h3")
                || Type.equals("h4")) {
            for (int i = 0; i < Stemmed_String.length; i++) {
                Query += "INSERT INTO Indexer (Url , StemmerWord ,OriginalWord, Position , Priority)" + " VALUES('" + this.Url + "','" + Stemmed_String[i] + "','" + Original_String[i] + "','" + Position + "','" + Type + "');\n";
                Position++;
            }

        }
        if (Type.equals("p")) {
            for (int i = 0; i < Stemmed_String.length; i++) {
                Query += "INSERT INTO Indexer (Url , StemmerWord ,OriginalWord, Position , Priority)" + " VALUES('" + this.Url + "','" + Stemmed_String[i] + "','" + Original_String[i] + "','" + Position + "','" + Type + "');\n";
                Position++;
            }

        }
        if (Type.equals("metaTitle")) {
            for (int i = 0; i < Stemmed_String.length; i++) {
                Query += "INSERT INTO Indexer (Url , StemmerWord ,OriginalWord, Position , Priority)" + " VALUES('" + this.Url + "','" + Stemmed_String[i] + "','" + Original_String[i] + "','" + Position + "','" + Type + "');\n";
                Position++;
            }
        }
        if (Type.equals("metaDescription")) {
            for (int i = 0; i < Stemmed_String.length; i++) {
                Query += "INSERT INTO Indexer (Url , StemmerWord ,OriginalWord, Position , Priority)" + " VALUES('" + this.Url + "','" + Stemmed_String[i] + "','" + Original_String[i] + "','" + Position + "','" + Type + "');\n";
                Position++;
            }
        }
        //System.out.println(Query);
        if (!"".equals(Query)) {
            r = Database.updateSql(begin + Query + end);  //insert tag 
            if (r == 0) {
                System.out.println("error during inserting words in Indexer of url=" + this.Url + " and tag=" + Type);
            }
        }
        Frequence(Stemmed_String, Type);
    }

    public void Frequence(String[] stemmed_string, String Type) throws SQLException {
        String begin = "Begin \n";
        String end = "End";
        String Query = "";
        int tf = 0;
        for (int i = 0; i < stemmed_string.length; i++) {
            String sql = "Select count(*) as count from Indexer where Url='" + this.Url + "'and StemmerWord='" + stemmed_string[i] + "' and priority='" + Type + "';";
            ResultSet rs = Database.runSql(sql);
            if (rs.next()) {
                int count = rs.getInt("count");
                if ("p".equals(Type)) {
                    tf = count * 1;
                } else if ("h4".equals(Type)) {
                    tf = count * 2;
                } else if ("h3".equals(Type)) {
                    tf = count * 3;
                } else if ("h2".equals(Type)) {
                    tf = count * 4;
                } else if ("h1".equals(Type)) {
                    tf = count * 5;
                } else if ("metaTitle".equals(Type)) {
                    tf = count * 6;
                } else if ("metaDescription".equals(Type)) {
                    tf = count * 7;
                } else {
                    tf = count * 8;
                }
                Query += "Insert into Frequency(Url,Word,Tag,Count,TF) Values('" + this.Url + "','" + stemmed_string[i] + "','" + Type + "','" + count + "'," + tf + ");\n";
            }
        }
        if(!"".equals(Query))
        {
        int r = Database.updateSql(begin + Query + end);
        if (r == 0) {
            System.out.println("error during inserting into Frequency the words of url=" + this.Url + "and tag=" + Type);
        }
        }

    }

//    public String[] Remove_Stopping_Words(String[] Original_Words) throws SQLException {
//        ArrayList<String> Without_Stopping_Words = new ArrayList<String>();
//        for (String Original_Word : Original_Words) {
//            boolean Word_Existence = false;
//            for (String Stoping_Word : Stopping_Words) {
//                if (Original_Word == null ? Stoping_Word == null : Original_Word.equals(Stoping_Word)) {
//                    Word_Existence = true;
//                    break;
//                }
//            }
//            if ((Original_Word.length() > 1) && (!Word_Existence)) {
//                Without_Stopping_Words.add(Original_Word);
//            }
//        }
//        String[] Without_Stopping_words_String = Without_Stopping_Words.toArray(new String[Without_Stopping_Words.size()]);
//        return (Without_Stopping_words_String);
//    }
//    public void insertFrequency(String title, String meta, String p, String h1, String h2, String h3, String h4) throws SQLException {
//        String begin = "Begin \n";
//        String end = "End";
//        String[] Array_Of_Words = title.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
//        String f1 = Frequence(Array_Of_Words, "title");
//        Array_Of_Words = meta.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
//        String f2 = Frequence(Array_Of_Words, "meta");
//        Array_Of_Words = p.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
//        String f3 = Frequence(Array_Of_Words, "p");
//        Array_Of_Words = h1.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
//        String f4 = Frequence(Array_Of_Words, "h1");
//        Array_Of_Words = h2.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
//        String f5 = Frequence(Array_Of_Words, "h2");
//        Array_Of_Words = h3.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
//        String f6 = Frequence(Array_Of_Words, "h3");
//        Array_Of_Words = h4.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
//        String f7 = Frequence(Array_Of_Words, "h4");
//        Database.updateSql(begin + f1 + f2 + f3 + f4 + f5 + f6 + f7 + end);
//    }
    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
        Indexer i = new Indexer();
        String Url;
        BufferedReader consolereader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.println("Enter Url:");
            Url = consolereader.readLine();
            i.Index_Url(Url);
        }
        //i.Index_Url("https://fifa.com");
        //i.Index_Url("https://hxim.github.io/Stockfish-Evaluation-Guide/");
    }
}
