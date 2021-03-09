dispatch
memberToSelector: aMember for: anObject arguments: arguments
	| selectorName  currentClass |
	selectorName := (aMember asString copyReplaceAll: '_' with: ':'), (
		arguments ifEmpty: [ '' ] ifNotEmpty: [ ':' ]).
	currentClass := anObject class.
	[ currentClass isNil ] whileFalse: [
		currentClass selectors
			detect: [ :ea | ea = selectorName ]
			ifFound: [ :s | ^ s ].
		currentClass := currentClass superclass ].
	^ nil
