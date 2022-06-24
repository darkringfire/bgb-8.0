package ru.mybgbilling.custom.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.xml.ws.Holder;

import bitel.billing.server.util.MailMsg;
import ru.bitel.bgbilling.common.BGException;
import ru.bitel.bgbilling.kernel.container.managed.ServerContext;
import ru.bitel.bgbilling.kernel.filestorage.common.bean.BGServerFile;
import ru.bitel.bgbilling.server.util.Setup;

public class RegistrationUtils
{
    private int cid =-1;

    private ServerContext context ;
    
    
    public RegistrationUtils(ServerContext context, int cid )
    {
        this.cid= cid;
        this.context = context;
        
        
    }
    
    
    
    
  
    
    public void sendFile(String email,String subject ,List<DataSource> files) throws BGException
    {
        sendFileImpl( email, subject, files );
    }
    
    public DataSource getFileDataSource( String fileName, InputStream is)
    {
        DataSource ds = new DataSource()
        {
            @Override
            public String getContentType()
            {
                return "application/octet-stream";
            }

            @Override
            public InputStream getInputStream()
                throws IOException
            {
                return is;
            }

            @Override
            public String getName()
            {
                return fileName;
            }

            @Override
            public OutputStream getOutputStream()
                throws IOException
            {
                return null;
            }
        };
        return ds;
    }
    
    
    private void sendFileImpl( String email, String subject, List<DataSource> files) throws BGException
    {
        try
        {
          
            // Прикрепляем файлы
            Multipart multipart = new MimeMultipart();
            for ( DataSource file : files )
            {
                MimeBodyPart filePart = new MimeBodyPart();
                filePart.setDataHandler( new DataHandler( file ) );
                filePart.setHeader("Content-ID", "<" + file.getName() + ">");
                filePart.setDisposition(MimeBodyPart.INLINE);
                try
                {
                    filePart.setFileName( MimeUtility.encodeText( file.getName(), "utf-8", "B" ) );//кодирование миме-заголовка
                }
                catch ( UnsupportedEncodingException e )
                {
                    throw new BGException( e );
                }
                multipart.addBodyPart( filePart );
            }
            
            // И отправляем сообщение
            MailMsg mailMsg = new MailMsg( Setup.getSetup() );
            mailMsg.sendMessage( email, subject, multipart );
            
        }
        catch ( MessagingException e )
        {
            throw new BGException( e );
        }
    }
    
  
    
    public String writeFile(String path, int cid, String prefix, String fileName, InputStream is) throws IOException
    {
        String p = getPath( path, cid, prefix, fileName );
        writeFileImpl( p, is );
        return p;
    }
    
    private String getPath(String path, int cid, String name, String fileName) throws IOException
    {
        String ext = getFileExtension(fileName);
        
        return path+cid+"_"+name+"."+ext; 
    }
    
    private void writeFileImpl(String path, InputStream is) throws IOException
    {
        
        Path p1 = Paths.get(path);
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
