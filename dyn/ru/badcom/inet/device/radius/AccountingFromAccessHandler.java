package ru.badcom.inet.device.radius;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;
import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import ru.bitel.bgbilling.kernel.container.managed.ServerContext;
import ru.bitel.bgbilling.kernel.container.resource.ResourceManager;
import ru.bitel.bgbilling.kernel.network.dhcp.DhcpProtocolHandler;
import ru.bitel.bgbilling.kernel.network.radius.RadiusAttributeSet;
import ru.bitel.bgbilling.kernel.network.radius.RadiusDictionary;
import ru.bitel.bgbilling.kernel.network.radius.RadiusListenerWorker;
import ru.bitel.bgbilling.kernel.network.radius.RadiusPacket;
import ru.bitel.bgbilling.kernel.network.radius.RadiusProtocolHandler;
import ru.bitel.bgbilling.kernel.network.radius.RadiusSession;
import ru.bitel.bgbilling.modules.inet.access.Access;
import ru.bitel.bgbilling.modules.inet.access.sa.ProtocolHandlerAdapter;
import ru.bitel.bgbilling.modules.inet.api.common.AccessCodes;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetConnection;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetDevice;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetDeviceType;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetServ;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetServType;
import ru.bitel.bgbilling.modules.inet.radius.InetNas;
import ru.bitel.bgbilling.modules.inet.radius.InetRadiusSessionParams;
import ru.bitel.bgbilling.modules.inet.radius.RadiusAccessRequestHandler;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.ParameterMap;
import ru.bitel.common.function.ThrowingRunnable;


import ru.bitel.bgbilling.kernel.network.ip.IpAddressSet;
import java.lang.reflect.Method;


public class AccountingFromAccessHandler
  extends ProtocolHandlerAdapter
  implements
  RadiusProtocolHandler, DhcpProtocolHandler, RadiusAccessRequestHandler
{
	private static final Logger logger = Logger.getLogger( AccountingFromAccessHandler.class );
	
	private static final ExecutorService executor = Executors.newFixedThreadPool( 10 );
	
	@Resource(name = "access")
	private Access access;

	private int deviceId;

	private static void log(String msg)
	{
		logger.info("Badcom: " + msg);
	}

	@Override
	public void init( Setup setup, int moduleId, InetDevice inetDevice, InetDeviceType inetDeviceType, ParameterMap deviceConfig )
		throws Exception
	{
		super.init( setup, moduleId, inetDevice, inetDeviceType, deviceConfig );

		this.deviceId = inetDevice.getId();

		ServerContext ctx = new ServerContext( setup, moduleId, 0 );
		ctx.init();
		try
		{
			ResourceManager rm = new ResourceManager();
			rm.inject( ctx, this, moduleId );

			ctx.commit();
		}
		catch( NameNotFoundException ex )
		{}
		catch( Exception ex )
		{
			logger.error( ex.getMessage(), ex );
		}
		finally
		{
			ctx.destroy();
		}
	}

	@Override
	public void beforeAuthentication( ServerContext context, RadiusListenerWorker<InetNas> req, RadiusSession<InetNas, InetRadiusSessionParams> radiusSession, RadiusPacket request, RadiusPacket response )
		throws Exception
	{}

	@Override
	public boolean addResponseAttributes( ServerContext context, InetServType inetServType, InetServ inetServ, RadiusPacket response, String realm, Map<String, RadiusAttributeSet> realmAttributeMap, RadiusAttributeSet inetServAttributes, Set<Integer> inetOptionSet )
		throws Exception
	{
		return false;
	}

	@Override
	public void afterAuthorization( ServerContext conext, RadiusListenerWorker<InetNas> req, RadiusSession<InetNas, InetRadiusSessionParams> radiusSession, RadiusPacket request, RadiusPacket response )
		throws Exception
	{
		log("After authorization");
		if( response.getCode() != RadiusPacket.ACCESS_ACCEPT )
		{
			return;
		}

		int accessCode = radiusSession.errorCode;

		InetServ serv = (InetServ)req.getRadiusSession().login;

		InetConnection connection = new InetConnection();
		connection.setDeviceId( deviceId );
		connection.setDevicePort( serv.getInterfaceId() );
		connection.setCircuitId( req.getCircuitId() );

		connection.setContractId( req.getContractId() );
		connection.setServId( serv.getId() );
		connection.setAcctSessionId( request.getStringAttribute( -1, RadiusDictionary.Acct_Session_Id, "UNDEF" ) );
		connection.setCallingStationId( request.getStringAttribute( -1, RadiusDictionary.Calling_Station_Id, "" ) );
//		log( Integer.toString(req.getIpAddressSet().getResourceId())  );
//		log( req.getIpAddressSet().getAddress().toString()  );
		//for ( Method method : Class.forName("ru.bitel.bgbilling.kernel.network.ip.IpAddressSet").getMethods() )
		//{
		//    log( method.getName() );
		//}
		connection.setIpResourceId( req.getIpAddressSet().getResourceId() );
		connection.setInetAddressBytes( req.getIpAddressSet().getAddress() );
		connection.setAccessCode( accessCode );
		connection.setDeviceState( (accessCode != AccessCodes.AUTHORIZATION_SUCCEEDED) ? InetServ.STATE_DISABLE : InetServ.STATE_ENABLE );
		connection.setConnectionStart( new Date() );

		executor.execute( (ThrowingRunnable)() -> {

			access.connectionManager.accountingStart( serv, connection, 4000 );
		} );
	}
}