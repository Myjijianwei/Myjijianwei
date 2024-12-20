package com.hmdp.utils;

public interface ILock {

    /**
     * changshihuoqusuo
     * @param timeoutSec
     * @return
     */
    boolean tryLock(long timeoutSec);

    /**
     * shifangsuo
     */
    void unlock();
}
