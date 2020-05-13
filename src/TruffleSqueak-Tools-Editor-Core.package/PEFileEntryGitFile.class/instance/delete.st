file operations
delete

	self withFSDo: [:fs | fs delete: self treeEntry entryName]