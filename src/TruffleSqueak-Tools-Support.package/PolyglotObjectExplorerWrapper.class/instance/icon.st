accessing
icon
	^ (Interop hasLanguageId: self object)
		ifTrue: [ Polyglot iconFor: (Interop getLanguageId: self object) ]
		ifFalse: [ ToolIcons iconNamed: #blank ]