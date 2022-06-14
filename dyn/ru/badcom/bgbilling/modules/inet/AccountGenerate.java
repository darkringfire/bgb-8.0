package ru.badcom.bgbilling.modules.inet;

import org.apache.log4j.Logger;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetServ;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetServType;
import ru.bitel.bgbilling.modules.inet.api.server.bean.InetAccountGenerate;
import ru.bitel.bgbilling.server.util.ServerUtils;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.ParameterMap;
import ru.bitel.common.Preferences;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountGenerate implements InetAccountGenerate {
    private static final Logger logger = Logger.getLogger(AccountGenerate.class);

    public String generateLogin(InetServ inetServ, InetServType servType, ParameterMap moduleSetup) {

        String mid;
        int loginMin;
        int loginMax;
        int loginNext = 1000001;
        String query;
        PreparedStatement ps;
        ResultSet rs;

        Preferences servTypePrefs = new Preferences(servType.getConfig(), "\r\n");

        mid = moduleSetup.get("moduleId");
        loginMin = servTypePrefs.getInt("serv.login.min", 1000000);
        loginMax = servTypePrefs.getInt("serv.login.max", 1999999);

        query = "select ls.next " +
                "from ( " +
                "   (select ? as next) union " +
                "   (select (login+1) next from inet_serv_" + mid + " " +
                "       where " +
                "           login between ? and ? " +
                "       order by next" +
                "   ) " +
                ") ls " +
                "left join inet_serv_" + mid + " t on (t.login = ls.next) " +
                "where t.login is null limit 1";

        Connection con = Setup.getSetup().getDBConnectionFromPool();
        try {
            ps = con.prepareStatement(query);

            ps.setInt(1, loginMin);
            ps.setInt(2, loginMin);
            ps.setInt(3, loginMax);

            rs = ps.executeQuery();
            rs.next();
            loginNext = rs.getInt("next");
        } catch (SQLException e) {
            logger.error("ERROR: " + e);
        } finally {
            ServerUtils.closeConnection(con);
        }

        return Integer.toString(loginNext);
    }

    public String generatePassword(InetServ inetServ, InetServType servType, ParameterMap moduleSetup) {
        return "22222222";
    }
}
