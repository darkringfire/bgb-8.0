package ru.badcom.bgbilling.reports;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.bitel.bgbilling.modules.reports.server.bean.filter.BGReportFilter;
import ru.bitel.bgbilling.modules.reports.server.report.BGCSVReport;
import ru.bitel.bgbilling.server.util.Setup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * Get contracts by city
 *
 * @author a.kosorotikov
 * @version 1.0-2022.06.08
 */
public class ContractsActivation implements BGCSVReport.CSVFillerDataFields {
    protected static final Logger logger = LogManager.getLogger(ContractsActivation.class);


    private Map<String, String> loadStatuses() {
        Map<String, String> map = new HashMap<>();

        Setup setup = Setup.getSetup();
        String statuses = setup.get("contract.status.list");
        StringTokenizer st = new StringTokenizer(statuses, ";");
        while(st.hasMoreTokens()) {
            String statusRow = st.nextToken();
            int pos = statusRow.indexOf(':');
            if (pos > -1) {
                map.put(
                        statusRow.substring(0, pos),
                        statusRow.substring(pos + 1)
                );
            }
        }
        return map;
    }

    @Override
    public void fillReport(Connection con, BGReportFilter filter, BGCSVReport.ReportResult result, Map<String, String> fields) throws Exception {
        Map<String, String> statusesMap = loadStatuses();

        String query = "SELECT " +
                "c.id as cid, t.date1 act_date, c.title as title, c.comment as comment, " +
                "c.status as status, c.status_date as status_date " +
                "FROM contract_tariff t " +
                "LEFT JOIN ( SELECT cid,MIN(date1) d1 FROM contract_tariff " +
                "    WHERE tpid != 25 GROUP BY cid  ) f ON t.cid=f.cid " +
                "LEFT JOIN contract c ON t.cid=c.id " +
                "WHERE t.date1=f.d1 ORDER BY t.date1";

        PreparedStatement ps = con.prepareStatement(query);
        List<Map<String, String>> data = new ArrayList<>();

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            String cid = rs.getString("cid");
            String act_date = rs.getString("act_date");
            String title = rs.getString("title");
            String comment = rs.getString("comment");
            String statusStr = statusesMap.get(rs.getString("status"));
            String statusDate = rs.getString("status_date");

            HashMap<String, String> map = new HashMap<>();
            map.put("cid", cid);
            map.put("act_date", act_date);
            map.put("title", title);
            map.put("comment", comment);
            map.put("status", statusStr);
            map.put("status_date", statusDate);
            data.add(map);
        }
        fields.put("cid", "cid##50");
        fields.put("act_date", "Дата активации");
        fields.put("title", "Договор");
        fields.put("comment", "Комментарий");
        fields.put("status", "Статус");
        fields.put("status_date", "Дата установки статуса");

        result.setData(data);

    }
}
