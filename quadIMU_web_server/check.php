<?PHP
$file_name = $_POST["getFileName"];
if(is_file("records/$file_name")){
	$status="true";
} else {
	$status="false";
}
echo $status;
?>
