dispatch
memberToSelector: aMember for: anObject arguments: arguments
	| selector |
	selector := ((aMember asString copyReplaceAll: '_' with: ':'), (
		arguments ifEmpty: [ '' ] ifNotEmpty: [ ':' ])) asSymbol.
	^ (anObject class canUnderstand: selector)
		ifTrue: [ selector ] ifFalse: [ nil ]