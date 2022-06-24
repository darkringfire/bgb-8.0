package ru.badcom.scripts.global.utils;

import java.lang.String;
import ru.bitel.bgbilling.modules.inet.api.server.bean.InetAccountGenerate;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetServ;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetServType;
import ru.bitel.common.ParameterMap;
import ru.bitel.common.Preferences;
import ru.bitel.bgbilling.server.util.Setup;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import ru.bitel.bgbilling.server.util.ServerUtils;

import java.sql.PreparedStatement;

public class BadcomInetAccountGenerate implements InetAccountGenerate
{
	public String generateLogin(InetServ inetServ, InetServType servType, ParameterMap moduleSetup)
	{
		String mID;
		Integer loginMin;
		Integer loginMax;
		Integer loginNext = 1000001;
		String query;
		PreparedStatement ps;
		ResultSet rs;
		
//		System.out.println("*** inetServ ***");
//		System.out.println(inetServ.toString());

		Preferences servTypePrefs = new Preferences(servType.getConfig(), "\r\n");
//		System.out.println("*** inetType ***");
//		System.out.println(servTypePrefs.toString());
//		System.out.println(servTypePrefs.getInt("serv.login.min", 1000001));
//		System.out.println(servTypePrefs.getInt("serv.login.max", 1999999));
//		System.out.println(servType.getConfig() );

//		System.out.println("*** moduleSetup ***");

		mID = moduleSetup.get("moduleId");
		loginMin = servTypePrefs.getInt("serv.login.min", 1000000);
		loginMax = servTypePrefs.getInt("serv.login.max", 1999999);

		System.out.println("moduleId=" + mID);
		System.out.println("loginMin=" + loginMin);
		System.out.println("loginMax=" + loginMax);

		query = "select ls.next " +
			"from ( (select ? as next) union (select (login+1) next from inet_serv_"+mID+" where login >= ? and login <= ? order by next) ) ls " +
			"left join inet_serv_"+mID+" t on (t.login = ls.next) where t.login is null limit 1";
		System.out.println("query=" + query);
		
		Connection con = Setup.getSetup().getDBConnectionFromPool();
		try {
			ps = con.prepareStatement(query);
			
			ps.setInt(1, loginMin);
			ps.setInt(2, loginMin);
			ps.setInt(3, loginMax);
			
			rs = ps.executeQuery();
			rs.next();
			loginNext = rs.getInt("next");
		}
		catch (SQLException e) {
			System.out.println("ERROR: "+e.toString());
		}
		finally {
			ServerUtils.closeConnection(con);
		}
		
		System.out.println("loginNext=" + loginNext);
		return loginNext.toString();
	}
	
	public String generatePassword(InetServ inetServ, InetServType servType, ParameterMap moduleSetup)
	{
		//System.out.println(moduleSetup.toString());
		System.out.println(moduleSetup.get("serv.login.max"));
		System.out.println(moduleSetup.get("serv.login.min"));
		return "22222222";
	}
}
