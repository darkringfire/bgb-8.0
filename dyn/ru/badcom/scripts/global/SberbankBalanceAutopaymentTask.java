package ru.badcom.scripts.global;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import ru.badcom.scripts.global.utils.SingleInstanceGlobalScriptBase;
import ru.bitel.bgbilling.kernel.contract.autopayment.common.bean.Autopayment;
import ru.bitel.bgbilling.kernel.contract.autopayment.common.bean.AutopaymentMode;
import ru.bitel.bgbilling.kernel.contract.balance.server.ConvergenceBalance;
import ru.bitel.bgbilling.kernel.contract.balance.server.ConvergenceBalanceManager;
import ru.bitel.bgbilling.kernel.event.EventProcessor;
import ru.bitel.bgbilling.modules.sberbank.server.bean.AutopaymentManager;
import ru.bitel.bgbilling.modules.sberbank.server.event.SberbankAutopaymentEvent;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.Preferences;
import ru.bitel.common.TimeUtils;
import ru.bitel.common.model.Period;
import ru.bitel.common.model.PeriodWithTime;
import ru.bitel.common.model.SearchResult;
import ru.bitel.common.sql.ConnectionSet;

/**
 * by Semyon Koshechkin 16.03.2019
 * Balance based autopayment sberbank scheduler task
 */
public class SberbankBalanceAutopaymentTask extends SingleInstanceGlobalScriptBase {

    @Override
    public void executeSingle(Setup setup, ConnectionSet connectionSet) throws Exception {
        final int mode = AutopaymentMode.AUTO.getCode(); //Режим автоплатежа - всегда "авто" (кастомный)
        //Список id модулей "Сбербанк", по которым отрабатываем (default=5)
        List<Integer> moduleIds = setup.getIntegerList("custom.script.global.sberbankbalanceautopayment.mids", Collections.singletonList(5));
        //BalanceDao bao = new BalanceDao(connectionSet.getConnection());
        print(moduleIds.size());
        ConvergenceBalanceManager cbm = ConvergenceBalanceManager.getInstance();
        Date now = new Date();
        BigDecimal sum;
        BigDecimal limit;
        ConvergenceBalance cb;
        Preferences modeData;
        String query;
        PreparedStatement ps;
        ResultSet rs;
        Autopayment autopayment;
        for (Integer moduleId : moduleIds) {
            print("moduleID = "+moduleId);
            //AutopaymentManager autopaymentManager = new AutopaymentManager(connectionSet.getConnection(), moduleId);
            //SearchResult<Autopayment> searchResult = new SearchResult<>();
            //autopaymentManager.searchCurrentAutopayment(searchResult, now, mode);
            query = "SELECT * FROM sberbank_autopayment_" + moduleId + " WHERE " + "mode=? AND NOT ISNULL( date1 ) AND date1 < ? AND ( ISNULL( date2 ) OR date2 >= ? )";
            ps = connectionSet.getConnection().prepareStatement(query);
            ps.setInt(1, mode);
            ps.setTimestamp(2, TimeUtils.convertDateToTimestamp(now));
            ps.setTimestamp(3, TimeUtils.convertDateToTimestamp(now));
            rs = ps.executeQuery();
            while (rs.next()){
                autopayment = new Autopayment();
                autopayment.setId(rs.getInt("id"));
                autopayment.setContractId(rs.getInt("contract_id"));
                autopayment.setPeriod(new PeriodWithTime(rs.getTimestamp("date1"), rs.getTimestamp("date2")));
                autopayment.setSum(rs.getBigDecimal("sum"));
                autopayment.setMode(AutopaymentMode.getAutopaymentModeByCode(rs.getInt("mode")));
                autopayment.setModeData(rs.getString("mode_data"));
                autopayment.setData(rs.getString("data"));
                autopayment.setAccessToken(rs.getString("access_token"));
                sum = autopayment.getSum();
                if(sum!=null){
                    modeData = autopayment.getModeData() != null ? new Preferences(autopayment.getModeData(), "\n") : new Preferences();
                    //Храним в autopayment.mode.auto.week.sum лимит баланса, чтобы не переписываь много родного кода BG
                    limit = modeData.getBigDecimal("autopayment.mode.auto.week.sum", null);
                    if(null==limit){
                        error("cid="+autopayment.getContractId()+", autopayment limit is null ("+modeData.get("autopayment.mode.auto.week.sum")+")");
                        continue;
                    }
                    cb = cbm.getBalance(connectionSet, autopayment.getContractId(), System.currentTimeMillis());
                    if(null==cb){
                        error("cid="+autopayment.getContractId()+", balance is null!");
                        continue;
                    }

                    if(limit.compareTo(cb.getBalance())>=0){ //Баланс договора меньше или равен лимита, указанного в автоплатеже
                        //Списываем
                        EventProcessor.getInstance().publish(new SberbankAutopaymentEvent(moduleId, autopayment.getContractId(), mode, sum));
                        print("[pay] cid="+autopayment.getContractId()+", payment requested, sum="+sum.toString());
                    }else{
                        print("[skip] cid = "+autopayment.getContractId()+", balance = "+cb.getBalance().toString()+", limit = "+limit.toString());
                    }
                }else{
                    error("cid="+autopayment.getContractId()+", autopay.summ is null!");
                }
            }
        }
    }
}                    