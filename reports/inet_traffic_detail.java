import java.sql.*;
import java.util.*;
import java.util.regex.*;
import java.sql.*;
import java.util.Collections;
import java.text.NumberFormat;
import ru.bitel.bgbilling.server.util.ServerUtils;
import bitel.billing.common.TimeUtils;


public void fillReport( con, filter, bitel.billing.server.reports.BGCSVReport.ReportResult result )
{	
	Calendar date = filter.getCalendarParam( "month" );
	long gr = filter.getLongParam( "gr" );
	NumberFormat nf = NumberFormat.getInstance();
	int mid = filter.getIntParam( "mid" );
	int koeff = 1;
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
	StringBuilder sb = new StringBuilder (100);
	
	String sessionLogDetail = ServerUtils.getModuleMonthTableName( "inet_session_log_detail", TimeUtils.convertCalendarToSqlDate(date), mid );
	String sessionLog = ServerUtils.getModuleMonthTableName( "inet_session_log", TimeUtils.convertCalendarToSqlDate(date), mid );
	String serv = ServerUtils.getModuleTableName( "inet_serv", mid );
	String trafficType = ServerUtils.getModuleTableName( "inet_traffic_type", mid );
	
	sb.append( "SELECT * FROM ( " );
	sb.append( "SELECT SUM(a.amount), contract.title as cttl, type.title, type.id AS tid, contract.id FROM " );
	sb.append( sessionLogDetail );
	sb.append( " AS a INNER JOIN ");
	sb.append( sessionLog );
	sb.append(" AS log ON a.sessionId=log.id INNER JOIN ");
	sb.append( serv );
	sb.append(" AS serv ON log.servId=serv.id INNER JOIN ");
	sb.append( trafficType );
	sb.append(" AS type ON a.trafficTypeId=type.id ");
	sb.append("INNER JOIN contract AS contract ON serv.contractId=contract.id ");
	if( gr > 0 )
        {
                sb.append( " AND contract.gr&" );
                sb.append( gr );
                sb.append( ">0 " );
        }

	sb.append(" WHERE 1=1 "); 
	sb.append(" AND a.day>0 ");	
	
	sb.append(" GROUP BY tid, contract.id WITH  ROLLUP) AS pres ");
	sb.append("ORDER BY pres.id DESC, pres.title DESC");
	
	ps = con.prepareStatement( sb.toString() );
	
	
	
	data = new ArrayList( 1000 );

	System.out.println("query="+ps.toString());
	rs = ps.executeQuery();
	while ( rs.next() )
	{
		trafficType = rs.getString( "title" );
		amount = rs.getDouble( "SUM(a.amount)" )/koeff;
		contractTitle = rs.getString ( "cttl" );
		contractId = rs.getString ( "id" );
		tid = rs.getString ( "tid" );
		if ((tid != null) && (contractTitle != null)){
			Map  map = new HashMap();
			map.put( "traffic_type", trafficType );
			map.put( "amount", (nf.format(amount)).toString() );
			if (contractId == null) {
				map.put( "contract", "Итог" );
			} else {
				map.put( "contract", contractTitle );
			}
			data.add( map );
		}
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

