*TruffleSqueak-Core-members
interopGetMembers: includeInternal
	| currentClass selectors |
	currentClass := self class.
	selectors := OrderedCollection new.
	[ currentClass isNil ] whileFalse: [
		selectors addAll: currentClass selectors.
		currentClass := currentClass superclass ].
	^ selectors collect: [ :ea | self class interopSelectorToMember: ea ]