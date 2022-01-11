<?PHP
$num = $_POST['getNum'] + 2;
$files = scandir("records/");
printf( "%s",$files[$num]);
?>
