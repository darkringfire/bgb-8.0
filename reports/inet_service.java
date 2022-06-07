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
	long time = System.currentTimeMillis();
	
	String sessionLogAccount = ServerUtils.getModuleMonthTableName( "inet_session_log_account", TimeUtils.convertCalendarToSqlDate(date), mid );
	
	StringBuilder sb = new StringBuilder (100);
	
	sb.append( "SELECT a.sessionId, SUM(a.amount), SUM(a.account), a.serviceId, type.title FROM " );
	sb.append( sessionLogAccount );
	sb.append( " AS a RIGHT JOIN service AS type ON a.serviceId=type.id WHERE type.mid=? GROUP BY type.id" );
	
	ps = con.prepareStatement( sb.toString() );
	ps.setInt(1, mid);
	switch (size)
	{
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
	data = new ArrayList( 1000 );
	rs = ps.executeQuery();
	while ( rs.next() )
	{
		servType = rs.getString( "title" );
		amount = rs.getDouble( "SUM(a.amount)" )/koeff;
		account = rs.getDouble( "SUM(a.account)" );
		Map  map = new HashMap();
		map.put( "service", servType );
		map.put( "amount", (nf.format(amount)).toString() );
		map.put( "account", (nf.format(account)).toString() );
		
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

