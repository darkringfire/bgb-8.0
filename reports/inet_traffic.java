import java.sql.*;
import java.util.*;
import java.util.regex.*;
import java.sql.*;
import java.text.NumberFormat;
import ru.bitel.bgbilling.server.util.ServerUtils;
import bitel.billing.common.TimeUtils;

public void fillReport( con, filter, bitel.billing.server.reports.BGCSVReport.ReportResult result )
{	
	Calendar date = filter.getCalendarParam( "month" );
	NumberFormat nf = NumberFormat.getInstance();
	int mid = filter.getIntParam( "mid" );
	int koeff = 1; //коэффицент на который будем делить трафик. (мб кб гб ну и тд)
	String size = filter.getStringParam("size");
	switch (size){
		case "0":
			koeff=1;
			break;
		case "1":
			koeff=1024;
			break;
		case "2":
			koeff=1048576;
			break;
		case "3":
			koeff=1073741824;
			break;
		default:
			koeff=1;	
	}
	long time = System.currentTimeMillis();
	
	String sessionLogDetail = ServerUtils.getModuleMonthTableName( "inet_session_log_detail", TimeUtils.convertCalendarToSqlDate(date), mid );
	String trafficType = ServerUtils.getModuleTableName( "inet_traffic_type", mid );

	StringBuilder sb = new StringBuilder (100);
	
	sb.append( "SELECT a.sessionId, a.day, a.hour, a.trafficTypeId, SUM(a.amount), type.title FROM " );
	sb.append( sessionLogDetail );
	sb.append( " AS a RIGHT OUTER JOIN " );
	sb.append( trafficType );
	sb.append( " AS type ON a.trafficTypeId=type.id WHERE (day>0) OR a.sessionId IS NULL GROUP BY type.id " );
	
	ps = con.prepareStatement( sb.toString() );

	data = new ArrayList( 1000 );


	rs = ps.executeQuery();
	while ( rs.next() )
	{
		trafficType = rs.getString( "title" );
		amount = rs.getDouble( "SUM(a.amount)" )/koeff;
		Map  map = new HashMap();
		map.put( "traffic_type", trafficType );
		map.put( "amount", (nf.format(amount)).toString() );
		
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

