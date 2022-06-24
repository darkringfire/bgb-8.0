package ru.badcom.scripts.behavior;

import ru.bitel.bgbilling.kernel.contract.api.common.event.ContractModifiedEvent;
import ru.bitel.bgbilling.kernel.contract.balance.server.event.ConvergenceBalanceEvent;
import ru.bitel.bgbilling.kernel.container.managed.ServerContext;
import ru.bitel.bgbilling.kernel.event.Event;
import ru.bitel.bgbilling.kernel.script.server.dev.EventScriptBase;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.sql.ConnectionSet;
import java.sql.*;
import java.util.Date;
 
import bitel.billing.server.contract.bean.*;
 
public class groupByStatus extends EventScriptBase
{
	@Override
	public void onEvent( Event event, Setup setup, ConnectionSet connectionSet )
		throws Exception
	{
		Connection con = connectionSet.getConnection();	
		ContractManager cpm = new ContractManager( con );
 
		int cid = event.getContractId();
		ContractStatusManager contract_status_manager = new ContractStatusManager(con);
		ContractStatus status = contract_status_manager.getStatus(cid, new Date());
		if (status == null)
		{
			return;
		}
		int contract_status = status.getStatus();
 
		if (contract_status == 4) 
		{
			cpm.addContractGroup( cid, 57 );
		}
 
		if (contract_status == 6) 
		{
			cpm.addContractGroup( cid, 57 );
		}
 
		if (contract_status == 0) 
		{
			cpm.deleteContractGroup( cid, 57 );
		};
		ServerContext context = ServerContext.get();
		context.publishAfterCommit( new ContractModifiedEvent( 0, cid ) );
	}
}