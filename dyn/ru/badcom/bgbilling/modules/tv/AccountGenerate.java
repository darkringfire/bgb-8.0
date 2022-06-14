package ru.badcom.bgbilling.modules.tv;

import java.lang.String;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import ru.bitel.bgbilling.modules.tv.api.server.bean.TvAccountGenerate;
import ru.bitel.bgbilling.modules.tv.api.common.bean.TvAccount;
import ru.bitel.common.ParameterMap;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.bgbilling.server.util.ServerUtils;


public class AccountGenerate implements TvAccountGenerate {
    private static final Logger logger = Logger.getLogger(AccountGenerate.class);

    private String getInetAccParam(int contractId, ParameterMap config, String param) {

        PreparedStatement ps;
        ResultSet rs;
        String inetMID = config.get("inetModuleId");
        String query = "select login, password from inet_serv_" + inetMID + " where contractId = ?";

        String val = null;


        Connection con = Setup.getSetup().getDBConnectionFromPool();
        try {
            ps = con.prepareStatement(query);

            ps.setInt(1, contractId);

            rs = ps.executeQuery();
            rs.next();
            val = rs.getString(param);
        } catch (SQLException e) {
            logger.error("ERROR: " + e);
        } finally {
            ServerUtils.closeConnection(con);
        }
        return val;
    }

    public String generateLogin(int contractId, TvAccount tvAccount, ParameterMap config) {
        return getInetAccParam(contractId, config, "login");
    }

    public String generatePassword(int contractId, TvAccount tvAccount, ParameterMap config) {
        return getInetAccParam(contractId, config, "password");
    }
}
