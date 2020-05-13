menus
menuCreateDirectory
	
	| name |
	name := UIManager default request: 'Directory Name'.
	name ifNotEmpty: [
		self selectedEntry createDirectory: name.
		self changed: #rootDirectoryList]