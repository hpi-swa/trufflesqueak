code generation
polyglotEvaluate
	| evaluatedCode insertedCode id languageInfos |
	
	self activeEditArea fileEntry ifNil: [UIManager inform: 'No file selected'. ^ self].
	self currentLanguageInfo polyglotId ifNil: [UIManager inform: 'No language recognized'. ^ self].
	
	languageInfos := PELanguageInfo allWithPolyglotSupport.
	
	id := UIManager 
		default chooseFrom: (languageInfos collect: #languageName)
		values: (languageInfos collect: #polyglotId) 
		title: 'Choose the target language:'.
	evaluatedCode := UIManager default request: ('Type your code').
	evaluatedCode ifEmpty: [^ self].
	insertedCode := self currentLanguageInfo ifNil: [^ self] ifNotNil: [:info |
		(info codeForPolyglotEvaluate: evaluatedCode in: id)].
	self activeEditArea replaceSelectionWith: insertedCode.
	self activeEditArea ensurePolyglotIsImported