package ru.badcom.bgbilling.reports;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.bitel.bgbilling.modules.reports.server.bean.filter.BGReportFilter;
import ru.bitel.bgbilling.modules.reports.server.report.BGCSVReport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Get contract by cities
 *
 * @author a.kosorotikov
 * @version 1.0-2022.06.08
 */
public class ContractsByCity implements BGCSVReport.CSVFillerDataFields {
    protected static final Logger logger = LogManager.getLogger(ContractsByCity.class);

    @Override
    public void fillReport(Connection con, BGReportFilter filter, BGCSVReport.ReportResult result, Map<String, String> fields) throws Exception {
        int cityId = filter.getIntParam("city");

        String query = "select c.id as cid, c.title, c.comment, c_a.address " +
                "from contract c " +
                "left join contract_parameter_type_2 c_a on c_a.cid = c.id and c_a.pid = 3 " +
                "left join address_house a_h on a_h.id = c_a.hid " +
                "left join address_street a_s on a_s.id = a_h.streetid " +
                "left join address_city a_c on a_c.id = a_s.cityid " +
                "where (c.date2 is null or c.date2 > curdate())";
        if (cityId == -1) {
            query += "and a_c.id is not null";
        } else if (cityId == -2) {
            query += "and a_c.id is null";
        } else if (cityId > 0) {
            query += "and a_c.id = ?";
        }
        PreparedStatement ps = con.prepareStatement(query);
        if (cityId > 0) {
            ps.setInt(1, cityId);
        }
        ResultSet rs = ps.executeQuery();
        List<Map<String, String>> data = new ArrayList<>();
        while (rs.next()) {
            HashMap<String, String> map = new HashMap<>();

            String cid = rs.getString("cid");
            String title = rs.getString("title");
            String comment = rs.getString("comment");
            String address = rs.getString("address");

            map.put("cid", cid);
            map.put("title", title);
            map.put("comment", comment);
            map.put("address", address);

            data.add(map);
        }
        fields.put("cid", "cid##50");
        fields.put("title", "Договор##250");
        fields.put("comment", "Комментарий##500");
        fields.put("address", "Адрес##700");
        result.setData(data);
    }
}
