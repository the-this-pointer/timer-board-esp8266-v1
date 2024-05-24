return function(filename)
    if file.open(filename .. '.lua') then
        file.close()
        print('Compiling: ', filename)
        node.compile(filename .. '.lua')
        file.remove(filename .. '.lua')
        collectgarbage()
    end

    if file.open(filename .. '.lc') then --TODO: should be '.lc'
        file.close()
        --print('Loading: ', filename)
        return dofile(filename .. '.lc')
    else
        print('Failed to load: ', filename)
        return false
    end
end
