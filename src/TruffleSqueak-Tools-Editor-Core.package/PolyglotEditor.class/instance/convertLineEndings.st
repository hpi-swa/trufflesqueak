code generation
convertLineEndings

	| newLineEndings |
	newLineEndings := UIManager default 
		chooseFrom: (PELineEndingManager stringToIdDictionary values collect: #asUppercase) 
		values: PELineEndingManager stringToIdDictionary keys.
	newLineEndings ifNil: [^ self].
	self activeEditArea convertLineEndings: newLineEndings