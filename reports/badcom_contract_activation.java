public void fillReport( con, filter, bitel.billing.server.reports.BGCSVReport.ReportResult result )
{
    query = "SELECT t.date1 act_date,c.title title,c.comment comment,c.status status,c.status_date status_date ";
    query += "FROM contract_tariff t ";
    query += "LEFT JOIN ( SELECT cid,MIN(date1) d1 FROM contract_tariff ";
    query += "    WHERE tpid != 25 GROUP BY cid  ) f ON t.cid=f.cid ";
    query += "LEFT JOIN contract c ON t.cid=c.id ";
    query += "WHERE t.date1=f.d1 ORDER BY t.date1";

    ps = con.prepareStatement( query );
    data = new ArrayList( 1000 );

    rs = ps.executeQuery();

    while( rs. next() )
    {
        act_date = rs.getString("act_date");
        title = rs.getString("title");
        comment = rs.getString("comment");
        switch (rs.getString("status")) {
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
        status_date = rs.getString("status_date");

        map = new HashMap();
        map.put( "act_date", act_date );
        map.put( "title", title );
        map.put( "comment", comment );
        map.put( "status", stsus_str );
        map.put( "status_date", status_date );
        data.add( map );    
    }

    result.setData( data );
}
