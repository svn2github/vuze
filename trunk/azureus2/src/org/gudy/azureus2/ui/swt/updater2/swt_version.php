<?php
	$latestVersion = 3052;

	if(! isset($platform) || $platform == '')
		$platform 	= @$_GET['platform'];
	
	if(! isset($platform) || $platform == '')
		exit();
	
	echo($latestVersion . "\n");

	if($platform == "win32") { ?>
http://mirror.tiscali.dk/eclipse/downloads/drops/S-3.0M9-200405211200/swt-3.0M8-win32.zip
<?php } else if($platform == "motif") { ?>
http://mirror.tiscali.dk/eclipse/downloads/drops/S-3.0M9-200405211200/swt-3.0M8-linux-motif.zip
<?php } else if($platform == "gtk") { ?>
http://mirror.tiscali.dk/eclipse/downloads/drops/S-3.0M9-200405211200/swt-3.0M8-linux-gtk.zip
<?php } else if($platform == "carbon") { ?>
http://192.168.0.101/project1/swt-3.0M9-macosx-carbon.zip
http://mirror.tiscali.dk/eclipse/downloads/drops/S-3.0M9-200405211200/swt-3.0M9-macosx-carbon.zip
<?php } ?>