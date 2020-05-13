accessing
fullPath

	^ self withFSDo: [:fs | 
		^ (fs repository repository workingDir / self treeEntry entryName) asString]