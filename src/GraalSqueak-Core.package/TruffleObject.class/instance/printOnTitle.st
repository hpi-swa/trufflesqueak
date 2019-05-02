printing
printOnTitle
	| arraySize memberSize properties |
	self isNull ifTrue: [ ^ 'ForeignNil' ].
	self isString ifTrue: [ ^ 'ForeignString[', self asString ,']' ].
	self fitsInLong ifTrue: [ ^ 'ForeignInteger' ].
	self fitsInDouble ifTrue: [ ^ 'ForeignFloat' ].
	properties := OrderedCollection new.
	arraySize := self hasArrayElements ifTrue: [self primitiveGetArraySize ] ifFalse: [ 0 ].
	memberSize := self hasMembers ifTrue: [ self getMembers size ] ifFalse: [ 0 ].
	arraySize > 0 ifTrue: [ properties add: 'arraySize=', arraySize ].
	memberSize > 0 ifTrue: [ properties add: 'memberSize=', memberSize ].
	self isInstantiable ifTrue: [ properties add: 'instantiable' ].
	self isExecutable ifTrue: [ properties add: 'executable' ].
	^ properties
		ifEmpty: [ 'ForeignObject' ]
		ifNotEmpty: [ 'ForeignObject[', (properties joinSeparatedBy: ','), ']' ]