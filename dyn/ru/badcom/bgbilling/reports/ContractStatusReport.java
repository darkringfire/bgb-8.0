package ru.badcom.bgbilling.reports;

import ru.bitel.bgbilling.modules.reports.server.bean.filter.BGReportFilter;
import ru.bitel.bgbilling.modules.reports.server.report.BGCSVReport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Get contract list by status on given date
 *
 * @author a.kosorotikov
 * @version 1.0-2022.06.07
 */
public class ContractStatusReport implements BGCSVReport.CSVFillerData {
    @Override
    public void fillReport(Connection con, BGReportFilter filter, BGCSVReport.ReportResult result) throws Exception {
        //java.text.DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        int statusId = filter.getIntParam("status");
        int active = filter.getIntParam("active");
        Date dt = filter.getDateParam("dt");
        java.text.DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String date = df.format(dt);

        String query = "select c.title t, c.comment c, cs.status s, cs.date1 d1, cs.date2 d2 " +
                "from contract_status as cs " +
                "left join contract as c on c.id = cs.cid " +
                "where cs.status = ? " +
                "and (c.date2 is null or c.date2 >= ?) ";

        if (active == 0) {
            query +=
                    "and cs.date1 <= ? " +
                            "and cs.date2 is null ";
        } else {
            query +=
                    "and cs.date1 <= ? " +
                            "and cs.date2 >= ? ";
        }
        query += "order by cs.date1";

        PreparedStatement ps = con.prepareStatement(query);

        int num = 1;
        ps.setInt(num++, statusId);
        ps.setString(num++, date);
        ps.setString(num++, date);
        if (active == 1) {
            ps.setString(num, date);
        }

        ResultSet rs = ps.executeQuery();

        List<Map<String, String>> data = new ArrayList<>();


        while (rs.next()) {
            String title = rs.getString("t");
            String comment = rs.getString("c");
            String statusStr = "Неизвестный";
            switch (rs.getString("s")) {
                case "0":
                    statusStr = "Активен";
                    break;
                case "4":
                    statusStr = "Приостановлен клиентом";
                    break;
                case "6":
                    statusStr = "Приостановлен по балансу";
                    break;
            }
            String date1 = rs.getString("d1");
            String date2 = rs.getString("d2");

            HashMap<String, String> map = new HashMap<>();
            map.put("title", title);
            map.put("comment", comment);
            map.put("status", statusStr);
            map.put("date", date);
            map.put("date1", date1);
            map.put("date2", date2);
            data.add(map);
        }

        result.setData(data);
    }
}
