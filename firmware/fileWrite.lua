return function (name, data)
    if file.open(name, "w") then
        file.write(data)
        file.close()
        collectgarbage()
        return true
    end

    return false
end
