-- Lua script for listing injection based on the frame rate
-- Author christophe Ponsard - 2020
-- usage: launch using command line -console -autoboot_script C:\DEV\NAM-IP-WEB\DAI\script-frame.lua
-- script will start to inject when emulator is started (first frames generated)
-- use F10 to accelerate process
-- use MOD variable to keep script sync with emulator rate 
-- (need to experiment, might not be a problem as there seem to be a cache but could depend on emulator)

MOD=8
basic_s="";
basic_l=0;

function readAll()
  local f=io.open("C:\\DEV\\NAM-IP-WEB\\DAI\\CPC_BOMB.BAS")
  basic_s = f:read("*all")
  basic_l = string.len(basic_s) 
end

function basic_load()
  readAll();
end

i=0;
j=0;
function basic_post()
  if ((i%MOD==0) and (j<=basic_l)) then
    local c=string.sub(basic_s,j,j);
--	if (c=="\n") then c="\n\n\n\n\n" end;
    emu.keypost(c);
	io.write(c);
	j=j+1;
  end
  i=i+1;
--  print("F:"..i)
end

emu.pause();
readAll();
emu.unpause();
emu.register_frame(basic_post)
