printing
printInteropOn: aStream
	| properties |
	aStream nextPutAll: 'ForeignObject'.
	properties := OrderedCollection new.
	(Interop isNull: self) ifTrue: [ properties add: 'null' ].
	(Interop isBoolean: self) ifTrue: [ properties add: 'bool' ].
	(Interop isString: self) ifTrue: [ properties add: 'string' ].
	(Interop isNumber: self) ifTrue: [ properties add: 'number' ].
	(Interop isDate: self) ifTrue: [ properties add: 'date' ].
	(Interop isDuration: self) ifTrue: [ properties add: 'duration' ].
	(Interop isInstant: self) ifTrue: [ properties add: 'instant' ].
	(Interop isTime: self) ifTrue: [ properties add: 'time' ].
	(Interop isTimeZone: self) ifTrue: [ properties add: 'timezone' ].
	(Interop isException: self) ifTrue: [ properties add: 'exception' ].
	(Interop isMetaObject: self) ifTrue: [ properties add: 'meta' ].
	(Interop isIterator: self) ifTrue: [ properties add: 'iterator' ].
	(Interop isHostObject: self) ifTrue: [ properties add: 'host' ].
	(Interop isHostSymbol: self) ifTrue: [ properties add: 'host-symbol' ].
	(Interop isHostFunction: self) ifTrue: [ properties add: 'host-function' ].
	(Interop isExecutable: self) ifTrue: [ properties add: 'executable' ].
	(Interop isInstantiable: self) ifTrue: [ properties add: 'instantiable' ].
	(Interop isPointer: self) ifTrue: [ properties add: 'pointer' ].
	(Interop isMetaObject: self) ifTrue: [ properties add: 'meta' ].
	(Interop isScope: self) ifTrue: [ properties add: (
		'scope,hasParent={1}' format: { Interop hasScopeParent: self }) ].
	(Interop hasIdentity: self) ifTrue: [ properties add: 'identity' ].
	(Interop hasArrayElements: self) ifTrue: [ properties add:
		'arraySize=', (Interop getArraySize: self) ].
	(Interop hasMembers: self) ifTrue: [ properties add:
		'memberSize=', (Interop getMemberSize: self) ].
	(Interop hasHashEntries: self) ifTrue: [ properties add:
		'hashSize=', (Interop getHashSize: self) ].
	(Interop hasBufferElements: self) ifTrue: [ properties add: (
		'bufferSize={1},writable={2}' format: {
			Interop getBufferSize: self. Interop isBufferWritable: self }) ].
	(Interop hasSourceLocation: self) ifTrue: [ properties add: 'sourceLocation' ].
	properties ifNotEmpty: [ aStream nextPutAll: '[', (properties joinSeparatedBy: ','), ']' ]
