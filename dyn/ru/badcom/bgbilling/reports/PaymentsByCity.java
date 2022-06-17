package ru.badcom.bgbilling.reports;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.bitel.bgbilling.modules.reports.server.bean.filter.BGReportFilter;
import ru.bitel.bgbilling.modules.reports.server.report.BGCSVReport;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * Get contracts by city
 *
 * @author a.kosorotikov
 * @version 1.0-2022.06.07
 */
public class PaymentsByCity implements BGCSVReport.CSVFillerDataFields {
    protected static final Logger logger = LogManager.getLogger(PaymentsByCity.class);

    @Override
    public void fillReport(Connection con, BGReportFilter filter, BGCSVReport.ReportResult result, Map<String, String> fields) throws Exception {
        Date dateFrom = new Date(filter.getDateParam("from").getTime());
        Calendar dateToCal = Calendar.getInstance();
        dateToCal.setTime(filter.getDateParam("to"));
        dateToCal.set(Calendar.DAY_OF_MONTH, dateToCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date dateTo = new Date(dateToCal.getTime().getTime());
        String query = "select a_c.title city, ifnull(sum(b_p.b_pay), 0) pay, ifnull(sum(b_u.b_used), 0) used " +
                "from address_city a_c " +
                "left join address_street a_s on a_c.id = a_s.cityid " +
                "left join address_house a_h on a_s.id = a_h.streetid " +
                "left join contract_parameter_type_2 p_a on a_h.id = p_a.hid " +
                "left join contract c on p_a.cid = c.id " +
                "left join ( " +
                "    select cid, sum(summa) b_pay " +
                "    from contract_payment " +
                "    where dt between ? and ? " +
                "    group by cid " +
                ") b_p on b_p.cid = c.id " +
                "left join ( " +
                "    select cid, sum(summa3) b_used " +
                "    from contract_balance " +
                "    where date(concat(yy, '-', mm, '-', 1)) between ? and ? " +
                "    group by cid " +
                ") b_u on b_u.cid = c.id " +
                "where p_a.cid is not null " +
                "group by a_c.id, a_c.title " +
                "order by a_c.title ";
        PreparedStatement ps = con.prepareStatement(query);
        ps.setDate(1, dateFrom);
        ps.setDate(2, dateTo);
        ps.setDate(3, dateFrom);
        ps.setDate(4, dateTo);
        ResultSet rs = ps.executeQuery();
        List<Map<String, String>> data = new ArrayList<>();

        while (rs.next()) {
            HashMap<String, String> map = new HashMap<>();
            map.put("city", rs.getString("city"));
            map.put("pay", rs.getString("pay"));
            map.put("used", rs.getString("used"));
            data.add(map);
        }
        fields.put("city", "Населённый пункт##400");
        fields.put("pay", "Приход##200");
        fields.put("used", "Наработка##200");
        result.setData(data);
    }
}
