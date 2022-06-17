package ru.badcom.bgbilling.reports;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.bitel.bgbilling.modules.reports.server.bean.filter.BGReportFilter;
import ru.bitel.bgbilling.modules.reports.server.report.BGCSVReport;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Get contract list by status on given date
 *
 * @author a.kosorotikov
 * @version 1.0-2022.06.07
 */
public class ContractStatus implements BGCSVReport.CSVFillerDataFields {
    int STATUS_ACTIVE = 0;
    int STATUS_SUSPENDED = 4;
    int STATUS_SUSPENDED_BY_BALANCE = 6;

    protected static final Logger logger = LogManager.getLogger(ContractStatus.class);


    @Override
    public void fillReport(Connection con, BGReportFilter filter, BGCSVReport.ReportResult result, Map<String, String> fields) throws Exception {
        int statusId = filter.getIntParam("status");
        int statusIsActual = filter.getIntParam("statusIsActual");
        Date repDate = new Date(filter.getDateParam("repDate").getTime());

//        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        String query = "select c.id cid, c.title title, c.comment comment, cs.status status, cs.date1 date1, cs.date2 date2 " +
                "from contract_status as cs " +
                "left join contract as c on c.id = cs.cid " +
                "where cs.status = ? " +
                "and (cs.date1 is null or cs.date1 <= ?) " +
                "and (cs.date2 is null or cs.date2 >= ?) " +
                "and (c.date2 is null or c.date2 >= ?) ";

        int ACT_ALL = 0;
        int ACT_NOW = 1;
        int ACT_CHANGED = 2;
        if (statusIsActual == ACT_NOW) {
            query += "and (cs.date2 is null or cs.date2 > now()) ";
        } else if (statusIsActual == ACT_CHANGED) {
            query += "and (cs.date2 is not null and cs.date2 < now()) ";
        }
        query += "order by cs.date1 ";


        logger.info(query);

        PreparedStatement ps = con.prepareStatement(query);

        int paramCounter = 1;
        ps.setInt(paramCounter++, statusId); // cs.status
        ps.setDate(paramCounter++, repDate); // cs.date1
        ps.setDate(paramCounter++, repDate); // cs.date2
        ps.setDate(paramCounter, repDate); // c.date2

        logger.info(ps);

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
            map.put("date", repDate.toString());
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
                                                                                                                