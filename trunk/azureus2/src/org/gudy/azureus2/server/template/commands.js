var mOvrClass='';
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
        case 0: document.Add.submit()
        case 1: document.Add.reset()
    }
}
function eTorrentInfo(hash) {
    document.TorrentInfo.hash.value=hash
    document.TorrentInfo.submit()
}
