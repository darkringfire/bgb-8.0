package ru.badcom.bgbilling.events;

import bitel.billing.server.contract.bean.CommentPatternManager;
import bitel.billing.server.contract.bean.Contract;
import bitel.billing.server.contract.bean.ContractManager;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.bitel.bgbilling.common.BGException;
import ru.bitel.bgbilling.kernel.contract.api.server.bean.ContractDao;
import ru.bitel.bgbilling.kernel.script.server.dev.EventScriptBase;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetServ;
import ru.bitel.bgbilling.modules.inet.api.server.event.InetServChangingEvent;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.sql.ConnectionSet;
import ru.bitel.oss.kernel.entity.common.bean.EntityAttrText;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * При изменении сервиса Inet меняет пароль договора и параметр договора "Логин" в соответствии с сервисом
 */
public class InetServChanging_SetContractLoginPassword_Event<E extends InetServChangingEvent> extends EventScriptBase<E>{
    protected static final Logger logger = LogManager.getLogger(InetServChanging_SetContractLoginPassword_Event.class);
    private static final int PARAM_ID_LOGIN = 12;

    @Override
    public void onEvent(E event, Setup setup, ConnectionSet connectionSet) throws BGException, SQLException {
        int cid = event.getContractId();
        int uid = event.getUserId();

        Connection con = connectionSet.getConnection();
        InetServ inetServ = event.getNewInetServ();
        ContractManager contractManager = new ContractManager(con);
        CommentPatternManager commentPatternManager = new CommentPatternManager(con);
        ContractDao contractDao = new ContractDao(con, uid);

        // Set login parameter by inet login
        EntityAttrText loginParameter = (EntityAttrText) contractDao.getContractParameter(cid, PARAM_ID_LOGIN);
        loginParameter.setValue(inetServ.getLogin());
        contractDao.updateContractParameter(cid, loginParameter);

        // Set contract password
        Contract contract = contractManager.getContractById(cid);
        contract.setPswd(inetServ.getPassword());
        contractManager.updateContract(contract);

        // Update contract comment
        commentPatternManager.updateContractComment(cid);
    }
}