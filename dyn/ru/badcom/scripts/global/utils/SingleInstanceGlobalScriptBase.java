package ru.badcom.scripts.global.utils;

import ru.bitel.bgbilling.kernel.script.server.dev.GlobalScript;
import ru.bitel.bgbilling.kernel.script.server.dev.GlobalScriptBase;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.sql.ConnectionSet;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Semyon Koshechkin on 22.03.2018.
 * Абстрактный класс глобальных скриптов, блокирующий параллельное выполнение одного и того же скрипта несколько раз
 */
public abstract class SingleInstanceGlobalScriptBase extends GlobalScriptBase implements GlobalScript {

    private static final ConcurrentHashMap<Class<? extends SingleInstanceGlobalScriptBase>, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    //Метод для переопределения в конечных классах, использовать вместо execute
    public abstract void executeSingle (Setup setup, ConnectionSet connectionSet) throws Exception;

    @Override
    public final void execute(Setup setup, ConnectionSet connectionSet) throws Exception {
        lockMap.putIfAbsent(this.getClass(), new ReentrantLock());//Синхронно добавляем лок в мэпу, если ещё нет
        ReentrantLock lock = lockMap.get(this.getClass());//Если добавим удаление устаревших локов, то нужно добавлять тут ещё синхронизацию, чтобы lock не оказался null
        if(!lock.tryLock()) {
            error("Script "+this.getClass().getName()+" is already running!");
        }else{
            try {
                this.executeSingle(setup, connectionSet);
            }finally {
                lock.unlock();
            }
        }
    }
}