menus
menuDelete
	
	| confirmed |
	confirmed := UIManager default confirm: 'Do you really want to delete "', self selectedEntry name, '"?'.
	confirmed ifTrue: [
		self selectedEntry delete.
		self changed: #rootDirectoryList]