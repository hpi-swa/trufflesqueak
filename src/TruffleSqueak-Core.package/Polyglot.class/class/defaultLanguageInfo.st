preferences
defaultLanguageInfo
	^ DefaultLanguageInfo ifNil: [ Polyglot availableLanguages first ]