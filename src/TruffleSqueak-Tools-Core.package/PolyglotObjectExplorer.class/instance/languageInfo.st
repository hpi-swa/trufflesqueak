language
languageInfo
	^ languageInfo ifNil: [
		languageInfo := (Polyglot useAdaptiveLanguageSelection and: [ Interop hasLanguage: self object ])
			ifTrue: [ (Interop getLanguageInfo: self object) ]
			ifFalse: [ Polyglot defaultLanguageInfo ] ]