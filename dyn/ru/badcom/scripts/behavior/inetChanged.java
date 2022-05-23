package ru.badcom.scripts.behavior;

import bitel.billing.server.contract.bean.CommentPatternManager;
import bitel.billing.server.contract.bean.Contract;
import bitel.billing.server.contract.bean.ContractManager;
import bitel.billing.server.contract.bean.ContractParameterManager;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.bitel.bgbilling.kernel.script.server.dev.EventScript;
import ru.bitel.bgbilling.kernel.script.server.dev.EventScriptBase;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetServ;
import ru.bitel.bgbilling.modules.inet.api.server.event.InetServChangingEvent;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.sql.ConnectionSet;

import java.sql.Connection;

public class inetChanged extends EventScriptBase<InetServChangingEvent> implements EventScript<InetServChangingEvent>
{
	protected static final Logger logger = LogManager.getLogger(InetServChangingEvent.class);
	private static final int loginParameterId = 12;

	@Override
	public void onEvent( InetServChangingEvent event, Setup setup, ConnectionSet connectionSet )
      throws Exception
	{
		logger.info("Inet service changed");
		Connection connection = connectionSet.getConnection();
		ContractManager contractManager = new ContractManager(connection);
		ContractParameterManager contractParameterManager = new ContractParameterManager(connection);
		CommentPatternManager commentPatternManager = new CommentPatternManager(connection);
		int contractId = event.getContractId();
		Contract contract = contractManager.getContractById(contractId);
		InetServ inetServ = event.getNewInetServ();
		
		contract.setPswd(inetServ.getPassword());

		contractManager.updateContract(contract);
		
		contractParameterManager.updateStringParam(contractId, loginParameterId, inetServ.getLogin(), 0);
		commentPatternManager.updateContractComment(contractId);
    }
}