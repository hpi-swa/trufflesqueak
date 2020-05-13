code generation
polyglotImport
	| name code |
	self activeEditArea fileEntry ifNil: [UIManager inform: 'No file selected'. ^ self].
	self currentLanguageInfo polyglotId ifNil: [UIManager inform: 'No language recognized'. ^ self].
	
	name := UIManager default
		chooseFromOrAddTo: self exportedVariables
		lines: OrderedCollection new
		title: 'Choose the name of the variable to import'.
	(name isNil or: [name isEmpty]) ifTrue: [^ self].
	code := self currentLanguageInfo ifNil: [^ self] ifNotNil: [:info | info codeForPolyglotImport: name].
	self activeEditArea replaceSelectionWith: code.
	self activeEditArea ensurePolyglotIsImported