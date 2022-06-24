package ru.badcom.scripts.behavior;

import ru.bitel.bgbilling.kernel.event.Event;
import ru.bitel.bgbilling.kernel.script.server.dev.EventScriptBase;
import ru.bitel.bgbilling.modules.mps.server.events.MPSBeforeResponseEvent;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.sql.ConnectionSet;
import org.apache.log4j.Logger;
import ru.bitel.bgbilling.modules.mps.server.bean.MPSResponse;
import bitel.billing.server.contract.bean.Contract;
import java.util.HashMap;

public class MPSBeforeResponseScript
	extends EventScriptBase<MPSBeforeResponseEvent>
{
	protected static final Logger log = Logger.getLogger( MPSBeforeResponseEvent.class );
	
	@Override
	public void onEvent( MPSBeforeResponseEvent event, Setup setup, ConnectionSet set )
		throws Exception
	{
    	log.info("~~~~~~~~~~~~~~~~~~~ Resp! ~~~~~~~~~~~~~~~~~~~~~~~");
    	//MPSResponse response = event.getResponse();
		//Contract contract = response.getContract();
		//if ( contract != null )
        //{
        	//HashMap<String, Object> respParams = response.getParameters();
        	//newParameters.put( "add", "address: " + address );
        	//log.info(respParams);
        	//respParams.put("fields", "fio:Test");
        	//respParams.put("add", "address:XYZ");
        	//respParams.put("recsum", "1000");
        	//respParams.put("SUM", "1000");
        	//log.info(respParams);
        	//event.setProcessed(true);
        	//response.getParameters().put( "", responseParameters );
        //}
    	//log.info("~~~~~~~~~~~~~~~~~~~ pserR! ~~~~~~~~~~~~~~~~~~~~~~~");
	}

}