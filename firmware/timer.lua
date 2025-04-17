local id, sda, scl, device = 0, 1, 2, 0x68
i2c.setup(id, sda, scl, i2c.SLOW)

local state = 1
local ledPin = 4
local outPin = 6
gpio.mode(ledPin,gpio.OUTPUT)
gpio.mode(outPin,gpio.OUTPUT)
gpio.write(ledPin, state)
gpio.write(outPin, 1-state)

local timer = tmr.create()
if not timer then
    print("Err: Unable to create timer!")
    return false
end

timer:alarm(20000, tmr.ALARM_AUTO, function()
    local day, wd, hr, min, sec
    
    if mockTimeConfig.enabled then
        -- Use mock time
        day = mockTimeConfig.time.d
        wd = mockTimeConfig.time.w
        hr = mockTimeConfig.time.h
        min = mockTimeConfig.time.m
        sec = mockTimeConfig.time.s
        
        -- Increment mock time
        sec = (sec or 0) + 20
        if sec >= 60 then
            sec = 0
            min = (min or 0) + 1
            if min >= 60 then
                min = 0
                hr = (hr or 0) + 1
                if hr >= 24 then
                    hr = 0
                    wd = wd + 1
                    day = day + 1
                    if wd > 6 then
                        wd = 0
                    end
                    if day > 31 then
                        day = 1
                    end
                end
            end
        end
        
        -- Update mock time config
        mockTimeConfig.time.s = sec
        mockTimeConfig.time.m = min
        mockTimeConfig.time.h = hr
        mockTimeConfig.time.w = wd
    else
        -- Use real RTC time
        i2c.start(id)
        i2c.address(id, device, i2c.TRANSMITTER)
        i2c.write(id, 0)
        i2c.stop(id)
        i2c.start(id)
        i2c.address(id, device, i2c.RECEIVER)
        local c = i2c.read(id, 7)  -- Read 7 bytes of data
        i2c.stop(id)

        if c then
            wd = tonumber(string.format("%X",string.byte(c, 4))) or 0
            hr = tonumber(string.format("%X",string.byte(c, 3))) or 0
            min = tonumber(string.format("%X",string.byte(c, 2))) or 0
            sec = tonumber(string.format("%X",string.byte(c, 1))) or 0
            day = tonumber(string.format("%X",string.byte(c, 5))) or 0
        else
            print("Error: Failed to read RTC time")
            return
        end
    end
    
    print(string.format("Time: day=%d wd=%d hr=%d min=%d sec=%d", day or 0, wd or 0, hr or 0, min or 0, sec or 0))

    local active = 0
    local dailyScheduleActive = false

    -- Check daily schedules first (higher priority)
    if sched ~= nil then
        for i, elem in ipairs(sched) do
            local startT = elem.st
            local endT = elem.et
            local currentTime = hr * 60 + min
            local startTime = startT.h * 60 + startT.m
            local endTime = endT.h * 60 + endT.m
            local scheduleType = elem.type or "daily"

            if scheduleType == "daily" then
                -- For daily schedules, we're active if current time is between start and end
                if currentTime >= startTime and currentTime <= endTime then
                    dailyScheduleActive = true
                    active = 1
                end
            elseif scheduleType == "weekly" then
                -- For weekly schedules, check if current day is between start and end days
                if startT.w ~= endT.w then
                    -- Cross-week schedule
                    if wd == startT.w and currentTime >= startTime then
                        dailyScheduleActive = true
                        active = 1
                    elseif wd == endT.w and currentTime <= endTime then
                        dailyScheduleActive = true
                        active = 1
                    elseif (wd > startT.w and wd < endT.w) or
                           (startT.w > endT.w and (wd > startT.w or wd < endT.w)) then
                        dailyScheduleActive = true
                        active = 1
                    end
                else
                    -- Same day schedule
                    if wd == startT.w and currentTime >= startTime and currentTime <= endTime then
                        dailyScheduleActive = true
                        active = 1
                    end
                end
            elseif scheduleType == "monthly" then
                -- For monthly schedules, check if current day of month is between start and end days
                local currentDay = day
                if startT.d ~= endT.d then
                    -- Cross-month schedule
                    if currentDay == startT.d and currentTime >= startTime then
                        dailyScheduleActive = true
                        active = 1
                    elseif currentDay == endT.d and currentTime <= endTime then
                        dailyScheduleActive = true
                        active = 1
                    elseif (currentDay > startT.d and currentDay < endT.d) or
                           (startT.d > endT.d and (currentDay > startT.d or currentDay < endT.d)) then
                        dailyScheduleActive = true
                        active = 1
                    end
                else
                    -- Same day schedule
                    if currentDay == startT.d and currentTime >= startTime and currentTime <= endTime then
                        dailyScheduleActive = true
                        active = 1
                    end
                end
            end
        end
    end

    if 1-active ~= state then
        state = 1-active
        gpio.write(ledPin, state)
        gpio.write(outPin, active)
        -- if state == 0 led is on and output is active
        -- print (wd, hr, min, 'state changed to ', state)
    end
end)

return true
