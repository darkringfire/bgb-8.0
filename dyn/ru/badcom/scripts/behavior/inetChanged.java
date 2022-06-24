package ru.badcom.scripts.behavior;

import ru.bitel.bgbilling.kernel.script.server.dev.EventScript;
import ru.bitel.bgbilling.kernel.script.server.dev.EventScriptBase;
import ru.bitel.bgbilling.modules.inet.api.server.event.InetServChangingEvent;

import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.sql.ConnectionSet;

import ru.bitel.bgbilling.modules.inet.api.common.bean.InetServ;
import java.sql.*;
import bitel.billing.server.contract.bean.ContractManager;
import bitel.billing.server.contract.bean.ContractParameterManager;
import bitel.billing.server.contract.bean.Contract;
import bitel.billing.server.contract.bean.CommentPatternManager;

public class inetChanged extends EventScriptBase<InetServChangingEvent> implements EventScript<InetServChangingEvent>
{
	@Override
	public void onEvent( InetServChangingEvent event, Setup setup, ConnectionSet connectionSet )
      throws Exception
	{
		System.out.println("Inet service changed");
		Connection connection = connectionSet.getConnection();
		ContractManager contractManager = new ContractManager(connection);
		ContractParameterManager contractParameterManager = new ContractParameterManager(connection);
		CommentPatternManager commentPatternManager = new CommentPatternManager(connection);
		int contracrId = event.getContractId();
		Contract contract = contractManager.getContractById(contracrId);	
		InetServ inetServ = event.getNewInetServ();
		
		//System.out.println(inetServ.getPassword());
		//System.out.println(contract.getPswd());
		contract.setPswd(inetServ.getPassword());
		//System.out.println(inetServ.getLogin());
		
		contractManager.updateContract(contract);
		
		contractParameterManager.updateStringParam(contracrId, 12, inetServ.getLogin(), 0);
		commentPatternManager.updateContractComment(contracrId);
		//System.out.println(contractParameterManager.getStringParam(contracrId, 12));
    }  
}