import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.json.*;

/**
 * Servlet implementation class SearchSuggestions
 */
@WebServlet("/SearchSuggestions")
public class SearchSuggestions extends HttpServlet {
	private static final long serialVersionUID = 1L;
	 static DB Database;
	 static ArrayList<String> PrevQueries = new ArrayList<String>();
       
    /**
     * @throws SQLException 
     * @throws ClassNotFoundException 
     * @see HttpServlet#HttpServlet()
     */
    public SearchSuggestions() throws ClassNotFoundException, SQLException {
    	System.out.println("Creating Database in search suggestions");
    	
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		String term = request.getParameter("term");
		ArrayList<String> Suggestions = new ArrayList<String>();
	    System.out.println("Data from ajax call " + term);
	    if(PrevQueries.isEmpty()) {
            String sql="select SearchQuery from SearchQueries WHERE SearchQuery LIKE '"+term+"%' order by SearchedCount DESC;";
            
            try {
				System.out.println("Creating new DB in search suggestions");
              	try {
        			Database = new DB();
        		} catch (ClassNotFoundException | SQLException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}
            ResultSet rs = Database.runSql(sql);
            PrintWriter out = response.getWriter();
            int i = 0;
            if(rs == null) return;
				while(rs.next() && i<5){
					Suggestions.add(rs.getString("SearchQuery"));
					i++;
				}
			//	rs.close();
				
			}catch (SQLException | NullPointerException e) {
			
			} 
        }
	    else {

			System.out.println("We have previous suggestions");
	    	for(String Query : PrevQueries) {
	    		if(Query.startsWith(term)) {
	    			Suggestions.add(Query);
	    		}
	    	}
	    }

        System.out.println(Suggestions);
        JSONArray json=new JSONArray(Suggestions);
		response.setContentType("application/json");
		response.getWriter().print(json);

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
