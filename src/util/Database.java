/*  Database.java
 *  
 *  This class acts as a wrapper for the JDBC, implementing a simple interface for
 *  making queries to the specified database. 
 */
package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Database 
{

 static Connection conn;
 private static Hashtable<String, Hashtable<String, Long>> word_cooccurrence;

    /* Default constructor */
public Database() 
{
	
   /* Parameter 1:  The driver for the type of database being supported
      Parameter 2:  A link to the location of the databases(default port 3306)
           Note: May be able to select specific home database
      The third and fourth parameters are the user and password for the db
   */
    this("sun.jdbc.odbc.JdbcOdbcDriver", "jdbc:odbc:listen", "liuliu", "liuliu");
}

public Database(String url)
{
	this("sun.jdbc.odbc.JdbcOdbcDriver", "jdbc:odbc:"+url, null, null);
}

public Database(String dbDriver, String url, String login, String password) 
{    
    try 
    {
    	//Register the JDBC driver for MySQL.
        Class.forName(dbDriver);
        
        
        //Get a connection to the database
        if(login == null && password == null)
        	conn = DriverManager.getConnection(url);
        else
        	conn = DriverManager.getConnection(url,login, password);
        
    } catch (java.lang.ClassNotFoundException e) {
        System.err.println("Got an exception in connecting to the database: ");
        System.out.println(e);
        System.err.println(e.getMessage());
    } catch (java.sql.SQLException e) {
        System.err.println("Got an exception in connecting to the database: ");
        System.out.println(e);
        System.err.println(e.getMessage());
    }
}

/* This method executes the passed in SQL query ('select' statement) */
public ResultSet execute(String query) 
{
    ResultSet rs = null;
    try {
        Statement stmt;
        stmt = conn.createStatement();
        rs = stmt.executeQuery(query);
    } catch (SQLException e) {
        System.out.println("Exception " + e + " while executing query  " + query);
    }
    return rs;
}
/*
 * Executes other statements (insert, update, delete, etc). 
 */
public int executeUpdate(String query) 
{
    int rowCount = 0;
    try {
        Statement stmt = conn.createStatement();
        rowCount = stmt.executeUpdate(query);
        stmt.close();
    } catch (SQLException e) {
        System.out.println("Exception " + e + " while executing update query  " + query);
    }

    return rowCount;
}	

public ResultSet execute_restrictSize(String query) 
{
    ResultSet rs = null;
    try {
        Statement stmt;
        stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                java.sql.ResultSet.CONCUR_READ_ONLY);
        System.out.println("fetchsize_______________________________");
        stmt.setFetchSize(100);
        System.out.println("fetchsize!!!!!!!!!!!!");
        rs = stmt.executeQuery(query);
    } catch (SQLException e) {
        System.out.println("Exception " + e + " while executing query  " + query);
    }
    return rs;
}
/*
 * Executes other statements (insert, update, delete, etc). 
 */
public int executeUpdate_restrictSize(String query) 
{
    int rowCount = 0;
    try {
        Statement stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                java.sql.ResultSet.CONCUR_READ_ONLY);
        stmt.setFetchSize(1000);
        rowCount = stmt.executeUpdate(query);
        stmt.close();
    } catch (SQLException e) {
        System.out.println("Exception " + e + " while executing update query  " + query);
    }

    return rowCount;
}	

/**
 * Insert the method's description here.
 * Creation date: (11/5/2003 12:08:45 AM)
 */
public void close() {
    try {
        conn.close();
    } catch (SQLException sqe) {
        System.err.println("Database.close() sql exception " + sqe);
    }
}

public static void readTier2(String filename){
	word_cooccurrence = new Hashtable<String, Hashtable<String, Long>>();
	try{
		File tier2 = new File(filename);
		FileReader fr = new FileReader(tier2);
		BufferedReader br = new BufferedReader(fr);
		String line = "";
		
		while((line=br.readLine())!=null){
			word_cooccurrence.put(line, new Hashtable<String, Long>());
		}
	}catch(IOException e){
		e.printStackTrace();
	}
}

public static void main(String[] args) throws SQLException {
    Database db = new Database();  // Check out the default constructor for Database!
    readTier2("Tier2.txt");
    Set<String> entryset = word_cooccurrence.keySet();
	Iterator<String> it = entryset.iterator();
    
    int i = 0;
    try {
    	while(it.hasNext()){
			String word = it.next();
			//System.out.println(word);
			String query = "select word from liuliu.verbosity where data = '"+word+"' limit 1";
			ResultSet rs = db.execute(query);  // Pass in a simple query
			if(rs.next()){
				i++;
			}
    	}
    	System.out.println(i);
    } catch (SQLException e) {
        System.out.println("Error in main method.  " + e);
    }
}
}

