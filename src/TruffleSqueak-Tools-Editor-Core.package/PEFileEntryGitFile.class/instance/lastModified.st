accessing
lastModified


	^ self withFSDo: [:fs | | entry |
		entry := (fs workingDirectory / self treeEntry entryName).
		self flag: #todo. "Modification time is always 0. We would need to search the git commit that last modified/created this file". 
		^ DateAndTime now]