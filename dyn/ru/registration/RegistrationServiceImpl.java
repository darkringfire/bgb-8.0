package ru.registration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.apache.log4j.Logger;

import bitel.billing.server.ActionBase;
import bitel.billing.server.contract.bean.CommentPatternManager;
import bitel.billing.server.contract.bean.Contract;
import bitel.billing.server.contract.bean.ContractParameterManager;
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

@WebService(endpointInterface = "ru.registration.RegistrationService")

public class RegistrationServiceImpl
extends AbstractService
implements RegistrationService
{
    
    protected static final Logger log = Logger.getLogger( RegistrationServiceImpl.class );
    private int INET_MODULE_ID =1;//179;
    //private int PATTERN_ID=35;
    private int FIO = 2;//51;
    private int P_EMAIL_ID = 18;
    private int P_PHONE_ID = 4;            
    private int P_DOCUMENTTYPE_ID = 20;     
    private int P_DOCSER_NUM__ID = 21;      
    private int P_DOCSUBJECT_ID = 22;       
    private int P_DOCDATE_ID = 23;                
    private int P_ID_ID = 8;          
    private int P_SN_ID = 14;               
    private int P_COMMENT_ID = 25;   
        

   

        
        
           

    private int P_ADDRESSOFREG_ID = 28;     
    private int P_ADDRESSOFACCOUNT_ID = 29; 
    
        
    public Result<String> registrationContract( MapHolder<String, String> params,String fileName1, Holder<DataHandler> file1,String fileName2, Holder<DataHandler> file2,String fileName3, Holder<DataHandler> file3 )
    {
       Map<String,String> param = params.getMap();
       
       int patternId = Utils.parseInt( param.get( "patternId" ),-1);
        String name = param.get( "name" );
        String lastName = param.get( "lastName" );
        String middleName = param.get( "middleName" );
        String email = param.get( "email" );
        String phone = param.get( "phone" );
        int documentType = Utils.parseInt( param.get( "documentType" ),-1);
        String docSer = param.get( "docSer" );
        String docNum = param.get( "docNum" );
        String docSubject = param.get( "docSubject" );
        String docDate = param.get( "docDate" );
        String addressOfReg = param.get( "addressOfReg" );
        String addressOfAccount = param.get( "addressOfAccount" );
        String comment = param.get( "comment" );
        String ID = param.get( "ID" );
        String SN = param.get( "SN" );
        
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
                             "ID"+                 ID+              
                             "SN"+                 SN );
        
        
        
        resultSB.append( "Вы зарегестрировали договор:" );
        int cid =-1;
        try
        {
            Connection con = getConnectionSet().getConnection();
            ConvertUtil cu = new ConvertUtil( getSetup(), con );
            ContractParameterManager cpm = new ContractParameterManager(con);
            // делаем договор
            Contract contract = cu.createContract( patternId, null, "Автоматическое создание", new Date() );
            cid = contract.getId();
            resultSB.append( contract.toString() );
            // заполняем параметры
            if ( !Utils.isEmptyString( lastName) || !Utils.isEmptyString(middleName) ||!Utils.isEmptyString( name ) )
            {
                cu.addContractParameter( cid, FIO, lastName + " " + middleName + " " + name );
            }
            if ( !Utils.isEmptyString(email) )
            {
                cu.addContractParameter( cid, P_EMAIL_ID, email );
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
            if ( !Utils.isEmptyString(ID ) )
            {
                cu.addContractParameter( cid, P_ID_ID, ID );
            }
            if ( !Utils.isEmptyString(SN ) )
            {
                cu.addContractParameter( cid, P_SN_ID, SN );
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
            }
            new CommentPatternManager( con ).updateContractComment( cid );
            EventProcessor.getInstance().request( new ContractCreatedEvent( contract, User.USER_CONTRACT ) );
            //пишем файлы
            if(fileName1!=null&&file1!=null)
            {
                writeFile( cid, "doc1", fileName1, file1.value.getInputStream() );
            }
            if(fileName2!=null&&file2!=null)
            {
            writeFile( cid, "doc2", fileName2, file2.value.getInputStream() );
            }
            if(fileName3!=null&&file3!=null)
            {
            writeFile( cid, "device", fileName3, file3.value.getInputStream() );
            }

        }
        catch( Exception e )
        {
            resultSB.append( e.getLocalizedMessage() );
            e.printStackTrace();
        }

        res.setAttribute( "cid", String.valueOf( cid ) );
        res.setAttribute( "contractText", resultSB.toString() );
        return res;
    }
    
    
    
    private void writeFile(int cid, String prefix, String fileName, InputStream is) throws IOException
    {
        String ext = getFileExtension(fileName);
        Path p1 = Paths.get("tmp/"+prefix+"_"+cid+"."+ext);
        java.nio.file.Files.copy(
                                 is, 
                                 p1, 
                                 StandardCopyOption.REPLACE_EXISTING);
    }
    
    private String getFileExtension(  String fileName )
    {
        if ( fileName.lastIndexOf( "." ) != -1 && fileName.lastIndexOf( "." ) != 0 )
            return fileName.substring( fileName.lastIndexOf( "." ) + 1 );
        else return "";
    }


}
