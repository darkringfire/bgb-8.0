package ru.badcom.scripts.behavior;

import ru.bitel.bgbilling.kernel.event.Event;
import ru.bitel.bgbilling.kernel.script.server.dev.EventScriptBase;
import ru.bitel.bgbilling.modules.mps.server.events.MPSSbrfSumEvent;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.sql.ConnectionSet;
import org.apache.log4j.Logger;
import java.math.BigDecimal;

public class MPSSbrfSumScript
	extends EventScriptBase<MPSSbrfSumEvent>
{
	protected static final Logger log = Logger.getLogger( MPSSbrfSumEvent.class );
	
	@Override
	public void onEvent( MPSSbrfSumEvent event, Setup setup, ConnectionSet set )
		throws Exception
	{
    	log.info("~~~~~~~~~~~~~~~~~~~ Sum! ~~~~~~~~~~~~~~~~~~~~~~~");
    	log.info(event.getContractId() );
    	log.info(event.getModuleId() );
    	log.info(event.getPluginId() );
    	log.info(event.getSuperContractId() );
    	log.info(event.getTime() );
    	log.info(event.getTimestamp() );
    	log.info(event.	getUserId() );
    	event.setSum(new BigDecimal(1000));
    	log.info("~~~~~~~~~~~~~~~~~~~ Mus! ~~~~~~~~~~~~~~~~~~~~~~~");
	}

}