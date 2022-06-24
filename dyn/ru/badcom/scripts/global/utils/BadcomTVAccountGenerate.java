package ru.badcom.scripts.global.utils;

import java.lang.String;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import ru.bitel.bgbilling.modules.tv.api.server.bean.TvAccountGenerate;
import ru.bitel.bgbilling.modules.tv.api.common.bean.TvAccount;
import ru.bitel.common.ParameterMap;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.bgbilling.server.util.ServerUtils;


public class BadcomTVAccountGenerate implements TvAccountGenerate
{
    private static final Logger logger = Logger.getLogger( BadcomTVAccountGenerate.class );
    
    private String getInetAccParam(int contractId, ParameterMap config, String param)
    {
		logger.info("getInetAccParam");
    	
		PreparedStatement ps;
		ResultSet rs;
		String inetMID = config.get("inetModuleId");
		String query = "select login, password from inet_serv_"+inetMID+" where contractId = ?";

		String val = null;

		logger.info("inetMID = " + inetMID);
		logger.info("contractId = " + contractId);
		logger.info("query = " + query);
		
		Connection con = Setup.getSetup().getDBConnectionFromPool();
		try {
			ps = con.prepareStatement(query);
			
			ps.setInt(1, contractId);
			
			rs = ps.executeQuery();
			rs.next();
			val = rs.getString(param);
		}
		catch (SQLException e) {
			System.out.println("ERROR: "+e.toString());
		}
		finally {
			ServerUtils.closeConnection(con);
		}
		return val;
   	}

	public String generateLogin(int contractId, TvAccount tvAccount, ParameterMap config)
	{
		logger.info("generateLogin");

		String login = getInetAccParam(contractId, config, "login");
		logger.info("login = " + login);
	
		return login;
	}
	
	public String generatePassword(int contractId, TvAccount tvAccount, ParameterMap config)
	{
		logger.info("generatePassword");

		String password = getInetAccParam(contractId, config, "password");
		logger.info("password = " + password);

		return password;
	}
}
