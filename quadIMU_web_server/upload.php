<?PHP
$file_name = $_POST['getFileName'];
$data = $_POST['getData'];
$myfile = fopen("records/$file_name", "a") or die("Unable to open file!");
fwrite($myfile, $data);
fclose($myfile);
$length = filesize("records/$file_name");
echo $length;
?>
