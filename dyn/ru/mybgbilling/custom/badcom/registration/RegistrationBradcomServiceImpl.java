package ru.mybgbilling.custom.badcom.registration;



import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.apache.log4j.Logger;

import bitel.billing.server.ActionBase;
import bitel.billing.server.contract.bean.CommentPatternManager;
import bitel.billing.server.contract.bean.Contract;
import bitel.billing.server.contract.bean.ContractEmailParamValue;
import bitel.billing.server.contract.bean.ContractParameterManager;
import ru.bitel.bgbilling.common.BGException;
import ru.bitel.bgbilling.kernel.container.managed.ServerContext;
import ru.bitel.bgbilling.kernel.container.service.server.AbstractService;
import ru.bitel.bgbilling.kernel.contract.param.common.bean.ListParamValue;
import ru.bitel.bgbilling.kernel.convert.ConvertUtil;
import ru.bitel.bgbilling.kernel.event.EventProcessor;
import ru.bitel.bgbilling.kernel.event.events.ContractCreatedEvent;
import ru.bitel.bgbilling.kernel.module.common.bean.User;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetServ;
import ru.bitel.bgbilling.modules.inet.api.common.service.InetServService;
import ru.bitel.common.TimeUtils;
import ru.bitel.common.Utils;
import ru.bitel.common.model.MapHolder;
import ru.bitel.common.model.Result;
import ru.mybgbilling.custom.util.RegistrationUtils;

@WebService(endpointInterface = "ru.mybgbilling.custom.badcom.registration.RegistrationBradcomService")

public class RegistrationBradcomServiceImpl 
extends AbstractService
implements RegistrationBradcomService
{
    
    protected static final Logger log = Logger.getLogger( RegistrationBradcomServiceImpl.class );
    //=======================
    //mallow
//    private int INET_MODULE_ID =1;//179;
//    //private int PATTERN_ID=35;
//    private int FIO = 2;//51;
//    private int P_EMAIL_ID = 18;
//    private int P_PHONE_ID = 4;            
//    private int P_DOCUMENTTYPE_ID = 20;     
//    private int P_DOCSER_NUM__ID = 21;      
//    private int P_DOCSUBJECT_ID = 22;       
//    private int P_DOCDATE_ID = 23;                
//    private int P_ID_ID = 8;          
//    private int P_SN_ID = 14;               
//    private int P_COMMENT_ID = 25;   
//    private int P_ADDRESSOFREG_ID = 28;     
//    private int P_ADDRESSOFACCOUNT_ID = 29;
    ////////////=
    
    //testLocal
    private int INET_MODULE_ID =1;
    //private int PATTERN_ID=35;
    private int FIO = 2;
    private int P_EMAIL_ID = 18;
    private int P_PHONE_ID = 5;
    private int P_DOCUMENTTYPE_ID = 20;     
    private int P_DOCSER_NUM__ID = 21;      
    private int P_DOCSUBJECT_ID = 22;
    private int P_DOCDATE_ID = 23;
    
    private int P_DEVICE_ID_ID = 8;          
    private int P_DEVICE_SN_ID = 14;               
    private int P_DEVICE_TYPE_ID = 13;
    private int P_DEVICE_ROUTER_SN_ID = 36;               
    private int P_DEVICE_ROUTER_TYPE_ID = 30;
    
    
    private int P_COMMENT_ID = 25;   
    private int P_AGENT_CONTRACT_ID = 15;
    private int P_ADDRESSOFREG_ID = 28;     
    private int P_ADDRESSOFACCOUNT_ID = 29;
    
    //private int P_AGENT_ID = 54;
    
    private String fileEmail = "boris@bitel.ru;info@badcom.ru";
    private String subject = "Документы по регистрации ";
    
    
    
    
    //=========
    
    //=========
        
    public Result<String> registrationContract( MapHolder<String, String> params ,
                                                String fileName1, Holder<DataHandler> file1, 
                                                String fileName2, Holder<DataHandler> file2,
                                                String fileName3, Holder<DataHandler> file3,
                                                String fileName4, Holder<DataHandler> file4,
                                                String fileName5, Holder<DataHandler> file5,
                                                String fileName6, Holder<DataHandler> file6,
                                                String fileName7, Holder<DataHandler> file7,
                                                String fileName8, Holder<DataHandler> file8,
                                                String fileName9, Holder<DataHandler> file9
                                                )
    {
       Map<String,String> param = params.getMap();
       
       int patternId = Utils.parseInt( param.get( "patternId" ),-1);
        String name = param.get( "name" );
        String lastName = param.get( "lastName" );
        String middleName = param.get( "middleName" );
        String email = param.get( "email" );
        String phone = param.get( "phone" );
        String phoneN = param.get( "phoneN" );
        int documentType = Utils.parseInt( param.get( "documentType" ),-1);
        String docSer = param.get( "docSer" );
        String docNum = param.get( "docNum" );
        String docSubject = param.get( "docSubject" );
        String docDate = param.get( "docDate" );
        String addressOfReg = param.get( "addressOfReg" );
        String addressOfAccount = param.get( "addressOfAccount" );
        String comment = param.get( "comment" );
        //String ID = param.get( "ID" );
        String SN = param.get( "SN" );
        int deviceType = Utils.parseInt( param.get( "deviceType" ),-1);
        
        String routerSN = param.get( "routerSN" );
        int routerDeviceType = Utils.parseInt( param.get( "routerDeviceType" ),-1);
        
        int agentId = Utils.parseInt( param.get( "agentId" ),-1);
        
        //проверка всего, 
        Result<String> res = new Result<String>();
        StringBuilder resultSB = new StringBuilder();
        if(patternId<0)
        {
            resultSB.append( "Неверный шаблон договора" );
            res.setAttribute( "contractText", resultSB.toString() );
            return res;
        }
        
        log.info(  "name"+               name+            
                            "lastName"+           lastName+         
                            "middleName"+         middleName+       
                             "email"+              email+           
                             "phone"+              phone+           
                             "documentType"+       documentType+    
                             "docSer"+             docSer+          
                             "docNum"+             docNum+          
                             "docSubject"+         docSubject+      
                             "docDate"+            docDate+         
                             "addressOfReg"+       addressOfReg+    
                             "addressOfAccount"+   addressOfAccount+
                             "comment"+            comment+         
                             //"ID"+                 ID+              
                             "SN"+                 SN );
        
        
        
        resultSB.append( "Вы зарегестрировали договор:" );
        int cid =-1;
        try
        {
            Connection con = getConnectionSet().getConnection();
            ConvertUtil cu = new ConvertUtil( getSetup(), con );
            ContractParameterManager cpm = new ContractParameterManager(con);
            // делаем договор
            String title = phone + (Utils.isEmptyString( phoneN ) ? "" : "-"+phoneN);
            Contract contract = cu.createContract( patternId, title, "Автоматическое создание", new Date() );
            cid = contract.getId();
            resultSB.append( contract.toString() );
            
            String fio = lastName + " " + name + " " + middleName ;
            // заполняем параметры
            if ( !Utils.isEmptyString( lastName) || !Utils.isEmptyString(middleName) ||!Utils.isEmptyString( name ) )
            {
                cu.addContractParameter( cid, FIO, fio );
            }
            resultSB.append( "\nФИО ["+fio+"]" );
            if ( !Utils.isEmptyString(email) )
            {
                ContractEmailParamValue emailValue = new ContractEmailParamValue();
                emailValue.setEmail( email );
                cpm.updateEmailParam( contract.getId(), P_EMAIL_ID, emailValue, 0 );
                
            }
            if ( !Utils.isEmptyString(phone ) )
            {
                cu.addContractParameter( cid, P_PHONE_ID, phone );
            }
            if ( documentType != -1 )
            {
                cpm.updateListParam( cid, P_DOCUMENTTYPE_ID, new ListParamValue( documentType ), 0 );
            }
            if ( !Utils.isEmptyString(docSer ) || !Utils.isEmptyString(docNum))
            {
                cu.addContractParameter( cid, P_DOCSER_NUM__ID, docSer + " " + docNum );
            }
            if ( !Utils.isEmptyString(docSubject ) )
            {
                cu.addContractParameter( cid, P_DOCSUBJECT_ID, docSubject );
            }
            if ( !Utils.isEmptyString(docDate ) )
            {
                log.info( "ddd="+docDate );
                cpm.updateDateParam( cid, P_DOCDATE_ID, TimeUtils.parseDate( docDate, "dd.MM.yyyy" ), 0 );
            }
            if ( !Utils.isEmptyString(comment ) )
            {
                cu.addContractParameter( cid, P_COMMENT_ID, comment );
            }
            
            cpm.setContractRefParam( cid, P_AGENT_CONTRACT_ID, agentId, User.USER_CONTRACT );
            
            if ( !Utils.isEmptyString(SN ) )
            {
                cu.addContractParameter( cid, P_DEVICE_SN_ID, SN );
            }
            if ( deviceType != -1 )
            {
                cpm.updateListParam( cid, P_DEVICE_TYPE_ID, new ListParamValue( deviceType ), 0 );
            }
            
            
            
            if ( !Utils.isEmptyString(routerSN ) )
            {
                cu.addContractParameter( cid, P_DEVICE_ROUTER_SN_ID, routerSN );
            }
            if ( routerDeviceType != -1 )
            {
                cpm.updateListParam( cid, P_DEVICE_ROUTER_TYPE_ID, new ListParamValue( routerDeviceType ), 0 );
                
            }
            
            
            
            
            if ( !Utils.isEmptyString(addressOfReg) )
            {
                cu.addContractParameter( cid, P_ADDRESSOFREG_ID, addressOfReg );
            }
            if ( !Utils.isEmptyString(addressOfAccount ) )
            {
                cu.addContractParameter( cid, P_ADDRESSOFACCOUNT_ID, addressOfAccount );
            }
            // получаем данные о логине и пароле
            ServerContext context = ServerContext.get();
            InetServService inetServService = context.getService( InetServService.class, INET_MODULE_ID );
            List<InetServ> inetServList = inetServService.inetServList( cid, null );
            if ( inetServList != null && inetServList.size() > 0 )
            {
                InetServ serv = inetServList.get( 0 );
                resultSB.append( "\n login:" + serv.getLogin() );
                resultSB.append( "\n password:" + serv.getPassword() );
                //ид оборудования
                
                cu.addContractParameter( cid, P_DEVICE_ID_ID, "W"+serv.getLogin() );
                
            }
            new CommentPatternManager( con ).updateContractComment( cid );
            EventProcessor.getInstance().request( new ContractCreatedEvent( contract, User.USER_CONTRACT ) );
            RegistrationUtils ru = new RegistrationUtils( context, cid ); 
            //пишем файлы
            List<String> filesNames = new ArrayList<String>();
            
            List<DataSource> files = new ArrayList<DataSource>();
            if ( fileName1 != null && file1 != null )
            {
                //files.add( ru.getFileDataSource( getName( cid, "doc1", fileName1 ), file1.value.getInputStream() ) );
                String p =ru.writeFile( "tmp/", cid, "doc1", fileName1, file1.value.getInputStream()  );
                DataSource fds = new FileDataSource( p );
                files.add(fds);
                filesNames.add( p );
            }
            if ( fileName2 != null && file2 != null )
            {
                //files.add( ru.getFileDataSource( getName( cid, "doc2", fileName2 ), file2.value.getInputStream() ) );
                String p =ru.writeFile( "tmp/", cid, "doc2", fileName2, file2.value.getInputStream()  );
                DataSource fds = new FileDataSource( p );
                files.add(fds);
                filesNames.add( p );
            }
            if ( fileName3 != null && file3 != null )
            {
                //files.add( ru.getFileDataSource( getName( cid, "device", fileName3 ), file3.value.getInputStream() ) );
                String p =ru.writeFile( "tmp/", cid, "device", fileName3, file3.value.getInputStream()  );
                DataSource fds = new FileDataSource( p );
                files.add(fds);
                filesNames.add( p );
            }
            if ( fileName4 != null && file4 != null )
            {
                //files.add( ru.getFileDataSource( getName( cid, "router", fileName4 ), file4.value.getInputStream() ) );
                String p =ru.writeFile( "tmp/", cid, "router", fileName4, file4.value.getInputStream()  );
                DataSource fds = new FileDataSource( p );
                files.add(fds);
                filesNames.add( p );
            }
            if ( fileName5 != null && file5 != null )
            {
                //files.add( ru.getFileDataSource( getName( cid, "mount1", fileName5 ), file5.value.getInputStream() ) );
                String p =ru.writeFile( "tmp/", cid, "mount1", fileName5, file5.value.getInputStream()  );
                DataSource fds = new FileDataSource( p );
                files.add(fds);
                filesNames.add( p );
            }
            if ( fileName6 != null && file6 != null )
            {
                //files.add( ru.getFileDataSource( getName( cid, "mount2", fileName6 ), file6.value.getInputStream() ) );
                String p =ru.writeFile( "tmp/", cid, "mount2", fileName6, file6.value.getInputStream()  );
                DataSource fds = new FileDataSource( p );
                files.add(fds);
                filesNames.add( p );
            }
            if ( fileName7 != null && file7 != null )
            {
                //files.add( ru.getFileDataSource( getName( cid, "mount3", fileName7 ), file7.value.getInputStream() ) );
                String p =ru.writeFile( "tmp/", cid, "mount3", fileName7, file7.value.getInputStream()  );
                DataSource fds = new FileDataSource( p );
                files.add(fds);
                filesNames.add( p );
            }
            if ( fileName8 != null && file8 != null )
            {
                //files.add( ru.getFileDataSource( getName( cid, "mount4", fileName8 ), file8.value.getInputStream() ) );
                String p =ru.writeFile( "tmp/", cid, "mount4", fileName8, file8.value.getInputStream()  );
                DataSource fds = new FileDataSource( p );
                files.add(fds);
                filesNames.add( p );
            }
            if ( fileName9 != null && file9 != null )
            {
                //files.add( ru.getFileDataSource( getName( cid, "mount5", fileName9 ), file9.value.getInputStream() ) );
                String p =ru.writeFile( "tmp/", cid, "mount5", fileName9, file9.value.getInputStream()  );
                DataSource fds = new FileDataSource( p );
                files.add(fds);
                filesNames.add( p );
            }
            ru.sendFile( fileEmail, subject+"("+contract.getTitle()+"["+contract.getId()+"])", files );
            
            for(String fn : filesNames)
            {
                Path p = Paths.get( fn );
                Files.delete( p );
            }
            

        }
        catch( Exception e )
        {
            resultSB.append( e.getLocalizedMessage() );
            e.printStackTrace();
        }
        //данные на возврат 
        
        res.setAttribute( "cid", String.valueOf( cid ) );
        res.setAttribute( "contractText", resultSB.toString() );
        return res;
    }
    
    
    
    private String getName(int cid, String prefix, String fileName) throws IOException
    {
        String ext = getFileExtension(fileName);
        String name = cid+"_"+prefix+"."+ext;
        return name;
    }
    
    private String getFileExtension(  String fileName )
    {
        if ( fileName.lastIndexOf( "." ) != -1 && fileName.lastIndexOf( "." ) != 0 )
            return fileName.substring( fileName.lastIndexOf( "." ) + 1 );
        else return "";
    }



    @Override
    public Integer findContract( String title )
    {
        int res = 0;
        try ( PreparedStatement ps = getConnection().prepareStatement( "SELECT count(*) FROM contract WHERE title LIKE ?" ) )
        {
            ps.setString( 1, "%" + title + "%" );
            ResultSet rs = ps.executeQuery();
            if(rs.next())
            {
                res = rs.getInt( 1);
            }
            rs.close();
        }
        catch( Exception e )
        {
            
        }

        return res;
    }


}
