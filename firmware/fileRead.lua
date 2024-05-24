return function (name)
    if file.exists(name) == false then
        print('File ' .. name .. ' doesn\'t exists...')
        return ''
    end

    local data = ''
    if file.open(name, 'r') then
        data = file.read('\n')
        file.close()
        collectgarbage()
    end

    return data
end
