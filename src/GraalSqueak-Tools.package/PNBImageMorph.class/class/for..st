instance creation
for: aFileName
	^ self new
		fileName: aFileName;
		image: (Form fromFileNamed: aFileName)