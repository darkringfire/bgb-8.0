// BadcomTVIPOrderManager
package ru.badcom.bgbilling.modules.tv;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import bitel.billing.server.contract.bean.Contract;
import bitel.billing.server.contract.bean.ContractManager;
import bitel.billing.server.contract.bean.ContractParameterManager;
import ru.bitel.bgbilling.kernel.container.managed.ServerContext;
import ru.bitel.bgbilling.modules.tv.access.om.AbstractOrderEvent;
import ru.bitel.bgbilling.modules.tv.access.om.AccountOrderEvent;
import ru.bitel.bgbilling.modules.tv.access.om.OrderManager;
import ru.bitel.bgbilling.modules.tv.access.om.OrderManagerAdapter;
import ru.bitel.bgbilling.modules.tv.access.om.ProductOrderEvent;
import ru.bitel.bgbilling.modules.tv.api.common.bean.TvAccount;
import ru.bitel.bgbilling.modules.tv.api.common.bean.TvDevice;
import ru.bitel.bgbilling.modules.tv.api.common.bean.TvDeviceType;
import ru.bitel.bgbilling.modules.tv.api.server.bean.TvAccountDao;
import ru.bitel.bgbilling.modules.tv.dyn.JsonClient;
import ru.bitel.bgbilling.modules.tv.dyn.JsonClient.JsonClientException;
import ru.bitel.bgbilling.modules.tv.dyn.JsonClient.Method;
import ru.bitel.bgbilling.modules.tv.dyn.JsonClient.Request;
import ru.bitel.bgbilling.modules.tv.dyn.JsonClient.Response;
import ru.bitel.bgbilling.modules.tv.dyn.TvDynUtils;
import ru.bitel.common.ParameterMap;
import ru.bitel.common.Utils;
import ru.bitel.oss.systems.inventory.product.common.bean.ProductSpec;
import ru.bitel.oss.systems.inventory.service.common.bean.ServiceSpec;

public class TVIPOrderManager
        extends OrderManagerAdapter
        implements OrderManager {
    private static final Logger logger = Logger.getLogger(TVIPOrderManager.class);

    private static final String dateTimePattern = "yyyy-MM-dd'T'HH:mm:ssZ";
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern).withZone(ZoneId.of("UTC"));

    private JsonClient jsonClient;

    private ContractManager contractManager;
    private ContractParameterManager contractParameterManager;

    /**
     * Полная или частичная синхронизация продуктов.
     */
    private boolean productSyncMode;

    /**
     * Если true - синхронизация на уровне сервисов, а не продуктов.
     */
    private boolean serviceMode;

    private int customerFirstNamePid;
    private int customerMiddleNamePid;
    private int customerLastNamePid;

    private int moduleId;
    private TvAccountDao tvAccountDao;

    /**
     * Форматирование для login.
     */
    private String loginFormat;

    @Override
    public Object init(ServerContext ctx, int moduleId, TvDevice tvDevice, TvDeviceType tvDeviceType, ParameterMap config)
            throws Exception {
        logger.info("init");
        this.moduleId = moduleId;

        super.init(ctx, moduleId, tvDevice, tvDeviceType, config);

        String url = config.get("om.url", config.get("tvip.api.url", "https://my.tvip.media/api/provider/"));
        String login = config.get("om.login", config.get("tvip.api.login", tvDevice.getUsername()));
        String password = config.get("om.password", config.get("tvip.api.password", tvDevice.getPassword()));

        this.jsonClient = new JsonClient(new URL(url), login, password);

        this.productSyncMode = config.getInt("om.product.syncMode", 1) > 0;
        this.serviceMode = config.getInt("om.product.serviceMode", 0) > 0;

        int customerNamePid = config.getInt("customer.name.pid", 0);
        customerLastNamePid = config.getInt("customer.lastName.pid", customerNamePid);
        customerFirstNamePid = config.getInt("customer.firstName.pid", 0);
        customerMiddleNamePid = config.getInt("customer.middleName.pid", 0);

        String loginFormat = config.get("om.login.format", config.get("om.account.loginFormat", null));
        if (Utils.notBlankString(loginFormat)) {
            this.loginFormat = loginFormat;
        } else {
            this.loginFormat = null;
        }

        return null;
    }

    @Override
    public Object destroy()
            throws Exception {
        logger.info("destroy");
        return null;
    }

    @Override
    public Object connect(ServerContext ctx)
            throws Exception {
        logger.info("connect");
        contractManager = new ContractManager(ctx.getConnection());
        contractParameterManager = new ContractParameterManager(ctx.getConnection());

        tvAccountDao = new TvAccountDao(ctx.getConnection(), moduleId);

        return null;
    }

    @Override
    public Object disconnect(ServerContext ctx)
            throws Exception {
        logger.info("disconnect");
        try {
            contractParameterManager = null;

            contractManager.recycle();
            contractManager = null;
        } finally {
            if (jsonClient != null) {
                jsonClient.disconnect();
            }
        }

        if (jsonClient != null) {
            jsonClient.disconnect();
        }

        if (tvAccountDao != null) {
            tvAccountDao.recycle();
            tvAccountDao = null;
        }

        return null;
    }

    private Request request(Method method, String resource)
            throws JSONException {
        logger.info("request");
        return jsonClient.newRequest()
                .setMethod(method)
                .setResource(resource)
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "*/*");
    }

    @Override
    public Object accountCreate(AccountOrderEvent e, ServerContext ctx)
            throws Exception {
        logger.info("accountCreate");

        return accountModify(e, ctx);
    }

    @Override
    public Object accountModify(AccountOrderEvent e, ServerContext ctx)
            throws Exception {
        logger.info("accountModify");

        final long tvipAccountId = accountModify0(e);

        try {
            // синхронизируем все продукты
            if (tvipAccountId > 0) {
                productsModifySyncFull(e, tvipAccountId, e.getNewState() == TvAccount.STATE_ENABLE);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

        return null;
    }

    /**
     * Редактирование и создание аккаунта.
     * <p>
     * {
     * "account_desc": "string",
     * "contract_info": "string",
     * "devices_per_account_limit": 0,
     * "enabled": true,
     * "fullname": "string",
     * "id": 0,
     * "login": "string",
     * "main_address": "string",
     * "pin_md5": "string",
     * "provider": 0,
     * "remote_custom_field": "string"
     * }
     *
     * @return userId
     */
    private long accountModify0(final AccountOrderEvent e)
            throws Exception {
        logger.info("accountModify0");

        long tvipAccountId = 0;

        TvAccount tvAccount = e.getNewTvAccount() != null ? e.getNewTvAccount() : e.getOldTvAccount();

        // создание аккаунта
        if (e.getOldTvAccount() == null || Utils.isBlankString(e.getOldTvAccount().getDeviceAccountId())) {
            final Contract contract = contractManager.getContractById(e.getContractId());

            final String[] name = TvDynUtils.getName(contractParameterManager, contract, customerLastNamePid, customerFirstNamePid, customerMiddleNamePid);

            final String login = TvDynUtils.getLogin(tvAccount, loginFormat);

            JSONObject tvipAccount = new JSONObject();
            tvipAccount.put("login", login);
            tvipAccount.put("fullname", name[0]);
            tvipAccount.put("contract_info", contract.getTitle());
            tvipAccount.put("account_desc", contract.getComment());
            tvipAccount.put("enabled", e.getNewState() == TvAccount.STATE_ENABLE);

            putPinMd5(tvipAccount, tvAccount.getPassword());

            Response response = request(Method.post, "accounts").setBody(tvipAccount).execute();

            if (response.getException() == null) {
                JSONObject result = response.getJsonObject();
                tvipAccountId = result.getLong("id");
            } else {
                if (response.getException().getResponseCode() == 400) {
                    // тут надо как то получить tvipAccountId от IPTV платформы (каким то запросом...)
                    response = request(Method.post, "accounts").setParam("login", login).execute();
                    if (response.getException() == null) {
                        JSONObject result = response.getJsonObject();
                        JSONArray data = result.getJSONArray("data");
                        if (data.length() > 0) {
                            tvipAccountId = data.getJSONObject(0).getLong("id");
                        }
                    }
                }
            }

            if (tvipAccountId > 0) {
                e.getEntry().setDeviceAccountId(String.valueOf(tvipAccountId));
                accountStateModify0(e, tvipAccountId);
            }
        } else // обновление аккаунта
        {
            tvipAccountId = Utils.parseLong(e.getOldTvAccount().getDeviceAccountId());

            final Contract contract = contractManager.getContractById(e.getContractId());

            final String[] name = TvDynUtils.getName(contractParameterManager, contract, customerLastNamePid, customerFirstNamePid, customerMiddleNamePid);

            final String login = TvDynUtils.getLogin(tvAccount, loginFormat);

            JSONObject tvipAccount = new JSONObject();
            tvipAccount.put("id", tvipAccountId);
            tvipAccount.put("login", login);
            tvipAccount.put("fullname", name[0]);
            tvipAccount.put("contract_info", contract.getTitle());
            tvipAccount.put("account_desc", contract.getComment());
            tvipAccount.put("enabled", e.getNewState() == TvAccount.STATE_ENABLE);

            // проставляем пароль только если он изменился
            if (!Utils.maskNull(e.getOldTvAccount().getPassword()).equals(Utils.maskNull(tvAccount.getPassword()))) {
                putPinMd5(tvipAccount, tvAccount.getPassword());
            }

            JSONObject result = request(Method.post, "accounts")
                    .setBody(tvipAccount)
                    .execute()
                    .getJsonObject();

            tvipAccountId = result.getLong("id");

            e.getEntry().setDeviceAccountId(String.valueOf(tvipAccountId));

            if (e.getOldState() != e.getNewState()) {
                accountStateModify0(e, tvipAccountId);
            }
        }

        return tvipAccountId;
    }

    @Override
    public Object accountRemove(AccountOrderEvent e, ServerContext ctx)
            throws Exception {
        logger.info("accountRemove");

        final long tvipAccount = Utils.parseLong(e.getOldTvAccount().getDeviceAccountId());

        if (tvipAccount <= 0) {
            logger.warn("deviceAccountId is empty for " + e.getOldTvAccount());
            return null;
        }

        try {
            logger.info("BEFORE REMOVE REQUEST");
            JSONObject result = request(Method.delete, "accounts")
                    .setId(tvipAccount)
                    .execute()
                    .getJsonObject();
            logger.info("AFTER REMOVE REQUEST");
            if (logger.isDebugEnabled()) {
                logger.debug(result);
            }
        } catch (JsonClientException ex) {
            if (ex.getResponseCode() == 404) {
                logger.info("Error 404 - account already removed");
                return null;
            }

            if (ex.getResponseCode() == 403) {
                logger.info("Error 403 - account already removed");
                return null;
            }

            if (ex.getResponseCode() == 400) {
                logger.info("Error 400 - account already removed");
                return null;
            }

            throw ex;
        }

        return null;
    }

    @Override
    public Object accountStateModify(AccountOrderEvent e, ServerContext ctx)
            throws Exception {
        logger.info("accountStateModify");

        if (e.getOldTvAccount() == null || Utils.isBlankString(e.getOldTvAccount().getDeviceAccountId())) {
            return accountModify(e, ctx);
        }

        long tvipAccountId = Utils.parseLong(e.getOldTvAccount().getDeviceAccountId());

        accountStateModify0(e, tvipAccountId);

        // if( e.isOptionsModified() )
        {
            accountOptionsModify0(e, tvipAccountId, e.getNewState() == TvAccount.STATE_ENABLE);
        }

        return null;
    }

    private void accountStateModify0(final AccountOrderEvent e, final long tvipAccountId)
            throws Exception {
        logger.info("accountStateModify0");
        JSONObject tvipAccount = new JSONObject();
        tvipAccount.put("id", tvipAccountId);
        tvipAccount.put("enabled", e.getNewState() == TvAccount.STATE_ENABLE);

        JSONObject result = request(Method.post, "accounts")
                .setBody(tvipAccount)
                .execute()
                .getJsonObject();

        if (logger.isDebugEnabled()) {
            logger.debug(result);
        }
    }

    @Override
    public Object accountOptionsModify(AbstractOrderEvent e, ServerContext ctx)
            throws Exception {
        logger.info("accountOptionsModify");

        final long tvipAccountId = Utils.parseLong(e.getTvAccountRuntime().getTvAccount().getDeviceAccountId());

        return accountOptionsModify0(e, tvipAccountId, e.getTvAccountRuntime().getTvAccount().getDeviceState() == TvAccount.STATE_ENABLE);
    }

    private Object accountOptionsModify0(AbstractOrderEvent e, final long tvipAccountId, final boolean accountEnabled)
            throws Exception {
        logger.info("accountOptionsModify0");

        return productsModifySyncFull(e, tvipAccountId, accountEnabled);
    }

    /**
     * Полная синхронизация продуктов/пакетов.
     *
     */
    private Object productsModifySyncFull(final AbstractOrderEvent e, final long tvipAccount, final boolean accountEnabled)
            throws Exception {
        logger.info("productsModifyFullSync");

        final Set<Long> servicesToAdd = new HashSet<>();

        if (accountEnabled) {
            if (serviceMode) {
                // получаем полный список активных сервисов
                for (ServiceSpec serviceSpec : e.getFullServiceSpecSetToEnable()) {
                    servicesToAdd.add(Utils.parseLong(serviceSpec.getIdentifier().trim()));
                }
            } else {
                // получаем список активных продуктов
                for (ProductSpec productSpec : e.getFullProductSpecSetToEnable()) {
                    logger.info("Product: " + productSpec);

                    servicesToAdd.add(Utils.parseLong(productSpec.getIdentifier().trim()));
                }

                // добавляем продукты-опции
                if (!(e instanceof AccountOrderEvent) || ((AccountOrderEvent) e).getNewState() == TvAccount.STATE_ENABLE) {
                    for (ProductSpec productSpec : e.getNewDeviceOptionProductSpecs()) {
                        logger.info("Product (option): " + productSpec);

                        servicesToAdd.add(Utils.parseLong(productSpec.getIdentifier().trim()));
                    }
                }
            }
        }

        // удаляем некорректные записи
        servicesToAdd.remove(0L);

        // текущие подписки ID сервиса-пакета <-> ID записи привязки сервиса-пакета к контракту
        Map<Long, Long> currentServiceIds = new HashMap<>();

        // получаем список текущих активных сервисов
        JSONObject subscriptionsResult = request(Method.get, "account_subscriptions")
                .setParam("account", tvipAccount)
                .execute()
                .getJsonObject();

        JSONArray subscriptionArray = subscriptionsResult.getJSONArray("data");

        for (int i = 0, size = subscriptionArray.length(); i < size; i++) {
            JSONObject serviceSubscription = subscriptionArray.getJSONObject(i);

            // id сервиса-пакета MW
            long id = serviceSubscription.getLong("id");
            long tariffId = serviceSubscription.getLong("tarif");

            currentServiceIds.put(tariffId, id);
        }

        logger.info("Current serviceIds: " + currentServiceIds.keySet() + ", need serviceIds: " + servicesToAdd);

        // удаляем те, что неактивны в биллинге, но есть в текущих
        for (Map.Entry<Long, Long> serviceId : currentServiceIds.entrySet()) {
            if (!servicesToAdd.contains(serviceId.getKey())) {
                logger.debug("delete subscription: " + serviceId.getKey());

                JSONObject result = request(Method.delete, "account_subscriptions")
                        .setId(serviceId.getValue())
                        .execute()
                        .getJsonObject();

                if (logger.isDebugEnabled()) {
                    logger.debug(result);
                }
            }
        }

        // добавляем те, что активны в биллинге, но в текущих - нет
        for (Long serviceId : servicesToAdd) {
            if (!currentServiceIds.containsKey(serviceId)) {
                logger.debug("add subscription: " + serviceId);

                JSONObject subscription = new JSONObject();
                subscription.put("account", tvipAccount);
                subscription.put("tarif", serviceId);
                subscription.put("start", dateTimeFormatter.format(ZonedDateTime.now()));
                try {
                    JSONObject result = request(Method.post, "account_subscriptions")
                            .setBody(subscription)
                            .execute()
                            .getJsonObject();

                    if (logger.isDebugEnabled()) {
                        logger.debug(result);
                    }
                } catch (JsonClientException ex) {
                    logger.info("Error in productsModifySyncFull: " + ex.getCause());
                }
            }
        }

        return null;
    }

    @Override
    public Object productsModify(final ProductOrderEvent e, final ServerContext ctx)
            throws Exception {
        logger.info("productsModify");

        final long tvipAccount = Utils.parseLong(e.getTvAccount().getDeviceAccountId());

        if (this.productSyncMode) {
            return productsModifySyncFull(e, tvipAccount, e.getTvAccount().getDeviceState() == TvAccount.STATE_ENABLE);
        }

        final Set<Long> servicesToRemove = new HashSet<>();
        final Set<Long> servicesToAdd = new HashSet<>();

        if (serviceMode) {
            for (ServiceSpec serviceSpec : e.getServiceSpecSetToRemove()) {
                servicesToRemove.add(Utils.parseLong(serviceSpec.getIdentifier().trim()));
            }

            for (ServiceSpec serviceSpec : e.getServiceSpecSetToAdd()) {
                servicesToAdd.add(Utils.parseLong(serviceSpec.getIdentifier().trim()));
            }
        } else {
            for (ProductSpec productSpec : e.getProductSpecSetToRemove()) {
                servicesToRemove.add(Utils.parseLong(productSpec.getIdentifier().trim()));
            }

            for (ProductSpec productSpec : e.getDeviceOptionProductSpecSetToDisable()) {
                servicesToRemove.add(Utils.parseLong(productSpec.getIdentifier().trim()));
            }

            for (ProductSpec productSpec : e.getProductSpecSetToAdd()) {
                servicesToAdd.add(Utils.parseLong(productSpec.getIdentifier().trim()));
            }

            for (ProductSpec productSpec : e.getDeviceOptionProductSpecSetToEnable()) {
                servicesToAdd.add(Utils.parseLong(productSpec.getIdentifier().trim()));
            }
        }

        servicesToRemove.remove(0L);
        servicesToAdd.remove(0L);

        if (servicesToRemove.size() > 0) {
            return productsModifySyncFull(e, tvipAccount, e.getTvAccount().getDeviceState() == TvAccount.STATE_ENABLE);
        }

        for (Long serviceId : servicesToAdd) {
            logger.debug("add subscription: " + serviceId);

            JSONObject subscription = new JSONObject();
            subscription.put("account", tvipAccount);
            subscription.put("tarif", serviceId);
            subscription.put("start", dateTimeFormatter.format(ZonedDateTime.now()));

            try {
                JSONObject result = request(Method.post, "account_subscriptions")
                        .setBody(subscription)
                        .execute()
                        .getJsonObject();

                if (logger.isDebugEnabled()) {
                    logger.debug(result);
                }
            } catch (JsonClientException ex) {
                logger.info("Error in productsModify: " + ex.getCause());
            }
        }

        return null;
    }

    protected void putPinMd5(JSONObject tvipAccount, final String password) {
        logger.info("putPinMd5");
        tvipAccount.put("pin_md5", Utils.getDigest(password, "UTF-8", "MD5").toLowerCase());
    }
}