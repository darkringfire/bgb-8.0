package ru.badcom.bgbilling.reports;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.badcom.util.NamedParamStatement;
import ru.bitel.bgbilling.modules.reports.server.bean.filter.BGReportFilter;
import ru.bitel.bgbilling.modules.reports.server.report.BGCSVReport;

import java.sql.Date;
import java.sql.*;
import java.util.*;

/**
 * TODO ContractPaymentsByCity description
 *
 * @author a.kosorotikov
 * @version 1.0-2022.06.22
 */
public class ContractPaymentsByCity implements BGCSVReport.CSVFillerDataFields {
    protected static final Logger logger = LogManager.getLogger(ContractPaymentsByCity.class);
    @Override
    public void fillReport(Connection con, BGReportFilter filter, BGCSVReport.ReportResult result, Map<String, String> fields) throws Exception {
        int cityId = filter.getIntParam("city");
        Calendar calFrom = Calendar.getInstance();
        Calendar calTo = Calendar.getInstance();
        calFrom.setTime(filter.getDateParam("month"));
        calTo.setTime(filter.getDateParam("month"));
        calTo.add(Calendar.MONTH, 1);
        calTo.add(Calendar.DAY_OF_MONTH, -1);
        Date dateFrom = new Date(calFrom.getTime().getTime());
        Date dateTo = new Date(calTo.getTime().getTime());
        logger.debug("cityId=" + cityId);
        logger.debug("from=" + dateFrom);
        logger.debug("to=" + dateTo);

        fields.put("month", "Месяц отчёта##70");
        fields.put("city", "Населённый пункт##200");
        fields.put("cid", "#cid");
        fields.put("title", "Договор##100");
        fields.put("comment", "Клиент##300");
        fields.put("login", "Логин##75");
        fields.put("password", "Пароль##75");
        fields.put("tariff", "Тариф##200");
        fields.put("income", "Пополнение##100");
        fields.put("outlay", "Расход##100");
        fields.put("balance", "Баланс на конец месяца##150");

        NamedParamStatement statement = new NamedParamStatement();
        statement.queryTemplate = "select date_format(:from, '%x-%m') as month, a_c.title as city, c.id as cid, c.title, c.comment \n" +
                "     , i_serv.login, i_serv.password, c_t.title as tariff \n" +
                "     , ifnull(c_b.summa2, 0) as income \n" +
                "     , ifnull(c_b.summa3, 0) as outlay \n" +
                "     , ifnull(c_b.summa1 + c_b.summa2 - c_b.summa3, 0) as balance \n" +
                "from contract as c \n" +
                " left join ( \n" +
                "    select contractId, group_concat(login) as login, group_concat(password) as password \n" +
                "    from inet_serv_1 \n" +
                "    group by contractId \n" +
                "    ) as i_serv on i_serv.contractId = c.id \n" +
                " left join contract_parameter_type_2 as c_addr on c_addr.cid = c.id and c_addr.pid = 3 \n" +
                " left join address_house as a_h on a_h.id = c_addr.hid \n" +
                " left join address_street as a_s on a_s.id = a_h.streetid \n" +
                " left join address_city as a_c on a_c.id = a_s.cityid \n" +
                " left join ( \n" +
                "    select cid, group_concat(DISTINCT t_p.title) as title \n" +
                "    from contract_tariff as j_c_t \n" +
                "    left join tariff_plan as t_p on t_p.id = j_c_t.tpid \n" +
                "    where " +
                "        date1 <= :to " +
                "        and (date2 is null or date2 >= :from) \n" +
                "    group by cid \n" +
                "    ) as c_t on c_t.cid = c.id \n" +
                " left join contract_balance as c_b \n" +
                "    on c_b.cid = c.id and date(concat_ws('-', c_b.yy, c_b.mm, 1)) = :from \n" +
                "where true \n" +
                "    and a_c.id = :cid \n" +
                "    and (c.date2 is null or c.date2 <= :to) \n" +
                "order by c.comment";
        statement.params.put("from", dateFrom);
        statement.params.put("to", dateTo);
        statement.params.put("cid", cityId);

        ResultSet rs = statement.executeQueryTemplate(con);

        List<Map<String, String>> data = new ArrayList<>();
        while (rs.next()) {
            HashMap<String, String> map = new HashMap<>();
            fields.keySet().forEach(k -> {
                try {
                    map.put(k, rs.getString(k));
                } catch (SQLException e) {
                    logger.error(e.getMessage());
                }
            });
            data.add(map);
        }
        result.setData(data);

    }
}
