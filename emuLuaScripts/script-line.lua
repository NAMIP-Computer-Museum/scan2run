-- Lua script (trivial) for listing injection based spit on line
-- Author christophe Ponsard - 2020
-- usage: launch using command line -console -autoboot_script C:\DEV\NAM-IP-WEB\DAI\script-line.lua
-- launch emulator
-- type inject(<path_to_file_to_inject>) to inject on a line basis with a normal CR (\n) at the end
-- alternatively: inject_more(<path_to_file_to_inject>,"\n\n","\n\n") can be use to extra characters before/after the line (here CR)
-- use F10 to accelerate process

function inject(path)
  f=io.open(path)
  for line in f:lines() do emu.keypost(line.."\n") end
end

function inject_more(path, before, after)
  f=io.open(path)
  for line in f:lines() do emu.keypost(before..line..after) end
end