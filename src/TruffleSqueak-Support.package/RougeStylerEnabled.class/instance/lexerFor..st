rouge
lexerFor: languageID
	| lexer |
	"Lexers are Ruby objects and are therefore replaced with nil when the image is saved."
	lexer := (lexers ifNil: [ lexers := Dictionary new]) at: languageID ifAbsent: [ nil ].
	lexer ifNil: [ lexer := lexers at: languageID put: (self newLexerFor: languageID) ].
	^ lexer