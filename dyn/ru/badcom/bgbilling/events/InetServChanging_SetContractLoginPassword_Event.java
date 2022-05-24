package ru.badcom.bgbilling.events;

import bitel.billing.server.contract.bean.CommentPatternManager;
import bitel.billing.server.contract.bean.Contract;
import bitel.billing.server.contract.bean.ContractManager;
import bitel.billing.server.contract.bean.ContractParameterManager;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.bitel.bgbilling.common.BGException;
import ru.bitel.bgbilling.kernel.script.server.dev.EventScriptBase;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetServ;
import ru.bitel.bgbilling.modules.inet.api.server.event.InetServChangingEvent;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.sql.ConnectionSet;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * При изменении сервиса Inet меняет пароль договора и параметр договора "Логин" в соответствии с сервисом
 */
public class InetServChanging_SetContractLoginPassword_Event<E extends InetServChangingEvent> extends EventScriptBase<E>{
    protected static final Logger logger = LogManager.getLogger(InetServChanging_SetContractLoginPassword_Event.class);
    private static final int PARAM_LOGIN_ID = 12;

    @Override
    public void onEvent(E event, Setup setup, ConnectionSet connectionSet) throws BGException, SQLException {
        Connection connection = connectionSet.getConnection();
        ContractManager contractManager = new ContractManager(connection);
        // TODO fix deprecated
        ContractParameterManager contractParameterManager = new ContractParameterManager(connection);
        CommentPatternManager commentPatternManager = new CommentPatternManager(connection);
        int contractId = event.getContractId();
        Contract contract = contractManager.getContractById(contractId);
        InetServ inetServ = event.getNewInetServ();

        contract.setPswd(inetServ.getPassword());
        contractManager.updateContract(contract);

        contractParameterManager.updateStringParam(contractId, PARAM_LOGIN_ID, inetServ.getLogin(), 0);
        commentPatternManager.updateContractComment(contractId);
    }
}