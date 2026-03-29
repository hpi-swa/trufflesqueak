language
languageInfo
	^ languageInfo ifNil: [
		languageInfo := (Polyglot useAdaptiveLanguageSelection and: [ Interop hasLanguageId: self object ])
			ifTrue: [ (Interop getLanguageInfo: self object) ]
			ifFalse: [ Polyglot defaultLanguageInfo ] ]