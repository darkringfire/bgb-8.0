package ru.mybgbilling.custom.badcom.registration;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.ws.Holder;

import ru.bitel.common.model.MapHolder;
import ru.bitel.common.model.Result;

@WebService 
public interface RegistrationBradcomService
{
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
                                                );
    
    /**
     * Возвращает количество догворов с таким номеров в titile
     * @param title
     * @return
     */
    public Integer findContract(String title);
}
