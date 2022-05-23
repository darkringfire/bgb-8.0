package ru.badcom.scripts.behavior;

import ru.bitel.bgbilling.kernel.container.managed.ServerContext;
import ru.bitel.bgbilling.kernel.contract.balance.common.PaymentService;
import ru.bitel.bgbilling.kernel.contract.balance.common.bean.Payment;
import ru.bitel.bgbilling.kernel.event.events.ContractCreatedEvent;
import ru.bitel.bgbilling.kernel.script.server.dev.EventScriptBase;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.sql.ConnectionSet;

import java.math.BigDecimal;
import java.util.Date;

public class paymentBonus extends EventScriptBase<ContractCreatedEvent> {
    @Override
    public void onEvent(ContractCreatedEvent event, Setup setup, ConnectionSet connectionSet)
            throws Exception {
        ServerContext context = ServerContext.get();
        int cid = event.getContractId();
        PaymentService paymentService = context.getService(PaymentService.class, 0);

        int payType = 6;
        String comment = "Бонус при подключении";
        BigDecimal sum = new BigDecimal(1000);
        Date date = new Date();

        Payment payment = new Payment(0, -1, cid, payType, date, comment, sum, date);
        paymentService.paymentUpdate(payment, null);
    }
}