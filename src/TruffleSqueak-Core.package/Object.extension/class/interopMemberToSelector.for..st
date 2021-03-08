*TruffleSqueak-Core
interopMemberToSelector: aMember for: arguments
	| selectorName  currentClass |
	selectorName := (aMember asString copyReplaceAll: '_' with: ':'), (
		arguments ifEmpty: [ '' ] ifNotEmpty: [ ':' ]).
	currentClass := self.
	[ currentClass isNil ] whileFalse: [
		currentClass selectors
			detect: [ :ea | ea = selectorName ]
			ifFound: [ :s | ^ s ].
		currentClass := currentClass superclass ].
	^ nil
