package com.hmdp;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
public class RedissonTest {
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RedissonClient redissonClient2;
    @Autowired
    private RedissonClient redissonClient3;

    private RLock lock;

    @BeforeEach
    void setUp() {
        RLock lock1 = redissonClient.getLock("order");
        RLock lock2 = redissonClient2.getLock("order");
        RLock lock3 = redissonClient3.getLock("order");

        //连锁
        lock=redissonClient.getMultiLock(lock1, lock2, lock3);
    }

    @Test
    void method1() throws InterruptedException {
        boolean isLock = lock.tryLock(1L, TimeUnit.SECONDS);
        if (!isLock) {
            log.error("获取锁失败。。。1");
            return;
        }
        try {
            log.info("获取锁成功");
            // 进行 method2 的操作，而不使用嵌套锁
            method2();
            log.info("开始执行业务");
        } finally {
            log.warn("准备释放锁、、、1");
            lock.unlock();
        }
    }

    void method2() throws InterruptedException {
        boolean isLock = lock.tryLock(1L, TimeUnit.SECONDS);
        if (!isLock) {
            log.error("获取锁失败。。。2");
            return;
        }
        try {
            log.info("获取锁成功。。。2");
            log.info("开始执行业务。。。2");
        } finally {
            log.warn("准备释放锁、、、2");
            lock.unlock();
        }
    }
}
