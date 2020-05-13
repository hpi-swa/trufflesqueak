accessing
hasPolyglotSupport

	"true when we are not in GraalSqueak, otherwise only true if the language is installed"
	| polyglot |
	polyglot := Smalltalk classNamed: #Polyglot.
	^ polyglot isNil or: [polyglot isPolyglotAccessAllowed not] or: [polyglot availableLanguages includes: self polyglotId]