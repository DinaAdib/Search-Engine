

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;




/**
 * Servlet implementation class SearchResults
 */
@WebServlet(name = "SearchResults", urlPatterns = {"/SearchResults"})
public class SearchResults extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static DB Database;
	static LinkedList<LinkSnip> SnippedLinks = new LinkedList<LinkSnip>();
	String Search_Query,Current_Page;
	int CurrentPageIndex = 0, LinkStartingIndex = 0,ResultsCount = 0;
	 org.joda.time.format.DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
     static int LinksPerPage = 10;
     Search_Rank qp;
    public SearchResults() throws ClassNotFoundException, SQLException {
      	connectDB();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

			  // Set response content type
		//Connection con = null;
			  response.setContentType("text/html");
			  Search_Query = request.getParameter("SearchText");
			  Current_Page = request.getParameter("CurrentPage");
				

				  try(PrintWriter out = response.getWriter()){

					  try {
						  writeHeadTag(out);
						  displaySearchbar(out);
						  
						  //Rank here since first time to search
						  if(Current_Page == null) {
							  Current_Page = "1";
						   SnippedLinks.clear();
						   connectDB();

					       
					       if(!getSnippedLinks(Search_Query) || needsUpdate(Search_Query)) { 
					    	   
					    	   rankUrls(Search_Query);
					    	   insertQueryResults(Search_Query);
					       }
					       else {
					    	   updateQuery(Search_Query);
					    	}
						  }

							ResultsCount = (SnippedLinks).size();
							
							//if no results found
							if(ResultsCount == 0) {
								out.println("<h5>No results found.</h5>");
							}
							System.out.println("Results count is " + ResultsCount);
							
							//End Index of pages
							int End =(int) Math.ceil((double)ResultsCount/(double)LinksPerPage);
			
							//get current page index
							CurrentPageIndex = getCurrentPageIndex(Current_Page, End);
						  
						  //get index of first link in page
						  LinkStartingIndex = (CurrentPageIndex - 1) * LinksPerPage;
						  System.out.println("Number of pages is "+End + ", Results count is " + ResultsCount);
						  
						  displayResults(out);
						 
						  writeFooterTag(Search_Query, CurrentPageIndex, End, out);
						  
				          writeScriptsTag(out);
					  
					} catch (SQLException e1) {
						out.println("Oops! Server is down. Please try again later..");
					}
				 }
				catch (InterruptedException e) {
				}
	}

	private boolean needsUpdate(String search_Query) throws SQLException {
		// TODO Auto-generated method stub
		
		DateTime start = null;
		String startString = "";
	   	 
	        String query = "select LastModified from SearchQueries where SearchQuery='" + Search_Query + "'; ";
	        ResultSet rs = Database.runSql(query);
	        if(rs == null) return true;
	        if (rs.next()) {
	       	 startString = rs.getString("LastModified");
	       	 start = formatter.parseDateTime(startString);
	        }

		 DateTime end = new org.joda.time.DateTime();
	        //System.out.println(Days.daysBetween(start.withTimeAtStartOfDay(), end.withTimeAtStartOfDay()).getDays());
	        if (Days.daysBetween(start.withTimeAtStartOfDay(), end.withTimeAtStartOfDay()).getDays() >= 3) {
	             return true;
	        }
		return false;
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub

	   	Search_Query = "";
		doGet(request, response);
		
	}

    protected void writeHeadTag(PrintWriter out) {
    	        out.println("<!DOCTYPE html>");
    	            out.println("<html>");
    	            out.println("<head>");
    	            out.println("<meta charset=\"utf-8\">\r\n" + 
    	            		"    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">\r\n" + 
    	            		"\r\n" + 
    	            		"  \r\n" + 
    	            		"    <!-- Bootstrap CSS -->\r\n" + 
    	            		"   <!-- Bootstrap CSS -->\r\n" + 
    	            		"    <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css\" integrity=\"sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm\" crossorigin=\"anonymous\">\r\n" + 
    	            		"	<link rel=\"stylesheet\" href=\"http://ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/themes/smoothness/jquery-ui.css\" /> \r\n" + 
    	            		"     ");
    	            out.println("<title></title>\n"+
    	                    "  <style>\r\n" + 
    	                    "            #SearchBar{\r\n" + 
    	                    "                padding-left: 10px;\r\n" + 
    	                    "            }\r\n" + 
    	                    "            #SearchText{\r\n" + 
    	                    "                width:500px;\r\n" + 
    	                    "            }\r\n" + 
    	                    "            footer{\r\n" + 
    	                    "                width: 100%;\r\n" + 
    	                    "                position: relative;\r\n" + 
    	                    "                bottom: 0px; \r\n" + 
    	                    "           margin: 0 auto; }\r\n" + 
    	                    "            p, h4{\r\n" + 
    	                    "                margin: 0;\r\n" + 
    	                    "            }\r\n" + 
    	                    "            \r\n" + 
    	                    "            .Link{\r\n" + 
    	                    "                padding-bottom: 20px;\r\n" + 
    	                    "            }\r\n" + 
    	                    "            a:hover{\r\n" + 
    	                    "                text-decoration: none;\r\n" + 
    	                    "                \r\n" + 
    	                    "            }\r\n" +
    	                    "			a:visited{\r\n" + 
    	                    "             color:purple;\r\n" + 
    	                    "                \r\n" + 
    	                    "            }\r\n" + 
    	                    "        </style>\r\n" + 
    	                    "   <title>Doodle</title> </head>"
    	                    );
    }
    
    protected void displaySearchbar(PrintWriter out) {
    	String QueryModified = Search_Query;
    	if(Search_Query.startsWith("'") && Search_Query.endsWith("'")) {
   		 QueryModified = Search_Query.substring(1, Search_Query.length() - 1);
	        }
		  out.println("<body>\r\n" + 
		  		"        <br>\r\n" + 
		  		"        <div class=\"container\">\n<div class =\"input-group-append\">\r\n" + 
		  		"                <a href=\"/SearchInterface\"><h3>Doodle</h3></a>\r\n" + 
		  		"                <form action=\"SearchResults\" class =\"input-group-append\" id = \"SearchBar\" method=\"GET\">\r\n" + 
		  		"                    <input type=\"text\" name=\"SearchText\" id=\"SearchText\" class=\"form-control\" value = '"+QueryModified +"' aria-label=\"\\\" aria-describedby=\"basic-addon2\">\r\n" + 
		  		"                    <button type=\"submit\" value=\"Submit\" class=\"btn btn-primary\">Search</button>\r\n" + 
		  		"                </form>\r\n" + 
		  		"            </div>\r\n" + 
		  		"            <br>\r\n" + 
		  		"            <br>");
    }
    protected void connectDB() {//PrintWriter out
    	try {
			Database = new DB();
		} catch (ClassNotFoundException | SQLException e) {
		}
    }
    protected void insertQueryResults(String Search_Query) throws SQLException {
    	int UrlRank =1;
    	int success;
    	String query = "";
    	DateTime todaysDate = new org.joda.time.DateTime(); 
    	//Insert into SearchQueries table
    	 query = "insert into SearchQueries (SearchQuery, SearchedCount) values('" + Search_Query.replaceAll("'", "''") + "'," + 1 + ");\n";
		success = Database.updateSql(query);
        if (success == 0) {
            System.out.println("error during inserting into SearchQueries table");
        }
    	
    	//Insert into LinkSnippet table
    	for(LinkSnip snip : SnippedLinks) {
    		query = "insert into LinkSnippet values('" + Search_Query.replaceAll("'", "''") + "','" + snip.Url.replaceAll("'", "''") + "','" + snip.Title.replaceAll("'", "''") + "','" + snip.Snippet.replaceAll("'", "''") + "','" + UrlRank +  "');\n";
    		success = Database.updateSql(query);
            if (success == 0) {
                System.out.println("error during inserting into LinkSnippet table");
                System.out.println("Values inserted " + Search_Query + ", " + snip.Url + ", " + snip.Title + ", " + snip.Snippet + ", " + UrlRank);
        		
            }
            UrlRank++;
    	}
    	
    }
    
    protected void updateQuery(String Search_Query) throws SQLException {
    	//Get current count
   	 int SearchedCount = 1;
   	 
        String query = "select SearchedCount as count from SearchQueries where SearchQuery='" + Search_Query + "'; ";
        ResultSet rs = Database.runSql(query);
        if(rs == null) return;
        if (rs.next()) {
       	 SearchedCount = rs.getInt("count");
        }

        int success = 0;
        SearchedCount++;
      //Insert into SearchQueries table
   	 query = "update SearchQueries set SearchedCount = " + SearchedCount + " where SearchQuery = '" + Search_Query + "';\n";
		success = Database.updateSql(query);
       if (success == 0) {
           System.out.println("error during updating count of SearchQueries table");
       }
   	
    }
    protected boolean getSnippedLinks(String Search_Query) throws SQLException {
    	System.out.println("Getting snipped links");
    	boolean hasQuery = false;
    	String QueryModified = Search_Query;
    	 if(Search_Query.startsWith("'") && Search_Query.endsWith("'")) {
    		 QueryModified = "'" + Search_Query + "'";
	        }
    	 String query = "Select * from SearchQueries where SearchQuery = '" + QueryModified + "'";
         ResultSet rs = Database.runSql(query);
         
         if(rs.next()) {
	    	 System.out.println("New query is " + QueryModified);
	    	 query = "Select * from LinkSnippet where SearchQuery = '" + QueryModified + "' order by Rank;";
	         rs = Database.runSql(query);
	        
	        while(rs.next()) {
	        	hasQuery = true;
	        	LinkSnip snip = new LinkSnip(); 
	        	snip.Url = rs.getString("Url");
	        	snip.Title = rs.getString("Title");
	        	snip.Snippet = rs.getString("Snippet");
	        	SnippedLinks.add(snip);
	        	System.out.println("Current size is " + SnippedLinks.size());
	        }
         }
        return hasQuery;
    }
    
    protected void rankUrls(String Search_Query) throws SQLException, InterruptedException {

  	  qp = new Search_Rank(Database);
  	  ArrayList<String> sortedUrls = new ArrayList<String>();
        qp.QuerySearch(Search_Query);
        sortedUrls = qp.sortUrls();
        System.out.println("Sorted Urls are "+sortedUrls);
  	Search_Rank.GetSnippets(qp, sortedUrls, Search_Query);
  	SnippedLinks = Search_Rank.SnippedLinks;

  	System.out.println("Snipped Links are " + SnippedLinks);	
    }
    
    protected void writeFooterTag(String Search_Query, int CurrentPageIndex, int End, PrintWriter out){

        out.println("<footer>\r\n" + 
        		"	           <center>\r\n" + 
        		"                   <ul class=\"pagination\"> <li class=\"page-item\">\r\n" + 
        		"                          <a class=\"page-link\" href='?SearchText="+Search_Query+"&CurrentPage="+(CurrentPageIndex-1)+"' aria-label=\"Previous\">\r\n" + 
        		"                            <span aria-hidden=\"true\">&laquo;</span>\r\n" + 
        		"                            <span class=\"sr-only\">Previous</span>\r\n" + 
        		"                          </a>\r\n" + 
        		"                        </li>" );  
        int PagStartIndex = (int) (10*(Math.floor((double)CurrentPageIndex/(double)LinksPerPage)) + 1);
        int PagEndIndex = Math.min(PagStartIndex + 9, End);
        //Pagination
        for(int i=PagStartIndex; i <= PagEndIndex; i++)
        {
            if(Integer.toString(i).equals(Current_Page))
                out.println(    "<li class=\"active page-item\"><a class=\"page-link\" href=\"#\">"+i+"</a></li>" );   
            else
                out.println(    " <li class= \"page-item\"><a class=\"page-link\" href='?SearchText="+Search_Query+"&CurrentPage="+i+"'>"+i+"</a></li>" );
        }
        out.println("<li class=\"page-item\">\r\n" + 
        		"                      <a class=\"page-link\" href='?SearchText="+Search_Query+"&CurrentPage="+(CurrentPageIndex+1)+"' aria-label=\"Next\">\r\n" + 
        		"                        <span aria-hidden=\"true\">&raquo;</span>\r\n" + 
        		"                        <span class=\"sr-only\">Next</span>\r\n" + 
        		"                      </a>\r\n" + 
        		"                    </li>\r\n" + 
        		"                   </ul>\r\n" + 
        		"                </center>\r\n" + 
        		"            </footer>\r\n" + 
        		"        </div>\r\n");
    }
    
    protected int getCurrentPageIndex(String Current_Page, int End) {
    	CurrentPageIndex = Integer.parseInt(Current_Page); 
		  if(CurrentPageIndex <= 0) {
			  Current_Page = "1";
			  CurrentPageIndex = 1;
		  }
		  else if(CurrentPageIndex > End && ResultsCount != 0) {
			  Current_Page = String.valueOf(End);
			  CurrentPageIndex = End;
		  }
		  return CurrentPageIndex;
    }
    
    protected void displayResults(PrintWriter out) {  //Displaying results in the form of title, url, and a snippet describing the content of the page
		  out.println("<div class=\"Links\">\n");
		  int CurrentLinkIndex = LinkStartingIndex;
		  while(CurrentLinkIndex < (CurrentPageIndex * LinksPerPage) && CurrentLinkIndex < ResultsCount) {
			  LinkSnip CurrentSnip = SnippedLinks.get(CurrentLinkIndex);
			//  System.out.println("Current Snip Title is " + CurrentSnip.Title);
			  out.println("<div class=\"Link\">\r\n" + 
			  		" <h5 style=\"display:inline-block; text-overflow: ellipsis; white-space: nowrap; color:dodgerblue\"><a href=\'"+CurrentSnip.Url+"'>"+CurrentSnip.Title+"</a></h5>\r\n" + 
			  		" <p style=\"color:green\">"+CurrentSnip.Url+"</p>\r\n" + 
			  		" <p>"+CurrentSnip.Snippet+"</p>\r\n" + 
			  		"  </div>");
	          CurrentLinkIndex++;
		  }
      }
    
    protected void writeScriptsTag(PrintWriter out) {
    	  out.println("<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js\"></script> \r\n" + 
          		"      <script src=\"http://ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/jquery-ui.min.js\"></script>\r\n" + 
          		"    <script type=\"text/javascript\">\r\n" + 
          		"     $(document).ready(function() {\r\n" + 
          		"                $(function() {\r\n" + 
          		"                    $(\"#SearchText\").autocomplete({\r\n" + 
          		"                        source: function(request, response) {\r\n" + 
          		"                            $.ajax({\r\n" + 
          		"                                url: \"SearchSuggestions\",\r\n" + 
          		"                                type: \"GET\",\r\n" + 
          		"                                data: {\r\n" + 
          		"                                    term: request.term\r\n" + 
          		"                                },\r\n" + 
          		"                                dataType: \"json\",\r\n" + 
          		"                                success: function(data) {\r\n" + 
          		"                                    response(data);\r\n" + 
          		"                                }\r\n" + 
          		"                            });\r\n" + 
          		"                        }\r\n" + 
          		"                    });\r\n" + 
          		"                });\r\n" + 
          		"            });\r\n" + 
          		"	      </script></body>");
          out.println("</html>");
    }
    
    }
    

