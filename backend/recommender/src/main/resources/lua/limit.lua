-- 固定窗口计数器限流算法
-- KEYS[1]: 限流的key
-- ARGV[1]: 限流次数（整数）
-- ARGV[2]: 限流时间窗口（秒，整数）
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])

-- 参数检查
if limit == nil or window == nil then
    return -1
end
-- 获取当前计数
local current = redis.call('GET', key)

if current then
    -- 如果存在，转换为数字
    local count = tonumber(current)
    -- 如果转换失败，说明存储的不是数字，则删除并重新初始化
    if count == nil then
        redis.call('DEL', key)
        redis.call('SET', key, 1, 'EX', window)
        return 1
    end

    -- 判断是否超过限制
    if count >= limit then
        -- 超过限制，返回当前计数+1，这样Java代码中判断>limit就会触发限流
        return count + 1
    else
        -- 未超过限制，递增计数
        local newCount = redis.call('INCR', key)
        -- 如果newCount是1，说明是刚刚创建的（可能之前已经过期了），需要设置过期时间
        if newCount == 1 then
            redis.call('EXPIRE', key, window)
        end
        return newCount
    end
else
    -- 不存在，初始化
    redis.call('SET', key, 1, 'EX', window)
    return 1
end