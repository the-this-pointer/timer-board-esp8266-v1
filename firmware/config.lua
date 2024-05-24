--Wifi SSID
local ssid = dofile(loader)('fileRead')('ssid.txt')
if ssid == '' then
    ssid = 'rkm1234'
end

--Wifi Password
--local password = encoder.toBase64(ssid)
--password = password:gsub("%=", "")
local password = dofile(loader)('fileRead')('pass.txt')

--Wifi Config
cfg={}
cfg.ssid=ssid
cfg.pwd=password

schd = nil
local decoder = sjson.decoder()
local data = dofile(loader)('fileRead')('data.txt')
if data ~= '' then
    decoder:write(data)
    sched = decoder:result()["times"]
end

--Debug
print('Wifi ssid: ' .. ssid .. ' pass: ' .. password)
return true
