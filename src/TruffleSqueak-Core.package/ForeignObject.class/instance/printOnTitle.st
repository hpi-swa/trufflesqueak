printing
printOnTitle
	| properties |
	properties := OrderedCollection new.
	(Interop isNull: self) ifTrue: [ properties add: 'nil' ].
	(Interop isHostObject: self) ifTrue: [ properties add: 'host' ].
	(Interop isString: self) ifTrue: [ properties add: 'string' ].
	(Interop fitsInLong: self) ifTrue: [ properties add: (Interop asLong: self) asString ].
	(Interop fitsInDouble: self) ifTrue: [ properties add: (Interop asDouble: self) asString ].
	(Interop isMetaObject: self) ifTrue: [ properties add: 'meta' ].
	(Interop isInstantiable: self) ifTrue: [ properties add: 'instantiable' ].
	(Interop isExecutable: self) ifTrue: [ properties add: 'executable' ].
	(Interop isMetaObject: self) ifTrue: [ properties add: 'meta' ].
	(Interop hasIdentity: self) ifTrue: [ properties add: 'identity' ].
	(Interop hasArrayElements: self) ifTrue: [ properties add: 'arraySize=', (Interop getArraySize: self) ].
	(Interop hasMembers: self) ifTrue: [ properties add: 'memberSize=', (Interop getMemberSize: self) ].
	^ properties
		ifEmpty: [ 'ForeignObject' ]
		ifNotEmpty: [ 'ForeignObject[', (properties joinSeparatedBy: ','), ']' ]