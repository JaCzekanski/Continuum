-- Hammerspoon file

local CONTINUUM_PORT = 49646

local function continuumCallback(request, path, headers, body)
    print(headers["X-Remote-Addr"] .. ": " .. request .. " " .. path)

    if (request == "POST" and path == "/upload") then
        local filename = headers["filename"]
        local path = os.getenv("HOME") .. "/Desktop/" .. filename
        local file = io.open(path, "w")
        file:write(body)
        file:close()
        print(filename .. " saved")

        hs.notify.new(function(n)
            hs.execute("open " .. path)
        end, {
            title = filename .. " received",
            actionButtonTitle = "Open",
            hasActionButton = true,
            withdrawAfter = 0
        }):send()
        print("Notification sent")

        return "", 200, {}
    end

    return "", 404, {}
end

local continuumServer = hs.httpserver.new(false, false)
continuumServer:maxBodySize(32*1024*1024)
continuumServer:setCallback(continuumCallback)
continuumServer:setPort(CONTINUUM_PORT)
continuumServer:start()

local continuumService = hs.bonjour.service.new("", "_continuum._tcp", CONTINUUM_PORT)
continuumService:publish()
