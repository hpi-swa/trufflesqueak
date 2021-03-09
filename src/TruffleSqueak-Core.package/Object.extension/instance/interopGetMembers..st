*TruffleSqueak-Core-members
interopGetMembers: includeInternal
	| selectors currentClass |
	selectors := includeInternal
		ifTrue: [ Set withAll: self class allInstVarNames ]
		ifFalse: [ Set new ].
	currentClass := self class.
	[ currentClass isNil ] whileFalse: [
		selectors addAll: (currentClass selectors collect: [ :ea | Interop selectorToMember: ea ]).
		currentClass := currentClass superclass ].
	^ selectors asArray