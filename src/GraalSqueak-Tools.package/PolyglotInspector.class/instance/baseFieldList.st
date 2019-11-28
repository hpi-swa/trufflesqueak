overrides
baseFieldList
	"Answer an Array consisting of 'self'
	and the members of the inspected object."

	^ (Array with: 'self' with: 'all inst vars'),
		((Interop hasMembers: object)
			ifTrue: [ (Interop getMembers: object) sorted ]
			ifFalse: [ #() ])