dispatch
memberToSelector: aMember for: anObject
	| selectorName selectorNameVariant currentClass |
	selectorName := aMember asString copyReplaceAll: '_' with: ':'.
	selectorNameVariant := selectorName, ':'.
	currentClass := anObject class.
	[ currentClass isNil ] whileFalse: [
		currentClass selectors
			detect: [ :ea | ea = selectorName or: [ ea = selectorNameVariant ] ]
			ifFound: [ :s | ^ s ].
		currentClass := currentClass superclass ].
	^ nil
