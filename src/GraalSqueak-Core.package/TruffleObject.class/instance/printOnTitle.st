printing
printOnTitle
	| arraySize memberSize properties |
	self primitiveIsNull ifTrue: [ ^ 'ForeignNil' ].
	self primitiveIsString ifTrue: [ ^ 'ForeignString[', self asString ,']' ].
	self primitiveFitsInLong ifTrue: [ ^ 'ForeignInteger' ].
	self primitiveFitsInDouble ifTrue: [ ^ 'ForeignFloat' ].
	properties := OrderedCollection new.
	arraySize := self primitiveHasArrayElements ifTrue: [self primitiveGetArraySize ] ifFalse: [ 0 ].
	memberSize := self primitiveHasMembers ifTrue: [ self primitiveGetMemberSize ] ifFalse: [ 0 ].
	arraySize > 0 ifTrue: [ properties add: 'arraySize=', arraySize ].
	memberSize > 0 ifTrue: [ properties add: 'memberSize=', memberSize ].
	self primitiveIsInstantiable ifTrue: [ properties add: 'instantiable' ].
	self primitiveIsExecutable ifTrue: [ properties add: 'executable' ].
	^ properties
		ifEmpty: [ 'ForeignObject' ]
		ifNotEmpty: [ 'ForeignObject[', (properties joinSeparatedBy: ','), ']' ]