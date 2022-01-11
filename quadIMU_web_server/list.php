<?PHP
$files = scandir("records/");
for ($x =2; $x <count($files);$x++){
	printf("%s #%s\n",$files[$x],filesize("records/$files[$x]"));
}
?>
