package de.Ryeera.Thready;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLConnector {

	private String address, user, password, database;
	private int port;
	private Connection conn = null;
	private Statement stmt = null;
	private ResultSet rs = null;
	private DragoLogger logger;
	
	public SQLConnector (String address, int port, String user, String password, String database, DragoLogger logger) {
		this.address = address;
		this.port = port;
		this.user = user;
		this.password = password;
		this.database = database;
		this.logger = logger;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://" + address + 
					":" + port + 
					"/" + database + 
					"?user=" + user + 
					"&password=" + password);
		} catch (SQLException e) {
			this.logger.log("ERROR", "Failed to connect to database!");
			this.logger.log("ERROR", "Exception: " + e.getMessage());
			this.logger.log("ERROR", "SQLState: " + e.getSQLState());
			this.logger.log("ERROR", "VendorError: " + e.getErrorCode());
			this.logger.logStackTrace(e);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public ResultSet executeQuery(String query) {
		int retries = 1;
		while (retries <= 5) {
			try {
			    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			    rs = stmt.executeQuery(query);
			    return rs;
			} catch (SQLException e) {
				System.err.println("SQL-Connection broken! Retry " + retries + " of 5...");
				if (retries == 5) {
					e.printStackTrace();
				} else {
					try {
						if (conn != null)
							conn.close();
						if (stmt != null)
							stmt.close();
						if (rs != null)
							rs.close();
					} catch (SQLException e1) {}
					try {
						Class.forName("com.mysql.cj.jdbc.Driver");
						conn = DriverManager.getConnection("jdbc:mysql://" + address + 
								":" + port + 
								"/" + database + 
								"?user=" + user + 
								"&password=" + password);
					} catch (SQLException e1) {
						System.err.println("SQLState: " + e1.getSQLState());
						System.err.println("VendorError: " + e1.getErrorCode());
						e1.printStackTrace();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				retries++;
			}
		}
		return null;
	}
	
	public boolean executeUpdate(String sql) {
		int retries = 1;
		while (retries <= 5) {
			try {
				stmt = conn.createStatement();
				return stmt.executeUpdate(sql) > 0;
			} catch (SQLException e) {
				if (retries == 5) {
					e.printStackTrace();
				} else {
					try {
						if (conn != null)
							conn.close();
						if (stmt != null)
							stmt.close();
						if (rs != null)
							rs.close();
					} catch (SQLException e1) {}
					try {
						Class.forName("com.mysql.cj.jdbc.Driver");
						conn = DriverManager.getConnection("jdbc:mysql://" + address + 
								":" + port + 
								"/" + database + 
								"?user=" + user + 
								"&password=" + password);
					} catch (SQLException e1) {
						System.err.println("SQLState: " + e1.getSQLState());
						System.err.println("VendorError: " + e1.getErrorCode());
						e1.printStackTrace();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				retries++;
			} finally {
				if (stmt != null) {
			        try {
			            stmt.close();
			        } catch (SQLException e) {}
			        stmt = null;
			    }
			}
		}
		return false;
	}
}
