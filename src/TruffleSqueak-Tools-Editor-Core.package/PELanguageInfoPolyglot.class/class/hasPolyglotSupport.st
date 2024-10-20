accessing
hasPolyglotSupport

	"true when we are not in TruffleSqueak, otherwise only true if the language is installed"
	| polyglot |
	polyglot := Smalltalk classNamed: #Polyglot.
	^ polyglot isNil or: [(polyglot isPolyglotEvalAllowed: self polyglotId) not] or: [polyglot isPermitted: self polyglotId]