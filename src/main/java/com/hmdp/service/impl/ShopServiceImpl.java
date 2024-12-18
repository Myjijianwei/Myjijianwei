package com.hmdp.service.impl;

import cn.hutool.cache.CacheUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CaCheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CaCheClient caCheClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        //更新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }

    @Override
    public Result queryById(Long id) {
       //缓存穿透
        Shop shop = caCheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,CACHE_SHOP_TTL,TimeUnit.MINUTES,this::getById);
        //互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);

        //逻辑过期解决缓存击穿
//        Shop shop = caCheClient.queryWithLogicalExpire(CACHE_SHOP_KEY,id, Shop.class,this::getById,20L,TimeUnit.SECONDS);
        if (shop == null) {
            return Result.fail(id+"店铺不存在");
        }
        return Result.ok(shop);
    }



//    private Shop queryWithMutex(Long id)  {
//        String shopKey= CACHE_SHOP_KEY + id;
//        //1、从redis查询缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
//        //判断是否存在
//        if(StrUtil.isNotBlank(shopJson)){
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//        //判断命中是否为空值
//        if(shopJson!=null){
//            return null;
//        }
//
//        //实现缓存重建
//        //获取互斥锁
//        String lockKey=LOCK_SHOP_KEY+id;
//        Shop shop = null;
//        try {
//            boolean isLock = tryLock(lockKey);
//            //判断是否获取成功
//            if(!isLock){
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//            //获取成功，根据id查库
//            shop = getById(id);
//            //模拟重建延时
//            Thread.sleep(200);
//            if(shop == null){
//                stringRedisTemplate.opsForValue().set(shopKey,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
//                return null;
//            }
//            stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            unlock(lockKey);
//        }
//        return shop;
//    }


    //解决缓存穿透
    public Shop queryWithPassThrough(Long id){
        String shopKey= CACHE_SHOP_KEY + id;
        //1、从redis查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        //判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //判断命中是否为空值
        if(shopJson!=null){
            return null;
        }

        Shop shop = getById(id);
        if(shop == null){
            stringRedisTemplate.opsForValue().set(shopKey,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }
        stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return shop;
    }
}
