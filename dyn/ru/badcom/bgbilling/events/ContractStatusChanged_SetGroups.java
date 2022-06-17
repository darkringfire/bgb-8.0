package ru.badcom.bgbilling.events;

import bitel.billing.server.contract.bean.Contract;
import bitel.billing.server.contract.bean.ContractManager;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.bitel.bgbilling.common.BGException;
import ru.bitel.bgbilling.kernel.container.managed.ServerContext;
import ru.bitel.bgbilling.kernel.contract.api.common.event.ContractModifiedEvent;
import ru.bitel.bgbilling.kernel.contract.label.server.bean.ContractLabelManager;
import ru.bitel.bgbilling.kernel.event.events.ContractStatusChangedEvent;
import ru.bitel.bgbilling.kernel.script.server.dev.EventScriptBase;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.sql.ConnectionSet;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class ContractStatusChanged_SetGroups<E extends ContractStatusChangedEvent> extends EventScriptBase<E> {
    private static final int GROUP_SUSPENDED = 57;
    private static final List<Integer> STATUSES_SUSPEND = Arrays.asList(4, 6);

    protected static final Logger logger = LogManager.getLogger(ContractStatusChanged_SetGroups.class);

    @Override
    public void onEvent(E event, Setup setup, ConnectionSet connectionSet)
            throws BGException, IllegalAccessException {
        logger.debug("Contract status changed");
        Connection con = connectionSet.getConnection();
        ContractManager contractManager = new ContractManager(con);
        ContractLabelManager contractLabelManager = new ContractLabelManager(con);
        int cid = event.getContractId();
        Contract contract = contractManager.getContractById(cid);
        int contractStatus = contract.getStatus();
        ServerContext context = ServerContext.get();


        try {
            if (STATUSES_SUSPEND.contains(contractStatus)) {
                contractManager.addContractGroup(cid, GROUP_SUSPENDED);
            } else {
                contractManager.deleteContractGroup(cid, GROUP_SUSPENDED);
            }
        } catch (SQLException ignored) {
        }
        try {
            ContractLabelManager.class.getMethod("syncLabelAndGroupContract", int.class)
                    .invoke(contractLabelManager, cid);
            logger.warn("Method syncLabelAndGroupContract exists. No need try/catch");
        }
        catch (NoSuchMethodException ignore) {
        }
        catch (InvocationTargetException e) {
            throw new BGException(e.getTargetException());
        }

        context.publishAfterCommit(new ContractModifiedEvent(0, cid));
    }
}
