*TruffleSqueak-Core-members
interopGetMembers: includeInternal
	| currentClass selectors |
	currentClass := self class.
	selectors := includeInternal
		ifTrue: [ Set withAll: self class allInstVarNames ]
		ifFalse: [ Set new ].
	[ currentClass isNil ] whileFalse: [
		selectors addAll: (currentClass selectors collect: [ :ea | self class interopSelectorToMember: ea ]).
		currentClass := currentClass superclass ].
	^ selectors asArray