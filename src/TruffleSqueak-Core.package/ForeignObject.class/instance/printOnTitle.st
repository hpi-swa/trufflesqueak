printing
printOnTitle
	| arraySize memberSize properties name |
	(Interop isNull: self) ifTrue: [ ^ 'ForeignNil' ].
	(Interop isString: self) ifTrue: [ ^ 'ForeignString[', self asString, ']' ].
	(Interop fitsInLong: self) ifTrue: [ ^ 'ForeignInteger[', (Interop asLong: self), ']' ].
	(Interop fitsInDouble: self) ifTrue: [ ^ 'ForeignFloat[', (Interop asDouble: self), ']' ].
	properties := OrderedCollection new.
	arraySize := (Interop hasArrayElements: self)
		ifTrue: [ Interop getArraySize: self ] ifFalse: [ 0 ].
	memberSize := (Interop hasMembers: self)
		ifTrue: [ Interop getMemberSize: self ] ifFalse: [ 0 ].
	arraySize > 0 ifTrue: [ properties add: 'arraySize=', arraySize ].
	memberSize > 0 ifTrue: [ properties add: 'memberSize=', memberSize ].
	(Interop isInstantiable: self) ifTrue: [ properties add: 'instantiable' ].
	(Interop isExecutable: self) ifTrue: [ properties add: 'executable' ].
	(Interop isMetaObject: self) ifTrue: [ properties add: 'meta' ].
	(Interop hasIdentity: self) ifTrue: [ properties add: 'identity' ].
	name := (Interop isHostObject: self)
		ifTrue: [ 'HostObject' ]
		ifFalse: [ 'ForeignObject' ].
	^ properties
		ifEmpty: [ name ]
		ifNotEmpty: [ name, '[', (properties joinSeparatedBy: ','), ']' ]