accessing
icon
	^ (Interop hasLanguage: self object)
		ifTrue: [ Polyglot iconFor: (Interop getLanguageInfo: self object) getId asString ]
		ifFalse: [ ToolIcons iconNamed: #blank ]