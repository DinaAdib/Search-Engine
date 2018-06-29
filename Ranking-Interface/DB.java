/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.sql.Connection;             // for connection
import java.sql.DriverManager;          // for connection
import java.sql.PreparedStatement;      // for queries execution
import java.sql.ResultSet;              //for return type
import java.sql.SQLException;           // for catching exception
import java.sql.Statement;              // for queries execution

import com.microsoft.sqlserver.jdbc.SQLServerException;

class DB {

    public static Connection connection = null;
    PreparedStatement ps = null;

    //default class for connection creation
    public DB() throws SQLException, ClassNotFoundException {
        connect();
    }
    
public DB(Connection con) {
	this.connection = con;
}
    
private void connect() {
	try {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver"); //connect to sql server
        //System.out.println("Driver loaded success");  
        String My_connection = "jdbc:sqlserver://localhost:1433;databaseName=SearchEngine;user=Dina;password=root";
        connection = DriverManager.getConnection(My_connection);  // setup connection
        //System.ot.println("Building my connection")

    } catch (SQLException | ClassNotFoundException e) {
        System.out.println("Connection failed" + e);
    }
}
//    public int InsertVisited(String url, String Doc) {
//        try {
//            String sql = "INSERT INTO Visited (Url,Doc,isIndexed,isCrawled)  VALUES( ?, ?,0,0);";
//            ps = connection.prepareStatement(sql);
//            ps.setString(1, url);
//            ps.setString(2, Doc);
//            ps.executeUpdate();
//            return 1;
//        } catch (SQLException e) {
//            System.out.println(e);
//            return 0;
//        }
//    }

    public void setConnection(Connection con) {
    	DB.connection = con;
    }
    // return result of query never return zero or null
    public ResultSet runSql(String sql) //select queries
    {   
    	try {
//    		while (connection == null || connection.isClosed()) {
//        		connect();
//            }
            PreparedStatement query = connection.prepareStatement(sql);
            ResultSet rs = query.executeQuery();
            return rs;
    	}
    	catch(SQLServerException se) {
    		connect();
    	}
    	catch(SQLException | NullPointerException e1) {
        	//System.out.println(e1.getCause());
    		e1.printStackTrace();
    	}
return null;      
    }

    public int updateSql(String sql) throws SQLException //create insert update delete queries
    {
        try {
//        	while (connection == null || connection.isClosed()) {
//        		connect();
//            }
            Statement ps = connection.createStatement();
            ps.executeUpdate(sql);
            return 1;
        } catch (SQLException e) {
            System.out.println(e);
            return 0;
        }
    }

    /*//return true or false according to creation of database
    public boolean runSql_checking(String sql) throws SQLException {
        Statement table = connection.createStatement();
        return table.execute(sql);
    }*/
    public static Connection getConnection(){
        return connection;
    }
    public void closeConnection() throws SQLException {
    	 if (connection != null || !connection.isClosed()) {
             connection.close();
         }
    }
    //connection closing 
    @Override
    protected void finalize() throws Throwable {
        try {
        	closeConnection();
        } finally {
            super.finalize();
        }
    }

    public static void main(String args[]) throws SQLException, ClassNotFoundException {
      DB db = new DB();
//        String sql = "Delete from Indexer where Url='https://hxim.github.io/Stockfish-Evaluation-Guide/';";
//        int re = db.updateSql(sql);
//        System.out.println(re);
           String begin="Begin \n";
           String end="End";
          String sql1 = "insert into t1 values('Mohamed');\n";
          String sql2 = "insert into t1 values('Zeftawey');\n";
          String sql3 = "delete from t1 where zefo='Zeftawey'; \n";
          String sql=begin+sql1+sql2+sql3+end;
          System.out.println(sql);
           int rs = db.updateSql(sql);
           System.out.println(rs);
//           while (rs.next()) {
//                   // System.out.println("Inserting " + rs.getString("Url"));
//                    System.out.println(rs.getString("Url"));
//                }
           
    }


}
