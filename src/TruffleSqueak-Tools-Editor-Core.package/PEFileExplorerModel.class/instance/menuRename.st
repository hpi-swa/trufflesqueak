menus
menuRename
	
	| newName |
	newName := UIManager default request: 'Enter new name.' initialAnswer: self selectedEntry name.
	newName ifNotEmpty: [
		self selectedEntry rename: newName.
		self changed: #rootDirectoryList]