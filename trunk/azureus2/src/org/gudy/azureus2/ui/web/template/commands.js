var mOvrClass='';
document.captureEvents(Event.KEYPRESS);
document.onkeypress = eKeyPress;
function eKeyPress(e) {
    var keyChar = String.fromCharCode(e.which);
    if (keyChar == 'T' || keyChar == 't')
        eCommand("torrentmain");
    if (keyChar == 'L' || keyChar == 'l')
        eCommand("log");
    if (keyChar == 'O' || keyChar == 'o')
        eCommand("config");
    if (keyChar == 'K' || keyChar == 'k')
        eCommand("exit", "Exit");
    if (document.Torrent != undefined) {
        if (keyChar == 'P' || keyChar == 'p')
            eTorrent(0);
        if (keyChar == 'D' || keyChar == 'd')
            eTorrent(1);
        if (keyChar == 'C' || keyChar == 'c')
            eTorrent(2);
    }
}
function mOvr(src,clrOver) {
 mOvrClass = src.className;
 src.className = mOvrClass + ' ' + clrOver; 
}
function mOut(src) {
 src.className=mOvrClass;
}
function eCommand(action, command, target) {
    if (action != undefined)
        document.Command.action = action
    if (command != undefined) {
        if (command == "Exit") {
            if (confirm("Are you sure?"))
                document.Command.command.value = command
            else {
                document.Command.command.value = ""
                return
            }
        } else
            document.Command.command.value = command
    } else
        document.Command.command.value = ""
    if (target != undefined)
        document.Command.target = target
    document.Command.submit()
}
function eAdd(action) {
    switch(action) {
        case 0: {
            document.Add.submit()
            break
          }
        case 1: document.Add.reset()
    }
}
function eTorrent(action) {
    switch(action) {
        case 0: {
            document.Torrent.subcommand.value="Pause"
            document.Torrent.submit()
            break
          }
        case 1: {
            document.Torrent.subcommand.value="Start"
            document.Torrent.submit()
            break
          }
        case 2: if (confirm("Are you sure?")) {
            document.Torrent.subcommand.value="Cancel"
            document.Torrent.submit()
            break
          }
    }
}
            
function eTorrentInfo(hash) {
    document.TorrentInfo.hash.value=hash
    document.TorrentInfo.submit()
}
