writing
box: aPCCodeBox referencedFromDirectory: directoryPath lineEndings: aString languageId: languageID

	| code languageInfo relativePath pathToEvaluate |
	aPCCodeBox language ifNil: [self error: 'You need to select the target language'].
	aPCCodeBox boxName ifEmpty: [self error: 'You need to give a name to this snippet.'].
	
	languageInfo := PELanguageInfo forId: languageID.
	
	aPCCodeBox fileEntry
		ifNil: [ | directoryName |
			"new code box; default to snippets directory relative to the surrounding file, if the surrounding file isn't already inside a snippets directory."
			directoryName := self baseNameOf: directoryPath.
			directoryName = 'snippets'
				ifTrue: [relativePath := './', aPCCodeBox fileName]
				ifFalse: [relativePath := './snippets/', aPCCodeBox fileName]]
		ifNotNil: ["existing code box; generate a relative path to the code box from the surrounding file"
			relativePath := self
				make: (aPCCodeBox fileEntry fullPath copyReplaceAll: '\' with: '/')
				relativeTo: (directoryPath copyReplaceAll: '\' with: '/')].
	
	code := WriteStream on: ''.
	code
		nextPutAll: (languageInfo codeForComment: self codeBoxBeginMarker, ':', relativePath); 
		nextPutAll: aString.
	
	aPCCodeBox importVariables do: [:each | code 
		nextPutAll: (languageInfo codeForPolyglotExportOf: each to: each);
		nextPutAll: languageInfo codeForStatementSeparator;
		nextPutAll: aString].
	
	
	PolyglotEditor useAbsolutePathsForSnippets
		ifTrue: [pathToEvaluate := directoryPath, '/', (relativePath copyFrom: 3 to: relativePath size)]
		ifFalse: [pathToEvaluate := relativePath].
	
	code
		nextPutAll: (languageInfo codeForPolyglotEvaluateFile: pathToEvaluate in: aPCCodeBox language);
		nextPutAll: languageInfo codeForStatementSeparator;
		nextPutAll: aString.
	
	aPCCodeBox exportVariables do: [:each | code 
		nextPutAll: (languageInfo codeForAssignmentOf: (languageInfo codeForPolyglotImport: each) to: each);
		nextPutAll: languageInfo codeForStatementSeparator;
		nextPutAll: aString].
	
	code
		nextPutAll: (languageInfo codeForComment: self codeBoxEndMarker).
	
	^ {code contents. relativePath. self generateInnerCodeForBox: aPCCodeBox}