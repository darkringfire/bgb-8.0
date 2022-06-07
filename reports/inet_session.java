import java.sql.*;

import java.util.*;
import java.util.regex.*;

import java.net.InetAddress;

import java.text.NumberFormat;
import ru.bitel.bgbilling.server.util.ServerUtils;
import bitel.billing.common.TimeUtils;


public void fillReport( con, filter, bitel.billing.server.reports.BGCSVReport.ReportResult result )
{	
	Calendar date = filter.getCalendarParam( "month" );
	NumberFormat nf = NumberFormat.getInstance();
	int mid = filter.getIntParam( "mid" );
	String session_id = filter.getStringParam ("session_id");
	int fl=0;
	if(session_id!="")
	{
		session_id="AND a.id LIKE '"+session_id+"' ";
		fl=1;
	}
	String connection_id = filter.getStringParam ("connection_id");
	if(connection_id!="")
	{
		connection_id="AND connectionId LIKE '"+connection_id+"' ";
		fl=1;
	}
	String ipAddress = filter.getStringParam( "ip_address" );
	if(ipAddress!="")
	{
		ipAddress = "AND ipAddress=? ";
		fl=1;
	}
	String contract_title = filter.getStringParam ("contract_title");
	if(contract_title!="")
	{
		contract_title="AND titles.title LIKE '"+contract_title+"' ";
		fl=1;
	}
	String where = "";
	if (fl==1){
		where = " WHERE "+(session_id+connection_id+contract_title+ipAddress).substring(3);
	}
	long time = System.currentTimeMillis();
	
	String sessionLog = ServerUtils.getModuleMonthTableName( "inet_session_log", TimeUtils.convertCalendarToSqlDate(date), mid );
	String serv = ServerUtils.getModuleTableName( "inet_serv", mid );
	
	StringBuilder sb = new StringBuilder (100);
	
	sb.append ( "SELECT a.id, a.connectionId, a.sessionStart, a.sessionStop, a.sessionTime, a.sessionCost, a.status, a.servId, titles.title FROM " );
	sb.append ( sessionLog );
	sb.append ( " AS a LEFT JOIN " );
	sb.append ( serv );
	sb.append ( " AS contract ON contract.id=a.servId LEFT JOIN contract AS titles ON contract.contractId=titles.id " );
	sb.append ( where );

	ps = con.prepareStatement( sb.toString()  );
	int index = 1;
	if(ipAddress!="")
	{
		InetAddress addr = InetAddress.getByName(filter.getStringParam( "ip_address" ));
		byte[] ipBytes = addr.getAddress();
		ps.setBytes(index++, ipBytes );
	}

	data = new ArrayList( 1000 );


	rs = ps.executeQuery();
	while ( rs.next() )
	{
		id = rs.getString( "id" );
		title = rs.getString( "title" );
		connectionId = rs.getString( "connectionId" );
		sessionStart = rs.getString( "sessionStart" ); 			
		sessionStop = rs.getString( "sessionStop" );
		sessionTime = rs.getDouble( "sessionTime" );
		sessionCost = rs.getDouble( "sessionCost" );
		Map  map = new HashMap();
		map.put( "title", title );
		map.put( "session_id", id );
		map.put( "connection_id", connectionId );
		map.put( "start_time", sessionStop );
		map.put( "stop_time", sessionStop );
		map.put( "duration", (nf.format(sessionTime)).toString()  );
		map.put( "cost", (nf.format(sessionCost)).toString()  );
		
		data.add( map );
	}
	ps.close();
	
	ps = con.prepareStatement( "SELECT FOUND_ROWS()" );
	rs = ps.executeQuery();
	
	int count = 0;
	if( rs.next() )
	{
		count = rs.getInt( 1 );
		result.setCount( count );
	}
	ps.close();
	
	result.setData( data );
}
