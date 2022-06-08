package ru.badcom.bgbilling.reports;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.bitel.bgbilling.modules.reports.server.bean.filter.BGReportFilter;
import ru.bitel.bgbilling.modules.reports.server.report.BGCSVReport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Get contract list by status on given date
 *
 * @author a.kosorotikov
 * @version 1.0-2022.06.07
 */
public class ContractStatusReport implements BGCSVReport.CSVFillerDataFields {
    int STATUS_ACTIVE = 0;
    int STATUS_SUSPENDED = 4;
    int STATUS_SUSPENDED_BY_BALANCE = 6;

    protected static final Logger logger = LogManager.getLogger(ContractStatusReport.class);


    @Override
    public void fillReport(Connection con, BGReportFilter filter, BGCSVReport.ReportResult result, Map<String, String> fields) throws Exception {
        int statusId = filter.getIntParam("status");
        int statusIsActual = filter.getIntParam("statusIsActual");
        Date repDate = filter.getDateParam("repDate");

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = dateFormat.format(repDate);

        String query = "select c.id cid, c.title title, c.comment comment, cs.status status, cs.date1 date1, cs.date2 date2 " +
                "from contract_status as cs " +
                "left join contract as c on c.id = cs.cid " +
                "where cs.status = ? " +
                "and (c.date2 is null or c.date2 >= ?) ";

        if (statusIsActual == 0) {
            query +=
                    "and cs.date1 <= ? " +
                            "and cs.date2 is null ";
        } else {
            query +=
                    "and cs.date1 <= ? " +
                            "and cs.date2 >= ? ";
        }
        query += "order by cs.date1 ";

        PreparedStatement ps = con.prepareStatement(query);

        int paramCounter = 1;
        ps.setInt(paramCounter++, statusId);
        ps.setString(paramCounter++, dateStr);
        ps.setString(paramCounter++, dateStr);
        if (statusIsActual == 1) {
            ps.setString(paramCounter, dateStr);
        }

        ResultSet rs = ps.executeQuery();

        List<Map<String, String>> data = new ArrayList<>();


        while (rs.next()) {
            String cid = rs.getString("cid");
            String title = rs.getString("title");
            String comment = rs.getString("comment");
            int status = rs.getInt("status");
            String statusStr = "Неизвестный";
            if (STATUS_ACTIVE == status) {
                statusStr = "Активен";
            } else {
                if (STATUS_SUSPENDED == status) {
                    statusStr = "Приостановлен клиентом";
                } else {
                    if (STATUS_SUSPENDED_BY_BALANCE == status) {
                        statusStr = "Приостановлен по балансу";
                    }
                }
            }
            String date1 = rs.getString("date1");
            String date2 = rs.getString("date2");

            HashMap<String, String> map = new HashMap<>();
            map.put("cid", cid);
            map.put("title", title);
            map.put("comment", comment);
            map.put("status", statusStr);
            map.put("date", dateStr);
            map.put("date1", date1);
            map.put("date2", date2);
            data.add(map);
        }

        fields.put("cid", "cid##50");
        fields.put("title", "Договор##250");
        fields.put("comment", "Комментарий##500");
        fields.put("status", "Статус##100");
        fields.put("date", "Дата отчёта##150");
        fields.put("date1", "Установка статуса##150");
        fields.put("date2", "Окончание статуса##150");
        result.setData(data);
    }

}
