<?php
	$latestVersion = 3068;

	if(! isset($platform) || $platform == '')
		$platform 	= @$_GET['platform'];
	
	if(! isset($platform) || $platform == '')
		exit();
	
	echo($latestVersion . "\n");

	if($platform == "win32") { ?>
http://download2.eclipse.org/downloads/drops/S-3.0M8-200403261517/swt-3.0M8-win32.zip
<?php } else if($platform == "motif") { ?>
http://download2.eclipse.org/downloads/drops/S-3.0M8-200403261517/swt-3.0M8-linux-motif.zip
<?php } else if($platform == "gtk") { ?>
http://download2.eclipse.org/downloads/drops/S-3.0M8-200403261517/swt-3.0M8-linux-gtk.zip
<?php } else if($platform == "carbon") { ?>
http://download2.eclipse.org/downloads/drops/S-3.0M8-200403261517/swt-3.0M8-macosx-carbon.zip
<?php } ?>