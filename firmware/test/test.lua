json  = require "json"

local buffer = ''
local unsucCounter = 0

function receiveCB(data)
  buffer = buffer .. data
  print("New data is : " .. data)
  print("New buffer is : " .. buffer)
  if string.len(buffer) <= 2 then return end
  
  local succ, jsonData = pcall(function()
    return json.decode(buffer)
  end)
  
  if succ then
    buffer = ''
    unsucCounter = 0
    print("Json is OK, data is : " .. jsonData['a'])
  else
    print("Error parsing json, data is : " .. buffer)
    unsucCounter = unsucCounter + 1
    if unsucCounter >= 3 then
      unsucCounter = 0
      buffer = ''
    end
  end
end

print(" ======= Testing receive cb 1")
receiveCB("{\"a\":\"b")
receiveCB("\"}")

print(" ======= Testing receive cb 2")
receiveCB("{\"a\":\"b")
receiveCB("\"}")

print(" ======= Testing receive cb 3")
receiveCB("{\"a\":\"b")
receiveCB("{\"a\":\"b\"}")
