local id, sda, scl, device = 0, 1, 2, 0x68
i2c.setup(id, sda, scl, i2c.SLOW)

local state = 1
local ledPin = 4
local outPin = 6
gpio.mode(ledPin,gpio.OUTPUT)
gpio.mode(outPin,gpio.OUTPUT)
gpio.write(ledPin, state)
gpio.write(outPin, 1-state)

if not tmr.create():alarm(20000, tmr.ALARM_AUTO, function()
    i2c.start(id)
    i2c.address(id, device, i2c.TRANSMITTER)
    i2c.write(id, 0)
    i2c.stop(id)
    i2c.start(id)
    i2c.address(id, device, i2c.RECEIVER)
    c = i2c.read(id, 7)  -- Read 7 bytes of data
    i2c.stop(id)

    local wd = tonumber(string.format("%X",string.byte(c, 4)))
    local hr = tonumber(string.format("%X",string.byte(c, 3)))
    local min = tonumber(string.format("%X",string.byte(c, 2)))
    local sec = tonumber(string.format("%X",string.byte(c, 1)))
    print(" wd "..wd,
            " hr "..hr,
            "min "..min,
            "sec "..sec)
    --print("Day No "..wd)
    --print("day", string.format("%X",string.byte(c, 5)))
    --print("mnt", string.format("%X",string.byte(c, 6)))
    --print(" yr", string.format("%X",string.byte(c, 7)))

    local active = 0
    if sched ~= nil and sched ~= '' then
        for i, elem in ipairs(sched) do
            local day = elem['day'] + 1
            if day == wd then
                local startT = elem['startTime']
                local endT = elem['endTime']
                --print ('check time start', day, startT['hour'], startT['minute'], endT['hour'], endT['minute'])
                if startT['hour'] <= endT['hour'] then
                    if startT['hour'] < hr and endT['hour'] > hr then
                        active = 1
                    elseif startT['hour'] == hr and startT['hour'] == endT['hour'] then
                        if startT['minute'] <= min and endT['minute'] >= min then
                            active = 1
                        end
                    elseif startT['hour'] == hr then
                        if startT['minute'] <= min then
                            active = 1
                        end
                    elseif endT['hour'] == hr then
                        if endT['minute'] >= min then
                            active = 1
                        end
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
then
    print("Err: Unable to create timer!")
    return false
end

return true