primitives members
getMemberSize: anObject includeInternal: aBoolean
	<primitive: 'primitiveGetMemberSize' module: 'PolyglotPlugin'>
	^ (self hasMembers: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ (self getMembers: anObject includeInternal: aBoolean) size ] ]
		ifFalse: [ self error: 'Object does not have members' ]