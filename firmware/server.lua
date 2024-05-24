wifi.setmode(wifi.STATIONAP)
wifi.ap.config(cfg)

if srv~=nil then
    srv:close()
end
srv=net.createServer(net.TCP)

function trim(s)
    return (s:gsub("^%s*(.-)%s*$", "%1"))
end

local buffer = ''
srv:listen(1234, function(conn)
    conn:on("receive", function(sck,data)
        buffer = buffer .. data
        if string.len(buffer) <= 2 then return end

        local succ, jsonData = pcall(function()
            return sjson.decode(buffer)
        end)
        --TODO rename and encrypt files...
        if succ then
            if jsonData.action == nil then return end
            local action = jsonData.action
            print("got action : " .. action)
            --ping
            if action == 'ping' then
                sck:send('{"action":"pong"}')
            --Password actions
            elseif action == 'set_pass' then
                dofile(loader)('fileWrite')('pass.txt', jsonData.param)
                sck:send('{"action":"dc"}')
                tmr.delay(1000000)
                node.restart()
            --SSID
            elseif action == 'set_ssid' then
                dofile(loader)('fileWrite')('ssid.txt', jsonData.param)
                sck:send('{"action":"dc"}')
                tmr.delay(1000000)
                node.restart()
            --Data actions
            elseif action == 'set_data' then
                ok, json = pcall(sjson.encode, jsonData.param)
                if ok then
                    dofile(loader)('fileWrite')('data.txt', json)
                    sck:send('{"action":"set_data"}')
                else
                    print("failed to encode!")
                end

                local decoder = sjson.decoder()
                decoder:write(dofile(loader)('fileRead')('data.txt'))
                sched = decoder:result()["times"]
            elseif action == 'data' then
                local dataFile, err = file.open ('data.txt',"r")
                local chunk
                if dataFile ~= nil then
                    chunk = dataFile:read(256)
                    while chunk ~= nil do
                        sck:send(trim(chunk))
                        chunk = dataFile:read(256)
                        collectgarbage()
                    end
                    dataFile:close()
                    dataFile = nil
                    collectgarbage()
                end
            --RTC actions
            elseif action == 'set_time' then
                -- TODO set time
                print('time ', jsonData.hour, jsonData.minute)
                local id, device = 0, 0x68
                i2c.start(id)
                i2c.address(id, device, i2c.TRANSMITTER)
                i2c.write(id, 0)
                i2c.write(id, tonumber(0,16))   -- seconds
                i2c.write(id, tonumber(jsonData.minute,16))  -- minutes
                i2c.write(id, tonumber(jsonData.hour,16))  -- hours
                i2c.write(id, tonumber(jsonData.wday+1,16))   -- wday
                i2c.write(id, tonumber(1,16))  -- day
                i2c.write(id, tonumber(1,16))  -- month
                i2c.write(id, tonumber(1,16))  -- year
                i2c.stop(id)

                sck:send('{"action":"set_time"}')

            elseif action == 'time' then
                sck:send('{"action":"time","param1":"1582284539"}')
            end
        else
            print("Error parsing json, data is : " .. buffer)
        end
        buffer = ''
    end)
end)

return true
