<?PHP
$files = scandir("records/");
printf( "%d", count($files) - 2);
?>
