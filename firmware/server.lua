-- Memory monitoring (disabled to save memory)
-- local function printMemory()
--     print("Heap: " .. node.heap() .. " bytes")
-- end
-- tmr.create():alarm(30000, tmr.ALARM_AUTO, printMemory)

-- Protected server initialization
local function initServer()
    wifi.setmode(wifi.SOFTAP)
    wifi.ap.config({
        ssid=cfg.ssid,
        auth=wifi.WPA2_PSK,
        pwd=cfg.pwd
    })

    -- Close any existing servers
    if srv ~= nil then
        srv:close()
        srv = nil
    end

    if httpServer ~= nil then
        httpServer:close()
        httpServer = nil
    end

    -- Reuse these tables to avoid creating new ones
    local responseTable = {
        a = "", -- action
        s = false, -- success
        m = nil -- message
    }

    local scheduleTable = {
        t = {} -- times
    }

    -- Short function names to save memory
    local function t(s) return (s:gsub("^%s*(.-)%s*$", "%1")) end

    local function sr(sck, a, s, m)
        responseTable.a = a
        responseTable.s = s
        responseTable.m = m
        sck:send("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n" .. sjson.encode(responseTable))
        collectgarbage()
    end

    local function ls()
        local d = dofile(loader)('fileRead')('data.txt')
        if #d > 0 then
            local dec = sjson.decoder()
            dec:write(d)
            local r = dec:result()
            if not r.a then r.a = scheduleTable.a end
            return r
        end
        return scheduleTable
    end

    local function ss(s)
        local ok, j = pcall(sjson.encode, s)
        if ok then
            dofile(loader)('fileWrite')('data.txt', j)
            collectgarbage()
            return true
        end
        return false
    end

    -- Create HTTP server with minimal memory footprint
    httpServer = net.createServer(net.TCP, 1) -- Only allow 1 connection at a time
    if not httpServer then
        print("Error: Failed to create HTTP server")
        return false
    end
    
    local fileSent = false
    local sendingFile = false
    local currentFile = nil
    
    local function sendNextChunk(sck)
        if not fileSent and currentFile then
            local chunk = currentFile.read(128) -- Small chunk size
            if chunk then
                sck:send(chunk)
            else
                currentFile:close()
                fileSent = true
                sendingFile = false
                currentFile = nil
            end
            collectgarbage()
        end
    end

    httpServer:listen(8080, function(conn)
        local req = ""
        conn:on("receive", function(sck, data)
            req = req .. data
            if string.find(req, "\r\n\r\n") then
                -- Parse request
                local method, path = string.match(req, "([A-Z]+) ([^ ]+) HTTP")
                local body = string.match(req, "\r\n\r\n(.*)$")
                -- print(method, path, body)

                -- Handle API requests
                if method == "POST" and path:match("^/api/") then
                    local action = path:match("^/api/([^/]+)")
                    local succ, jd = pcall(sjson.decode, body)
                    
                    if succ then
                        if action == "ping" then
                            sr(sck, "pong", true, "Pong!")
                        elseif action == "set_wifi" then
                            dofile(loader)('fileWrite')('ssid.txt', jd.ssid)
                            dofile(loader)('fileWrite')('pass.txt', jd.pass)
                            sr(sck, "set_wifi", true, "WiFi configuration updated")
                            tmr.delay(1000000)
                            node.restart()
                        elseif action == "set_day" then
                            local s = ls()
                            if not s.t then s.t = {} end
                            
                            -- Check if all time components are zero (unset request)
                            local isUnset = jd.st.h == 0 and jd.st.m == 0 and jd.st.w == 0 and jd.st.d == 0 and
                                          jd.et.h == 0 and jd.et.m == 0 and jd.et.w == 0 and jd.et.d == 0
                            
                            if isUnset then
                                -- Remove the schedule with matching ID
                                local nt = {}
                                for i, d in ipairs(s.t) do
                                    if d.id ~= jd.id then
                                        table.insert(nt, d)
                                    end
                                end
                                s.t = nt
                            else
                                -- Generate or use provided ID
                                jd.id = jd.id or tostring(os.time()) .. math.random(1000)
                                
                                -- Update or insert schedule
                                local u = false
                                for i, d in ipairs(s.t) do
                                    if d.id == jd.id then
                                        s.t[i] = jd
                                        u = true
                                        break
                                    end
                                end
                                
                                if not u then
                                    table.insert(s.t, jd)
                                end
                            end
                            
                            if ss(s) then
                                sr(sck, "set_day", true, "Schedule updated")
                                sched = s.t
                            else
                                sr(sck, "set_day", false, "Failed to save schedule")
                            end
                        elseif action == "set_time" then
                            if mockTimeConfig.enabled then
                                mockTimeConfig.time = {
                                    s = 0,
                                    m = jd.m,
                                    h = jd.h,
                                    w = jd.w,
                                    d = jd.d,
                                    mo = jd.mo,
                                    y = 1
                                }
                                local ok, j = pcall(sjson.encode, mockTimeConfig)
                                if ok then
                                    dofile(loader)('fileWrite')('mock_time.txt', j)
                                    sr(sck, "set_time", true, "Time updated (mock)")
                                else
                                    sr(sck, "set_time", false, "Failed to save mock time")
                                end
                            else
                                local id, dev = 0, 0x68
                                i2c.start(id)
                                i2c.address(id, dev, i2c.TRANSMITTER)
                                i2c.write(id, 0)
                                i2c.write(id, tonumber(0,16))
                                i2c.write(id, tonumber(jd.m,16))
                                i2c.write(id, tonumber(jd.h,16))
                                i2c.write(id, tonumber(jd.w+1,16))
                                i2c.write(id, tonumber(jd.d or 1,16))
                                i2c.write(id, tonumber(jd.mo or 1,16))
                                i2c.write(id, tonumber(1,16))
                                i2c.stop(id)
                                sr(sck, "set_time", true, "Time updated (RTC)")
                            end
                        else
                            sr(sck, "error", false, "Unknown action: " .. action)
                        end
                    else
                        sr(sck, "error", false, "Invalid JSON")
                    end
                elseif method == "GET" and path:match("^/api/") then
                    local action = path:match("^/api/([^/]+)")
                    
                    if action == "get_schedule" then
                        local s = ls()
                        sr(sck, "get_schedule", true, s)
                    elseif action == "get_time" then
                        local td
                        if mockTimeConfig.enabled then
                            td = {
                                s = mockTimeConfig.time.s,
                                m = mockTimeConfig.time.m,
                                h = mockTimeConfig.time.h,
                                w = mockTimeConfig.time.w,
                                d = mockTimeConfig.time.d,
                                mo = mockTimeConfig.time.mo,
                                y = mockTimeConfig.time.y
                            }
                        else
                            local id, dev = 0, 0x68
                            i2c.start(id)
                            i2c.address(id, dev, i2c.TRANSMITTER)
                            i2c.write(id, 0)
                            i2c.stop(id)
                            i2c.start(id)
                            i2c.address(id, dev, i2c.RECEIVER)
                            local c = i2c.read(id, 7)
                            i2c.stop(id)
                            
                            td = {
                                s = tonumber(string.format("%X",string.byte(c, 1))),
                                m = tonumber(string.format("%X",string.byte(c, 2))),
                                h = tonumber(string.format("%X",string.byte(c, 3))),
                                w = tonumber(string.format("%X",string.byte(c, 4))),
                                d = tonumber(string.format("%X",string.byte(c, 5))),
                                mo = tonumber(string.format("%X",string.byte(c, 6))),
                                y = tonumber(string.format("%X",string.byte(c, 7)))
                            }
                        end
                        sr(sck, "get_time", true, td)
                    else
                        sr(sck, "error", false, "Unknown action: " .. action)
                    end
                elseif method == "GET" then
                    if path == "/" then
                        currentFile = file.open("web_interface.html", "r")
                        if currentFile then
                            sck:send("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n")
                            sendingFile = true
                            sendNextChunk(sck)
                        else
                            sck:send("HTTP/1.1 404 Not Found\r\n\r\n")
                        end
                    else
                        sck:send("HTTP/1.1 404 Not Found\r\n\r\n")
                    end
                else
                    sck:send("HTTP/1.1 405 Method Not Allowed\r\n\r\n")
                end
                req = ""
                collectgarbage()
            end
        end)
        
        conn:on("sent", function(sck)
            if sendingFile then
                if not fileSent then
                    sendNextChunk(sck)
                else
                    sck:close()
                end
            else
                sck:close()
            end
            collectgarbage()
        end)
        
        conn:on("disconnection", function(sck)
            if sendingFile and currentFile then
                currentFile:close()
                currentFile = nil
                sendingFile = false
                fileSent = false
            end
            collectgarbage()
        end)
    end)

    return true
end

-- Run server initialization with error protection
local success, err = pcall(initServer)
if not success then
    print("Server initialization failed with error:")
    print(err)
    print("Stack trace:")
    print(debug.traceback())
    return false
end

return true