--Wifi SSID
local ssid = dofile(loader)('fileRead')('ssid.txt')
if ssid == '' then
    ssid = 'ESP8266_AP'
end

--Wifi Password
--local password = encoder.toBase64(ssid)
--password = password:gsub("%=", "")
local password = dofile(loader)('fileRead')('pass.txt')
if password == '' then
    password = 'AbCdEf01'  -- Default password if none is set
end

--Wifi Config
cfg={}
cfg.ssid=ssid
cfg.pwd=password

schd = nil
local decoder = sjson.decoder()
local data = dofile(loader)('fileRead')('data.txt')
if data ~= '' then
    decoder:write(data)
    sched = decoder:result()["t"]
end

--Debug
print('Wifi ssid: ' .. ssid .. ' pass: ' .. (password or 'not set'))

-- Mock time configuration
mockTimeConfig = {
    enabled = false,
    time = {
        sec = 0,
        min = 0,
        hr = 0,
        wd = 1,
        day = 1,
        month = 1,
        year = 1
    }
}

-- Load mock time config if exists
local mockConfigData = dofile(loader)('fileRead')('mock_time.txt')
if mockConfigData ~= '' then
    local decoder = sjson.decoder()
    decoder:write(mockConfigData)
    mockTimeConfig = decoder:result()
end

return true
