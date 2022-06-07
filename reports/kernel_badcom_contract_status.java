import java.sql.*;
import java.util.*;
import java.text.*;

public void fillReport( con, filter, result )
{
    //java.text.DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    int status = filter.getIntParam("status");
    int active = filter.getIntParam("active");
    Date dt = filter.getDateParam("dt");
    java.text.DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    String date = df.format(dt);
    
    String query = "select c.title t, c.comment c, cs.status s, cs.date1 d1, cs.date2 d2 " + 
            "from contract_status as cs " +
            "left join contract as c on c.id = cs.cid " +
            "where cs.status = ? " +
            "and (c.date1 is null or c.date1 <= ?) " +
            "and (c.date2 is null or c.date2 >= ?) ";
    
    if (active == 0) {
        query +=
                "and (cs.date1 is null or cs.date1 <= ?) " +
                "and (cs.date2 is null or cs.date2 >= ?) ";
    } else  if (active == 1) {
        query +=
                "and (cs.date1 is null or cs.date1 <= ?) " + 
                "and (cs.date2 is null or cs.date2 >= curdate()) ";
    } else  if (active == 2) {
        query +=
                "and (cs.date1 is null or cs.date1 <= ?) " + 
                "and cs.date2 >= ? " + 
                "and cs.date2 <= curdate() ";
    }
    query += "order by cs.date1";
    
    PreparedStatement ps = con.prepareStatement(query);
    
    int num = 1;
    ps.setInt(num++, status);
    ps.setString(num++, date);
    ps.setString(num++, date);
    if (active == 0) { 
      ps.setString(num++, date); 
      ps.setString(num++, date); 
    } else  if (active == 1) {
      ps.setString(num++, date); 
    } else  if (active == 2) {
      ps.setString(num++, date); 
      ps.setString(num++, date); 
    }
    
    ResultSet rs = ps.executeQuery();

    data = new ArrayList();


    while( rs.next() )
    {
        title = rs.getString("t");
        comment = rs.getString("c");
        switch (rs.getString("s")) {
            case "0":
                stsus_str = "Активен";
                break;
            case "4":
                stsus_str = "Приостановлен клиентом";
                break;
            case "6":
                stsus_str = "Приостановлен по балансу";
                break;
            default:
                stsus_str = "Неизвестный";
        }
        date1 = rs.getString("d1");
        date2 = rs.getString("d2");

        map = new HashMap();
        map.put( "title", title );
        map.put( "comment", comment );
        map.put( "status", stsus_str );
        map.put( "date", date );
        map.put( "date1", date1 );
        map.put( "date2", date2 );
        data.add( map );    
    }

    result.setData( data );
}
