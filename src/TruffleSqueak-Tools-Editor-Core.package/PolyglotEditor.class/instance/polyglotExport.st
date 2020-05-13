code generation
polyglotExport
	| name value code |
	self activeEditArea fileEntry ifNil: [UIManager inform: 'No file selected'. ^ self].
	self currentLanguageInfo polyglotId ifNil: [UIManager inform: 'No language recognized'. ^ self].
	
	name := UIManager default request: 'Choose the name of the variable to export'.
	name ifEmpty: [^ self].
	value := UIManager default request: ('Choose the value of ', name).
	value ifEmpty: [^ self].
	code := self currentLanguageInfo
		ifNil: [^ self]
		ifNotNil: [:info | info codeForPolyglotExportOf: value to: name].
	self activeEditArea replaceSelectionWith: code.
	self activeEditArea ensurePolyglotIsImported.
	self exportedVariables add: name