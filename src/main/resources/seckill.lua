-- 从 ARGV 中获取 voucherId 和 userId
local voucherId = ARGV[1]
local userId = ARGV[2]

-- 生成库存键和订单键
local stockKey = 'seckill:stock:'.. voucherId
local orderKey = 'seckill:order:'.. voucherId

-- 判断库存
local stock = tonumber(redis.call('get', stockKey))
if (stock == nil or stock <= 0) then
    -- 库存不足
    return 1
end

-- 判断用户是否下单
if (redis.call('sismember', orderKey, userId) == 1) then
    -- 存在说明重复下单
    return 2
end

-- 扣库存
redis.call('decrby', stockKey, 1)

-- 下单
redis.call('sadd', orderKey, userId)

return 0