package ru.badcom.bgbilling.events;

import bitel.billing.server.contract.bean.ContractManager;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.bitel.bgbilling.common.BGException;
import ru.bitel.bgbilling.kernel.container.managed.ServerContext;
import ru.bitel.bgbilling.kernel.contract.api.common.bean.ContractTariff;
import ru.bitel.bgbilling.kernel.contract.api.common.bean.ContractTariffGroup;
import ru.bitel.bgbilling.kernel.contract.api.common.event.ContractModifiedEvent;
import ru.bitel.bgbilling.kernel.contract.api.server.bean.ContractDao;
import ru.bitel.bgbilling.kernel.contract.api.server.bean.ContractTariffDao;
import ru.bitel.bgbilling.kernel.contract.api.server.bean.ContractTariffGroupDao;
import ru.bitel.bgbilling.kernel.contract.label.server.bean.ContractLabelManager;
import ru.bitel.bgbilling.kernel.event.events.ContractTariffUpdateEvent;
import ru.bitel.bgbilling.kernel.script.server.dev.EventScriptBase;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.model.Period;
import ru.bitel.common.sql.ConnectionSet;
import ru.bitel.oss.kernel.entity.common.bean.EntityAttrDate;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


/**
 * Событие смены тарифа. Установка групп договора, группы тарифов, параметра даты активации договора
 *
 * @author a.kosorotikov
 * @version 1.1-2022.05.20
 */
public class ContractTariffUpdate_SetGroups<E extends ContractTariffUpdateEvent> extends EventScriptBase<E> {
    protected static final Logger logger = LogManager.getLogger(ContractTariffUpdate_SetGroups.class);

    // pid Даты активации договора
    private static final int PARAM_ID_DATE = 7;
    // id Неактивированного тарифа
    private static final int PLAN_ID_NEW = 25;
    // id Группы неактивированных договоров
    private static final int GROUP_ID_NEW = 53;
    // id Группы активированных, но не настроеных договоров
    private static final int GROUP_ID_ACTIVATED = 52;

    // each row is tariff group IDs: [new, activated]
    private static final List<List<Integer>> TARIFF_SWITCHES = Arrays.asList(
            Arrays.asList(3, 1),
            Arrays.asList(4, 5),
            Arrays.asList(6, 7)
    );

    private int cid;
    private ServerContext context;
    private ContractManager contractManager;
    private ContractDao contractDao;
    private ContractTariffGroupDao tariffGroupDao;
    private ContractLabelManager contractLabelManager;
    private ContractTariffDao contractTariffDao;
    private ContractTariff tariff;
    private ContractTariffGroup contractTariffGroup;
    private EntityAttrDate dateParameter;
    private int planId;
    private Period tariffPeriod;
    private List<Integer> tariffSwitch;

    private void initEventVars(E event, ConnectionSet connectionSet) throws BGException {

        Connection con = connectionSet.getConnection();

        cid = event.getContractId();
        int uid = event.getUserId();

        context = ServerContext.get();
        contractManager = new ContractManager(con);
        contractDao = new ContractDao(con, uid);
        tariffGroupDao = new ContractTariffGroupDao(con);
        contractLabelManager = new ContractLabelManager(con);
        contractTariffDao = new ContractTariffDao(con);

        tariff = contractTariffDao.get(event.getContractTariff().getId());
        dateParameter = (EntityAttrDate) contractDao.getContractParameter(cid, PARAM_ID_DATE);
        planId = tariff.getTariffPlanId();
        tariffPeriod = tariff.getPeriod();

        // get tariff group ID from tariff if not default
        int contractTariffGroupId = tariffGroupDao.list(cid, null).stream()
                .map(ContractTariffGroup::getTariffGroupId).findFirst().orElse(0);
        int givenTariffGroupId = tariff.getTariffGroupId();
        int newContractTariffGroupId;
        // if given group (default | none) leave as is
        if (Arrays.asList(-1, 0).contains(givenTariffGroupId)) {
            newContractTariffGroupId = contractTariffGroupId;
        } else {
            newContractTariffGroupId = givenTariffGroupId;
        }

        tariffSwitch = TARIFF_SWITCHES.stream()
                .filter(sw -> sw.contains(newContractTariffGroupId))
                .findFirst().orElse(null);
        if (tariffSwitch == null) {
            tariffSwitch = Arrays.asList(contractTariffGroupId, contractTariffGroupId);
            tariff.setTariffGroupId(givenTariffGroupId);
        } else {
            tariff.setTariffGroupId(-1);
        }

        contractTariffGroup = new ContractTariffGroup();
        contractTariffGroup.setContractId(cid);
        contractTariffGroup.setTariffGroupId(tariffSwitch.get(1));
    }


    @Override
    public void onEvent(E event, Setup setup, ConnectionSet connectionSet)
            throws BGException, SQLException, IllegalAccessException {
        initEventVars(event, connectionSet);

        if (planId == PLAN_ID_NEW) {
            if (tariffPeriod.getDateTo() == null) {
                contractManager.addContractGroup(cid, GROUP_ID_NEW);
                contractManager.deleteContractGroup(cid, GROUP_ID_ACTIVATED);
                dateParameter.setValue(null);
                contractTariffGroup.setTariffGroupId(tariffSwitch.get(0));
            } else {
                contractManager.addContractGroup(cid, GROUP_ID_ACTIVATED);
                contractManager.deleteContractGroup(cid, GROUP_ID_NEW);
                Calendar activationDate = Calendar.getInstance();
                activationDate.setTime(tariffPeriod.getDateTo());
                activationDate.add(Calendar.DAY_OF_MONTH, 1);
                dateParameter.setValue(activationDate.getTime());
            }
        } else {
            if (dateParameter.getValue() == null) {
                if (tariffPeriod.getDateFrom() != null) {
                    dateParameter.setValue(tariffPeriod.getDateFrom());
                }
                contractManager.addContractGroup(cid, GROUP_ID_ACTIVATED);
                contractManager.deleteContractGroup(cid, GROUP_ID_NEW);
            }
        }

        contractTariffDao.update(tariff);
        try {
            ContractLabelManager.class.getMethod("syncLabelAndGroupContract", int.class)
                    .invoke(contractLabelManager, cid);
            logger.warn("Method syncLabelAndGroupContract exists. No need try/catch");
        }
        catch (NoSuchMethodException ignore) {
        }
        catch (InvocationTargetException e) {
            throw new BGException(e.getTargetException());
        }
        contractDao.updateContractParameter(cid, dateParameter);
        tariffGroupDao.update(contractTariffGroup);
        context.publishAfterCommit(new ContractModifiedEvent(0, cid));
    }
}