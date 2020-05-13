updating
update: aSymbol

	(labelSelector notNil and: [labelSelector == aSymbol]) 
		ifTrue: [^ self contents: (self model perform: labelSelector)].
	
	^ super update: aSymbol