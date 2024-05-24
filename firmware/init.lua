local outPin = 6
gpio.mode(outPin,gpio.OUTPUT)
gpio.write(outPin, 0)
print('Booting\r\n5s delay to allow for abort')
if file.open('load.lua') then
    file.close()
    print('Compiling: ', 'load.lua')
    node.compile('load.lua')
    file.remove('load.lua')
    collectgarbage()
end
loader = 'load.lc'

tmr.create():alarm(5000, tmr.ALARM_SINGLE, function()
    print('Booting...')
    if not dofile(loader)('config') then return end
    if not dofile(loader)('timer') then return end
    if not dofile(loader)('server') then return end

    print('Init successful')
    collectgarbage()
end)
