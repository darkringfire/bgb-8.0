package ru.registration;

import java.util.Map;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.ws.Holder;

import ru.bitel.common.model.MapHolder;
import ru.bitel.common.model.Result;

@WebService
public interface RegistrationService
{
    public Result<String> registrationContract( MapHolder<String, String> params ,String fileName1, Holder<DataHandler> file1, String fileName2, Holder<DataHandler> file2,String fileName3, Holder<DataHandler> file3);
}