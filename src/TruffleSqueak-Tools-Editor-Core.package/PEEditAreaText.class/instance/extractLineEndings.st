code actions
extractLineEndings

	lineEndingsSymbol := PELineEndingManager detectLineEndings: self unsavedCode.
	lineEndingsSymbol = #unknown ifTrue: [lineEndingsSymbol := self languageInfo 
		ifNotNil: [:info | PELineEndingManager stringToId: info defaultLineEndings] 
		ifNil: [#lf]].
	(#(cr lf crlf) includes: lineEndingsSymbol)
		ifTrue: [self lineEndings: (String perform: lineEndingsSymbol)].
	
	self changed: #lineEndings.
	self triggerEvent: #lineEndingsChanged