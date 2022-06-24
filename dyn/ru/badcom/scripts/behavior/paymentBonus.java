package ru.badcom.scripts.behavior;

import ru.bitel.bgbilling.kernel.script.server.dev.EventScriptBase;
import ru.bitel.bgbilling.kernel.container.managed.ServerContext;
import ru.bitel.bgbilling.kernel.event.Event;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.sql.ConnectionSet;
//import java.sql.*;
//import bitel.billing.server.contract.bean.ContractManager;
import ru.bitel.bgbilling.kernel.contract.balance.common.PaymentService;
import ru.bitel.bgbilling.kernel.contract.balance.common.bean.Payment;
//import bitel.billing.server.contract.bean.PaymentManager;
import java.math.*;
import java.util.Date;

public class paymentBonus extends EventScriptBase
{
	@Override
	public void onEvent( Event event, Setup setup, ConnectionSet connectionSet )
		throws Exception
	{
		//System.out.println("START");
		ServerContext context = ServerContext.get();
		//Connection con = connectionSet.getConnection();	
		//ContractManager cpm = new ContractManager( con );
		//PaymentManager payMan = new PaymentManager( con );
		int cid = event.getContractId();
		PaymentService paymentService = context.getService(PaymentService.class, 0);
		
		int payType = 6;
		java.lang.String comment = "Бонус при подключении";
		BigDecimal sum = new BigDecimal(1000);
		Date date =  new Date();
		
		Payment payment = new Payment(0, -1, cid, payType, date, comment, sum, date);
		paymentService.paymentUpdate(payment, null);
		//System.out.println("END");
	}
}