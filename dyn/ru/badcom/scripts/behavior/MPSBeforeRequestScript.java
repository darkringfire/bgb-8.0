package ru.badcom.scripts.behavior;

import ru.bitel.bgbilling.kernel.event.Event;
import ru.bitel.bgbilling.kernel.script.server.dev.EventScriptBase;
import ru.bitel.bgbilling.modules.mps.server.events.MPSBeforeRequestEvent;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.sql.ConnectionSet;
import org.apache.log4j.Logger;
import ru.bitel.bgbilling.modules.mps.server.bean.MPSResponse;
import bitel.billing.server.contract.bean.Contract;
import java.util.HashMap;


public class MPSBeforeRequestScript
	extends EventScriptBase<MPSBeforeRequestEvent>
{
	protected static final Logger log = Logger.getLogger( MPSBeforeRequestEvent.class );
	
	@Override
	public void onEvent( MPSBeforeRequestEvent event, Setup setup, ConnectionSet set )
		throws Exception
	{
    	log.info("~~~~~~~~~~~~~~~~~~~ Yo! ~~~~~~~~~~~~~~~~~~~~~~~");
	}
}