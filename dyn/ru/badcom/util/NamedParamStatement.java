package ru.badcom.util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO NamedParamStatement description
 *
 * @author a.kosorotikov
 * @version 1.0-2022.06.23
 */
public class NamedParamStatement {
    protected static final Logger logger = LogManager.getLogger(NamedParamStatement.class);
    public Map<String, Object> params = new HashMap<>();
    // language=MariaDB
    public String queryTemplate = "";
    private final Pattern pattern = Pattern.compile(":([A-z]\\w*)");


    public ResultSet executeQueryTemplate(Connection con) throws SQLException {
        List<String> paramsList = new ArrayList<>();
        Matcher matcher = pattern.matcher(queryTemplate);
        while (matcher.find()) {
            paramsList.add(matcher.group(1));
        }
        String query = matcher.replaceAll("?");
        logger.info(paramsList);
        logger.info(query);
        PreparedStatement ps = con.prepareStatement(query);
        int paramNum = 1;
        for (String p : paramsList) {
            Object val = params.get(p);
            if ( val == null) {
                throw new SQLException("Parameter " + p + " not set");
            }
            if (val instanceof String) {
                ps.setString(paramNum++, (String) val);
            } else if (val instanceof Integer) {
                ps.setInt(paramNum++, (Integer) val);
            } else if (val instanceof Date) {
                ps.setDate(paramNum++, (Date) val);
            } else {
                throw new SQLException("Not supported data type " + val.getClass());
            }
        }
        return ps.executeQuery();

    }

}
