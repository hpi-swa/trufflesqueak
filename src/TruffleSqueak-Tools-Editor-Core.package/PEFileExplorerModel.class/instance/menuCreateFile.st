menus
menuCreateFile
	
	| name |
	name := UIManager default request: 'File Name'.
	name ifNotEmpty: [
		self selectedEntry createFile: name.
		self changed: #rootDirectoryList]