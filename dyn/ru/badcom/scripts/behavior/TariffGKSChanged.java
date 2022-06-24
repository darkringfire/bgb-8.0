package ru.badcom.scripts.behavior;

import ru.bitel.bgbilling.kernel.script.server.dev.EventScript;
import ru.bitel.bgbilling.kernel.script.server.dev.EventScriptBase;

import ru.bitel.bgbilling.kernel.event.events.ContractTariffUpdateEvent;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.sql.ConnectionSet;

import java.sql.*;
import ru.bitel.bgbilling.kernel.contract.api.server.bean.ContractTariffGroupDao;
import ru.bitel.bgbilling.kernel.contract.api.common.bean.ContractTariffGroup;
//import bitel.billing.server.contract.bean.*;
import bitel.billing.server.contract.bean.ContractTariff;
import bitel.billing.server.contract.bean.ContractParameterManager;
import bitel.billing.server.contract.bean.ContractManager;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;

//import ru.bitel.bgbilling.kernel.contract.api.common.bean.ContractTariff;
import ru.bitel.bgbilling.kernel.tariff.server.bean.TariffPlanDao;
import ru.bitel.bgbilling.kernel.tariff.common.bean.TariffPlan;

import ru.bitel.bgbilling.kernel.contract.api.common.service.ContractTariffService;


public class TariffGKSChanged extends EventScriptBase<ContractTariffUpdateEvent> implements EventScript<ContractTariffUpdateEvent>
{
	@Override
	public void onEvent( ContractTariffUpdateEvent event, Setup setup, ConnectionSet connectionSet )
      throws Exception
	{
		System.out.println("--- Tariff change event ---");

		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
		
		// activation date pid
		int DATE_PID = 7;
		// unactivated tarif id
		int TEMP_PLAN_ID = 25;
		// unactivated group id
		int TEMP_GID = 53;
		// activated but not registered group id
		int ACTIVATED_TEMP_GID = 52;
		// unactivated tarif group id
		int TEMP_TGID = 6;
		// activated tarif group id
		int ACTIVATED_TGID = 7;

		Connection con = connectionSet.getConnection();

		ContractTariff tariff = event.getContractTariff();
		TariffPlanDao planDao = new TariffPlanDao(con);
		ContractParameterManager contractParameterManager = new ContractParameterManager(con);
		ContractManager contractManager = new ContractManager( con );
		int cid = event.getContractId();
		int planId = tariff.getTariffPlanId();
		TariffPlan plan = planDao.get(planId);
		Calendar planDate1 = tariff.getDate1();
		Calendar planDate2 = tariff.getDate2();
		
		ContractTariffGroupDao ctgd = new ContractTariffGroupDao (con);
		ContractTariffGroup ctg = new ContractTariffGroup();
		ctg.setContractId(cid);
		ctg.setTariffGroupId(ACTIVATED_TGID);

		// ----------------------
		
		if ( event.isAddTariff() ) {
			System.out.println("Tariff added");
		} else {
			System.out.println("Tariff changed");
		}
		System.out.println( "Tariff: " + plan.getTitle() );
		System.out.println( "Tariff plan id = " + planId );
		System.out.println( "Dates :" );
		if (planDate1 != null) {
			System.out.println( fmt.format(planDate1.getTime()) );
		} else {
			System.out.println( "null" );
		}
		if (planDate2 != null) {
			System.out.println( fmt.format(planDate2.getTime()) );
		} else {
			System.out.println( "null" );
		}

		// ----------------------

		Date mewDate = null;
		if (planId == TEMP_PLAN_ID) {
			// TEMP TARIFF EVENT
			System.out.println("Temp tariff");
			if (planDate2 != null) {
				// TEMP TARIFF CLOSED
				System.out.println("Add group: temp activated");
				contractManager.addContractGroup( cid, ACTIVATED_TEMP_GID );

				System.out.println("Delete group: temp");
				contractManager.deleteContractGroup( cid, TEMP_GID );

				System.out.println("New date :" + fmt.format(planDate2.getTime()));
				mewDate = new Date(planDate2.getTime().getTime() + (1000*60*60*24));
				contractParameterManager.updateDateParam(cid, DATE_PID, mewDate, 0);
			} else {
				// TEMP TARIFF OPENED
				System.out.println("Add group: temp");
				contractManager.addContractGroup( cid, TEMP_GID );

				System.out.println("Delete group: temp activated");
				contractManager.deleteContractGroup( cid, ACTIVATED_TEMP_GID );

				contractParameterManager.deleteDateParam(cid, DATE_PID, 0);
				ctg.setTariffGroupId(TEMP_TGID);
			}
		} else {
			// ACTIVATE TARIFF EVENT
			Calendar contractDate = contractParameterManager.getDateParam(cid, DATE_PID);
			if (contractDate == null) {
				System.out.println("Contract date: " + fmt.format(planDate1.getTime()));
				mewDate = planDate1.getTime();
				contractParameterManager.updateDateParam(cid, DATE_PID, mewDate, 0);

				System.out.println("Add group: temp activated");
				contractManager.addContractGroup( cid, ACTIVATED_TEMP_GID );

				System.out.println("Delete group: temp");
				contractManager.deleteContractGroup( cid, TEMP_GID );

			}
		}
		ctgd.update(ctg);
		
		ctgd.recycle();
	}
}