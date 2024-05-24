uart.setup(0, 115200, 8, uart.PARITY_NONE, uart.STOPBITS_1, 0)

local buffer = ''
uart.on("data", "\r", function(data)
    buffer = buffer .. data
    if string.len(buffer) <= 2 then return end
    local succ, jsonData = pcall(function()
        return sjson.decode(buffer)
    end)
    if succ then
        if jsonData.action == nil then return end
        local action = jsonData.action
        if action == 'time' then
            --local tm = rtctime.epoch2cal(1582284539)
            --uart.write(0, string.format("%04d/%02d/%02d %02d:%02d:%02d", tm["year"], tm["mon"], tm["day"], tm["hour"], tm["min"], tm["sec"]))
        elseif action == 'wifi_off' then
            wifi.setmode(wifi.NULLMODE)
            uart.write(0, "ok;")
        elseif action == 'wifi_on' then
            if not dofile(loader)('config') then 
                uart.write(0, "err;")
                node.restart() 
            end
            if not dofile(loader)('server') then 
                uart.write(0, "err;")
                node.restart() 
            end
            uart.write(0, "ok;")
        elseif action == 'quit' then
            uart.on("data")
            uart.write(0, "ok;")
        end
    else
        print("Error parsing json, data is : " .. buffer)
    end
    buffer = ''
end, 0)

return true
