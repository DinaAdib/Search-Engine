/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.tartarus.snowball.ext.EnglishStemmer;

/**
 *
 * @author Mohamed
 */
public class Search_Rank {

    static DB Database;
    private int totalNoOfPages;
    private static boolean isPhrase;
    private static LinkedList<String> StemmedWords = new LinkedList<String>();;
    private int maxPopularity;
    private EnglishStemmer stemmer;
    private static String[] Stopping_Words;
    static String[] qs_words_without_stopping;
    static LinkedList<LinkSnip> SnippedLinks = new LinkedList<LinkSnip>();
    static HashMap<String, Boolean> inTag = new HashMap<String, Boolean>();
    public Search_Rank(DB Database) throws SQLException {
        stemmer = new EnglishStemmer();
        this.Database = Database;

        totalNoOfPages = CalculateNumberOfPages();
        maxPopularity = 100;
        maxPopularity = getMaxPopularity();
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

    public void QuerySearch(String qs) throws SQLException, InterruptedException {
        deleteRanker();
        String query = "";
        qs = qs.replaceAll("'", ">");
        boolean phrase = false;
        if ('>' == qs.charAt(0) && '>' == qs.charAt(qs.length() - 1)) {
            qs = qs.replaceAll(">", " ");
            phrase = true;
            isPhrase = true;
        }
        if (phrase == false) {
        	isPhrase = false;
            qs = qs.replaceAll("[^a-zA-Z ]", " ").toLowerCase();
            String[] qs_words = qs.split("\\s+");
            qs_words_without_stopping = Remove_Stopping_Words(qs_words);
            for (String qs_word : qs_words_without_stopping) {
                //get stemmerWord
                stemmer.setCurrent(qs_word);
                stemmer.stem();
                qs_word = stemmer.getCurrent();
                query += CalculateRank(qs_word);
            }
        }
        if (phrase == true) {
        	System.out.println("Phrase searching..");
            query += phraseSearching(qs);
        }
        insertRanker(query);
        insertFinalRank();
    }

    private String phraseSearching(String phrase) throws SQLException, InterruptedException {
        String query = "";
        double weight = 0.0;
        int tf = 0;
        double idf = 0;
        double popularity = 0.0;
        String url = "";
        idf = CalculateIDF_phrase(phrase);
        String select = "select Url from Visited where paragraph like '%" + phrase + "%';";
        ResultSet rs = Database.runSql(select);
        tf++;
        weight = (double) tf * idf;
        while (rs.next()) {
            url = rs.getString("Url");
            query += "insert into Ranker values('" + url + "'," + weight + ");\n";
        }

        select = "select Url from Visited where h4 like '%" + phrase + "%';";
        rs = Database.runSql(select);
        tf = tf + 1;
        weight = (double) tf * idf;
        while (rs.next()) {
            url = rs.getString("Url");
            query += "insert into Ranker values('" + url + "'," + weight + ");\n";
        }

        select = "select Url from Visited where h3 like '%" + phrase + "%';";
        rs = Database.runSql(select);
        tf++;
        weight = (double) tf * idf;
        while (rs.next()) {
            url = rs.getString("Url");
            query += "insert into Ranker values('" + url + "'," + weight + ");\n";
        }

        select = "select Url from Visited where h2 like '%" + phrase + "%';";
        rs = Database.runSql(select);
        tf++;
        weight = (double) tf * idf;
        while (rs.next()) {
            url = rs.getString("Url");
            query += "insert into Ranker values('" + url + "'," + weight + ");\n";
        }

        //Thread.sleep(2120);
        select = "select Url from Visited where h1 like '%" + phrase + "%';";
        rs = Database.runSql(select);
        tf++;
        weight = (double) tf * idf;
        while (rs.next()) {
            url = rs.getString("Url");
            query += "insert into Ranker values('" + url + "'," + weight + ");\n";
        }

        select = "select Url from Visited where meta like '%" + phrase + "%';";
        rs = Database.runSql(select);
        tf++;
        weight = (double) tf * idf;
        if(rs != null) {
	        while (rs.next()) {
	            url = rs.getString("Url");
	            query += "insert into Ranker values('" + url + "'," + weight + ");\n";
	        }
        }

        select = "select Url from Visited where title like '%" + phrase + "%';";
        rs = Database.runSql(select);
        tf++;
        weight = (double) tf * idf;
        while (rs.next()) {
            url = rs.getString("Url");
            query += "insert into Ranker values('" + url + "'," + weight + ");\n";
        }
        System.out.println("Query now is " + query);
        return query;
    }

    public void insertFinalRank() throws SQLException {
        String query = "";
        String url = "";
        double weight = 0.0;
        double popularity = 0.0;
        int inLinks = 0;
        String select = "select Url,sum(Weight) as weight from Ranker group by Url;";
        ResultSet rs = Database.runSql(select);
        if(rs== null) return;
        while (rs.next()) {
            url = rs.getString("Url");
            weight = rs.getFloat("weight");
            inLinks=calculateInLinks(url);
            popularity=((double)inLinks)/((double)this.maxPopularity);
            query += "insert into Final_Rank values('" + url + "'," + weight + "," + popularity + ");\n";
        }
        insertRanker(query);
    }

    //get Urls of the word
    //get the tf for the word in its url 
    //get idf of the word in that url
    //get tf-idf
    //get popularity of the url
    //return query whic will be inserted in Ranker table 
    private String CalculateRank(String word) throws SQLException {
        String query = "";
        String url = "";
        double tf = 0.0;
        double idf = 0.0;
        double tf_idf = 0.0;
        String finalQuery = "";
        query = "Select DISTINCT Url from Frequency where Word='" + word + "';";
        ResultSet rs = Database.runSql(query);
        if(rs == null) return "";
        while (rs.next()) {
            url = rs.getString("Url");
            tf = CalculateTF(word, url);
            idf = CalculateIDF(word);
            tf_idf = tf * idf;
            finalQuery += getQueryRanker(url, tf_idf);
        }
        return finalQuery;
    }

    private double CalculateTF(String word, String url) throws SQLException {
        String query = "";
        int tf = 0;
        int numberOfWordsPerUrl = 0;
        query = "select sum(TF) as sum from Frequency where Url='" + url + "' and Word='" + word + "';";
        ResultSet rs = Database.runSql(query);
        if(rs == null) return 0;
        if (rs.next()) {
            tf = rs.getInt("sum");
        }

        numberOfWordsPerUrl = calculateNumberOfwordsIn(url);
        if(numberOfWordsPerUrl!=0)
        	return ((double) tf) / ((double) numberOfWordsPerUrl);
        else
        	return 0;
    }

    private int calculateNumberOfwordsIn(String url) throws SQLException {
        int numberOfWords = 0;
        String query = "select count(Word) as count from Frequency where Url='" + url + "'; ";
        ResultSet rs = Database.runSql(query);
        if(rs == null) return 0;
        if (rs.next()) {
            numberOfWords = rs.getInt("count");
        }

        return numberOfWords;
    }

    private double CalculateIDF(String word) throws SQLException {
        String query = "";
        double idf = 0.0;
        int numberOfPagesContainsTerm = 0;
        query = "select count(DISTINCT Url) as count from Indexer where StemmerWord='" + word + "';";
        ResultSet rs = Database.runSql(query);
        if(rs == null) return 0;
        if (rs.next()) {
            numberOfPagesContainsTerm = rs.getInt("count");
        }

        if(numberOfPagesContainsTerm !=0) 
            idf = ((double) totalNoOfPages) / ((double) numberOfPagesContainsTerm);
            else idf= 0;
            return idf;
    }

    private double CalculateIDF_phrase(String phrase) throws SQLException {
        String query = "";
        double idf = 0.0;
        int numberOfPagesContainsTerm = 0;
        query = "select count(DISTINCT Url) as count from Visited where paragraph like '%" + phrase + "%' or title like '%" + phrase + "%' or meta like '%" + phrase + "%' or h1 like '%" + phrase + "%' or h2 like '%" + phrase + "%' or h3 like '%" + phrase + "%' or h4 like '%" + phrase + "%';";
        ResultSet rs = Database.runSql(query);
        if(rs == null) return 0;
        if (rs.next()) {
            numberOfPagesContainsTerm = rs.getInt("count");
        }

        if(numberOfPagesContainsTerm !=0) 
            idf = ((double) totalNoOfPages) / ((double) numberOfPagesContainsTerm);
            else idf= 0;
            return idf;
    }

    private int calculateInLinks(String url) throws SQLException {
        String query = "";
        int inLinks = 0;
        query = "select inLinks from Visited where Url='" + url + "'";
        ResultSet rs = Database.runSql(query);
        if(rs == null) return 0;
        if (rs.next()) {
            inLinks = rs.getInt("inLinks");
        }
        return inLinks;
    }

    private String getQueryRanker(String url, double tf_idf) throws SQLException {
        String query = "insert into Ranker values('" + url + "'," + tf_idf + ");\n";
        return query;
    }

    private void insertRanker(String query) throws SQLException {
        String begin = "begin \n";
        String end = "end";
        if (!"".equals(query)) {
            int success = Database.updateSql(begin + query + end);
            if (success == 0) {
                System.out.println("error during inserting into Ranker table");
            }
        }
    }

    public ArrayList<String> sortUrls() throws SQLException {
        ArrayList<String> sortedUrls = new ArrayList<String>();
        String query = "Select Url from Final_Rank order by weight DESC,Popularity DESC;";
        ResultSet rs = Database.runSql(query);
        while (rs.next()) {
            sortedUrls.add(rs.getString("Url"));
        }

        return sortedUrls;
    }

    public Map<String, String> getUrlsContains(String[] words) throws SQLException {
        String url = "";
        String priority = "";
        Map<String, String> urls = new HashMap<String, String>();
        String query = "select Url,Priority from Indexer where OriginalWord='" + words[0] + "' ";
        for (int i = 0; i < words.length - 1; i++) {
            query += "intersect select Url,Priority from Indexer where OriginalWord='" + words[i + 1] + "' ";
        }
        query += ";\n";
        ResultSet rs = Database.runSql(query);
        while (rs.next()) {
            url = rs.getString("Url");
            priority = rs.getString("Priority");
            urls.put(url, priority);
        }
        System.out.println(urls);
        System.out.println("Urls size=" + urls.size());
        return urls;
    }

    public ArrayList<Integer> getPositionsOf(String word, String url, String priority) throws SQLException {
        ArrayList<Integer> positions = new ArrayList<Integer>();
        String query = "select Position from Indexer where Url='" + url + "' and Priority='" + priority + "' and OriginalWord='" + word + "';";
        ResultSet rs = Database.runSql(query);
        while (rs.next()) {
            positions.add(rs.getInt("Position"));
        }
        return positions;
    }

    public boolean checkPositionsOf(String[] words, String url, String priority, int position) throws SQLException {
        String query = "select Url from Indexer where Url='" + url + "' and Priority='" + priority + "' and Position=" + position + " and OriginalWord='" + words[0] + "' ";
        for (int i = 0; i < words.length - 1; i++) {
            query += "intersect select Url from Indexer where Url='" + url + "' and Priority='" + priority + "' and Position=" + (position + i + 1) + " and OriginalWord='" + words[i + 1] + "' ";
        }
        query += ";\n";
        System.out.println(query);
        ResultSet rs = Database.runSql(query);
        if (rs.next()) {
            return true;
        } else {
            return false;
        }
    }

    public Map<String, String> PhraseSearchingPositions(String[] words) throws SQLException {
        String url = "";
        String key = "";
        String val = "";
        boolean flag = false;
        Map<String, String> Urls = new HashMap<String, String>();
        ArrayList<String> removedUrls = new ArrayList<String>();
        ArrayList<Integer> positions = new ArrayList<Integer>();
        Urls = getUrlsContains(words);
        for (Map.Entry<String, String> entry : Urls.entrySet()) {
            key = entry.getKey();
            val = entry.getValue();
            positions = getPositionsOf(words[0], key, val);
            System.out.println("Url=" + key + " Priority=" + val + " Positions=" + positions.size());
            for (int i = 0; i < positions.size(); i++) {
                if (checkPositionsOf(words, key, val, positions.get(i))) {
                    flag = true;
                    break;
                }
            }
            if (flag == false) {
                removedUrls.add(key);
                System.out.println("Removed");
            }
            flag = false;
        }
        for (int i = 0; i < removedUrls.size(); i++) {
            Urls.remove(removedUrls.get(i));
        }
        return Urls;
    }

    private int CalculateNumberOfPages() throws SQLException {
        String query = "";
        int count = 0;
        query = "Select count(*) as count from Visited where isIndexed='1';";
        ResultSet rs = Database.runSql(query);
        if (rs.next()) {
            count = rs.getInt("count");
        }
        return count;
    }

    private int getMaxPopularity() throws SQLException {
        String query = "";
        int count = 0;
        query = "Select max(inLinks) as max from Visited where isIndexed='1' ;";
        ResultSet rs = Database.runSql(query);
        if (rs.next()) {
            count = rs.getInt("max");
        }
        System.out.println("max popularity="+count);
        return count;
    }

    private void deleteRanker() throws SQLException {
        String query = "delete from Ranker;\n"
                + "delete from Final_Rank;\n ";
        int success = Database.updateSql("Begin \n" + query + "End");
        if (success == 0) {
            System.out.println("error in deleting ranker");
        }
    }

    public static String[] Remove_Stopping_Words(String[] Original_Words) throws SQLException {
        ArrayList<String> Without_Stopping_Words = new ArrayList<String>();
        for (String Original_Word : Original_Words) {
            boolean Word_Existence = false;
            for (String Stoping_Word : Stopping_Words) {
                if (Original_Word == null ? Stoping_Word == null : Original_Word.equals(Stoping_Word)) {
                    Word_Existence = true;
                    break;
                }
            }
            if ((Original_Word.length() > 1) && (!Word_Existence)) {
                Without_Stopping_Words.add(Original_Word);
            }
        }
        String[] Without_Stopping_words_String = Without_Stopping_Words.toArray(new String[Without_Stopping_Words.size()]);
        return (Without_Stopping_words_String);
    }
    public static String BoldQuery(String Snippet, String word) {
    	System.out.println("word to replace : " + word);
    	String iword = "(?i)" + word;
    	Snippet=Snippet.replaceAll("\\b("+iword+")\\b\\s*","<b>"+word+"</b>")
    			.replaceAll("\\b("+iword+"."+"|"+iword+":"
                +"|"+iword+"!"+"|"+iword+"?"+"|"+iword+","+")\\b\\s*","<b>"+word+" </b>");
    	return Snippet;
    }
    public static String ExtractSnippet(String text, String SearchQuery, String h1, String MetaDescription) {
    	String Snippet = "";
    	int StartingIndex = 0;
    	int EndIndex = text.length() - 1;
    	int EndSentIndex = 0, StartSentIndex = 0;
    	text=text.replaceAll("\\?\\?", "");
    	int MinWordIndex = text.length() -1;
    	String[] words = null;
    	if(!isPhrase) 
			 words = SearchQuery.split(" ");
    	//	System.out.println("Text contains our search query");
    	//	System.out.println("Text length is " + text.length() );

    			
    			if(text.contains(SearchQuery)) {

    				System.out.println("Found query");
    		    	StartingIndex = text.indexOf(SearchQuery, StartingIndex);
    		    }
    			if(!isPhrase) {
    			    	for(String word : qs_words_without_stopping) { 
    			    		if(text.indexOf(word, StartingIndex) < MinWordIndex) {
    			    			MinWordIndex = text.indexOf(word, StartingIndex);
    			    	   }
			    		if(MinWordIndex < text.length() - 1 && MinWordIndex != -1)
    			    	StartingIndex = MinWordIndex;
    		    	}
    			}
    			
    			System.out.println("Starting Index is " + StartingIndex);
    			StartSentIndex = text.lastIndexOf(" () ", StartingIndex);
    			if(StartSentIndex == -1) StartSentIndex = 0;
    			else if (StartSentIndex + 4 < text.length() -1)  StartSentIndex = StartSentIndex + 4;
        		if(StartSentIndex < EndIndex && StartSentIndex != -1) {
        			EndSentIndex = text.indexOf(" () ", StartSentIndex);
        			System.out.println("EndSentIndex is " + EndSentIndex);
        			if(EndSentIndex!=-1) {
        				
        				if(EndSentIndex - StartSentIndex > 180) {

        					System.out.println("greater than 180");
        					if(text.indexOf(" ", StartSentIndex + 180) != -1)
        						return Snippet=text.substring(StartSentIndex,text.indexOf(" ", StartSentIndex + 180))+ "...<br>";
        					else
        						return Snippet=text.substring(StartSentIndex,StartSentIndex + 180) + "...<br>";
        				}
        				else {
        					if(!Snippet.contains(text.substring(StartSentIndex,EndSentIndex)) && (EndSentIndex - StartSentIndex > 50)) {
        						System.out.println("Adding " + text.substring(StartSentIndex,EndSentIndex));
	        					Snippet=Snippet.concat(text.substring(StartSentIndex,EndSentIndex));
	        					if(!text.substring(StartSentIndex,EndSentIndex).equals("")) {
	        						Snippet = " " + Snippet.concat(".");
	        					}
	        					if(Snippet.length()>160) return Snippet.concat("..<br>");
        					}
        					
        				}
        			}
        		}
        		StartingIndex = EndSentIndex+4;
    		
    	
		if(Snippet.equals("")) {
			StartingIndex = 0;
			if((EndSentIndex = h1.indexOf(" () ", StartingIndex)) > MetaDescription.length()) {
				System.out.println("Adding h1");
				StartingIndex = h1.lastIndexOf(" () ", StartingIndex);
				if(StartingIndex == -1) StartingIndex = 0;
				return Snippet=h1.substring(StartingIndex, h1.indexOf(" () ", StartingIndex)) + "...<br>";
			}
			else {
				MetaDescription.replaceAll(" () ", "..");
				return Snippet=MetaDescription.substring(StartingIndex,MetaDescription.length())+ "...<br>";
			}
		}
		return Snippet.concat("..<br>");
    	
    }
    public static void GetSnippets(Search_Rank Ranker, ArrayList<String> sortedUrls, String SearchQuery) throws SQLException {
    	LinkedList<String> Titles = new LinkedList<String>();
    	LinkedList<String> Snippets = new LinkedList<String>();
        String text = "";
        String QueryModified = SearchQuery;
        String Phrase = "";
        if(isPhrase) {
        	QueryModified = "'" + SearchQuery + "'";
        	System.out.println("Modified query is " + QueryModified);
        	SearchQuery = SearchQuery.substring(1, SearchQuery.length() - 1);
        	System.out.println("Phrase now is " + SearchQuery);
        }

        String[] words = SearchQuery.split(" ");
        System.out.println("Sorted urls size is " + sortedUrls.size());
    	for(int i=0; i<sortedUrls.size();i++) {
    		LinkSnip Snip = new LinkSnip(); 
    		text = "";
    		 String sql = "";
    		 int index = 0 ;
    	        try {
    	        	String[] tags = {"h1", "h2", "h3", "h4", "paragraph","metaDescription"};
    	        	String h1 = "", MetaDescription = ""; 
    	            sql = "select * from Visited where Url ='"+ sortedUrls.get(i)+"';";
        	        ResultSet rs = Database.runSql(sql);

    	            Snip.Url = sortedUrls.get(i);
    	            while (rs.next()) {
    	              Snip.Title = rs.getString("metaTitle");
    	              if(Snip.Title.equals("")) Snip.Title = rs.getString("Title");
    	              Snip.Title = Snip.Title.replaceAll("\\?\\?", "");
    	              for(String tag : tags) {
	    	            	  text = text.concat(rs.getString(tag) + " () ");
    	              }
    	              h1 = rs.getString("h1");
    	              MetaDescription = rs.getString("metaDescription");
    	            }
    	            text = text.replaceAll("\\?\\?", "");
    	            if(!Titles.contains(Snip.Title)) {
    	            	Titles.add(Snip.Title);
    	            	Snip.Snippet = "";
        	            Snip.Snippet = ExtractSnippet(text, SearchQuery, h1, MetaDescription);
        	            Snip.Snippet = Snip.Snippet.replaceAll("\\?\\?", "");
        	            if(!Snippets.contains(Snip.Snippet)) {
        	            	
        	            	Snippets.add(Snip.Snippet);
	        	            System.out.println("Snippet for url " + sortedUrls.get(i) + " is " + Snip.Snippet);
	        	            if(isPhrase) {
	        	        		Snip.Snippet = BoldQuery(Snip.Snippet, SearchQuery + " ");
	        	            }
	        	            else {
	        	            	SearchQuery = SearchQuery.replaceAll("[^a-zA-Z ]", " ");
	        	                String[] qs_words = SearchQuery.split("\\s+");
	        	                words = Remove_Stopping_Words(qs_words);
	        	            	for(String word: words)
	        	            	Snip.Snippet = BoldQuery(Snip.Snippet, word + " ");
	        	            }
	
	                   	 	SnippedLinks.add(Snip);
	                   	 	if(SnippedLinks.size() > 100) break;
        	            }
        	        } 
    	           }catch (NullPointerException e1) {
        	            System.out.println("No url found");
        	        }
    	        	
    	}
    }
  
    public static void main(String args[]) throws SQLException, ClassNotFoundException, InterruptedException {
        Database = new DB();
		
        String searchQuery = "'Prince'";
        Search_Rank qp = new Search_Rank(Database);
        qp.QuerySearch(searchQuery);
        ArrayList<String> sortedUrls = qp.sortUrls();
        GetSnippets(qp, sortedUrls, searchQuery);
        System.out.println(sortedUrls.size());
        
        System.out.println("SnippedLinks Size is " + SnippedLinks.size());
        System.out.println("Query Processor Finished");
    }
}
