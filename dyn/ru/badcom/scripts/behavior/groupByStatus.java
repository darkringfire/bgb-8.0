package ru.badcom.scripts.behavior;

import bitel.billing.server.contract.bean.Contract;
import bitel.billing.server.contract.bean.ContractManager;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.bitel.bgbilling.kernel.container.managed.ServerContext;
import ru.bitel.bgbilling.kernel.contract.api.common.event.ContractModifiedEvent;
import ru.bitel.bgbilling.kernel.event.events.ContractStatusChangedEvent;
import ru.bitel.bgbilling.kernel.script.server.dev.EventScriptBase;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.sql.ConnectionSet;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

public class groupByStatus extends EventScriptBase<ContractStatusChangedEvent> {
    private static final int GROUP_SUSPENDED = 57;
    private static final List<Integer> STATUSES_SUSPEND = Arrays.asList(4, 6);

    protected static final Logger logger = LogManager.getLogger(ContractStatusChangedEvent.class);

    @Override
    public void onEvent(ContractStatusChangedEvent event, Setup setup, ConnectionSet connectionSet)
            throws Exception {
        Connection con = connectionSet.getConnection();
        ContractManager contractManager = new ContractManager(con);
//        var contractLabelManager = new ContractLabelManager(con);
        int cid = event.getContractId();
        Contract contract = contractManager.getContractById(cid);
        int contractStatus = contract.getStatus();
        ServerContext context = ServerContext.get();


        if (STATUSES_SUSPEND.contains(contractStatus)) {
            contractManager.addContractGroup(cid, GROUP_SUSPENDED);
        } else {
            contractManager.deleteContractGroup(cid, GROUP_SUSPENDED);
        }
//        contractLabelManager.syncLabelAndGroupContract(cid);

        context.publishAfterCommit(new ContractModifiedEvent(0, cid));
    }
}
