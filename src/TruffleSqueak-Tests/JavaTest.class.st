Class {
	#name : #JavaTest,
	#superclass : #TestCase,
	#category : 'TruffleSqueak-Tests'
}

{ #category : #failures }
JavaTest >> expectedFailures [
	^ Java primitiveIsHostLookupAllowed
		ifTrue: [ #() ]
		ifFalse: [ self class allTestSelectors ]
]

{ #category : #testing }
JavaTest >> testJavaAddToHostClassPath [
	self assert: Java equals: (Java addToHostClassPath: 'some.jar').
	"No way to check #addToClassPath: succeeded (apart from looking up symbol that lives in some.jar)."
]

{ #category : #testing }
JavaTest >> testJavaArray [
	| ints |
	ints := (Java type: 'int[]') new: 2.
	self assert: 2 equals: ints size.
	
	self should: [ ints at: 0 ] raise: Error.
	self assert: 0 equals: (ints at: 1).
	self assert: 0 equals: (ints at: 2).
	self should: [ ints at: 3 ] raise: Error.
	
	self assert: 32 equals: (ints at: 1 put: 32).
	self assert: 32 equals: (ints at: 1).
	self assert: #(32 0) equals: ints asCollection asArray
]

{ #category : #testing }
JavaTest >> testJavaBasic [
	self assert: Java primitiveIsHostLookupAllowed.

	self assert: (Interop isHostObject: (Java type: 'int[]')).
	self assert: (Interop isHostSymbol: (Java type: 'int[]')).
	self assert: (Interop isHostObject: ((Java type: 'int[]') new: 0)).
	self deny: (Interop isHostSymbol: ((Java type: 'int[]') new: 0)).

	self assert:  (Interop identityHashCode: (Java type: 'int[]')) > 0.
	self assert:  'int' equals: (Interop toDisplayString: (Java type: 'int')) asString.

	self assert: ((Java type: 'java.lang.System') getProperty: 'java.version') notEmpty.

	self assert: ((Java type: 'java.awt.Frame') allInstVarNames includes: 'NORMAL').

]

{ #category : #testing }
JavaTest >> testJavaBuffers [
	| buffer |
	buffer := (Java type: 'java.nio.ByteBuffer') allocate: 32.
	self assert: (Interop hasBufferElements: buffer).
	self assert: 32 equals: (Interop getBufferSize: buffer).
	self assert: (Interop isBufferWritable: buffer).
	
	self assert: 42 equals: (Interop writeBufferByte: buffer index: 0 value: 42).
	self assert: 42 equals: (Interop readBufferByte: buffer index: 0).
	self assert: 512 equals: (Interop writeBufferShort: buffer index: 1 value: 512).
	self assert: 512 equals: (Interop readBufferShort: buffer index: 1).
	self assert: 20000 equals: (Interop writeBufferInt: buffer index: 2 value: 20000).
	self assert: 20000 equals: (Interop readBufferInt: buffer index: 2).
	self assert: 1 << 16 equals: (Interop writeBufferLong: buffer index: 3 value: 1 << 16).
	self assert: 1 << 16 equals: (Interop readBufferInt: buffer index: 3).
	self assert: 2.1 equals: (Interop writeBufferFloat: buffer index: 4 value: 2.1).
	self assert: 2.0999999046325684 equals: (Interop readBufferFloat: buffer index: 4).
	self assert: 4.32198 equals: (Interop writeBufferDouble: buffer index: 5 value: 4.32198).
	self assert: 4.32198 equals: (Interop readBufferDouble: buffer index: 5).

]

{ #category : #testing }
JavaTest >> testJavaDuration [
	| zero nonZero |
	self deny: (Interop isDuration: 42).

	zero := (Java type: 'java.time.Duration') ZERO.
	self assert: (Interop isDuration: zero).
	self assert: zero asDuration isZero.
	
	nonZero := (Java type: 'java.time.Duration') ofSeconds: 123 _: 456.
	self assert: 123 equals: nonZero asDuration asSeconds.
	self assert: 456 equals: nonZero asDuration nanoSeconds.
	self assert: nonZero asDuration = (Duration seconds: 123 nanoSeconds: 456)
]

{ #category : #testing }
JavaTest >> testJavaException [
	| ex |
	self deny: (Interop isException: 42).

	ex := (Java type: 'java.lang.RuntimeException') new.
	self assert: (Interop isException: ex).
]

{ #category : #testing }
JavaTest >> testJavaHashes [
	| map expectedValues iterator |
	map := (Java type: 'java.util.HashMap') new.
	self assert: (Interop hasHashEntries: map).
	self assert: 0 equals: (Interop getHashSize: map).

	self deny: (Interop isHashEntryExisting: map key: 42).
	self assert: Array equals: (Interop readHashValueOrDefault: map key: 42 default: Array).
	self assert: (Interop isHashEntryWritable: map key: 42).
	self assert: Smalltalk equals: (Interop writeHashEntry: map key: 42 value: Smalltalk).
	self assert: 1 equals: (Interop getHashSize: map).
	self assert: (Interop isHashEntryExisting: map key: 42).
	self assert: (Interop isHashEntryReadable: map key: 42).
	self assert: Smalltalk equals: (Interop readHashValue: map key: 42).
	self assert: (Interop isHashEntryModifiable: map key: 42).
	self assert: (Interop isHashEntryRemovable: map key: 42).
	self assert: map equals: (Interop removeHashEntry: map key: 42).
	self assert: 0 equals: (Interop getHashSize: map).
	
	expectedValues := {true. 2.34. #(9 8 7 6 5). OrderedCollection new. Smalltalk}.
	expectedValues withIndexDo: [ :v :i | Interop writeHashEntry: map key: i value: v ].
	self assert: expectedValues size equals: (Interop getHashSize: map).
	
	"Iterate entries"
	iterator := Interop getHashEntriesIterator: map.
	self assert: (Interop isIterator: iterator).
	expectedValues withIndexDo: [ :v :i | | entry |
		self assert: (Interop hasIteratorNextElement: iterator).
		entry := (Interop getIteratorNextElement: iterator).
		self assert: i equals: (entry at: 1).
		self assert: v equals: (entry at: 2) ].
	self deny: (Interop hasIteratorNextElement: iterator).
	
	"Iterate keys"
	iterator := Interop getHashKeysIterator: map.
	self assert: (Interop isIterator: iterator).
	expectedValues withIndexDo: [ :v :i |
		self assert: (Interop hasIteratorNextElement: iterator).
		self assert: i equals: (Interop getIteratorNextElement: iterator) ].
	self deny: (Interop hasIteratorNextElement: iterator).
	
	"Iterate values"
	iterator := Interop getHashValuesIterator: map.
	self assert: (Interop isIterator: iterator).
	expectedValues withIndexDo: [ :v :i |
		self assert: (Interop hasIteratorNextElement: iterator).
		self assert: v equals: (Interop getIteratorNextElement: iterator) ].
	self deny: (Interop hasIteratorNextElement: iterator).

]

{ #category : #testing }
JavaTest >> testJavaInetAddress [
	"Issue #78"
	self assert: '192.168.0.1' equals: ((Java type: 'java.net.InetAddress') getByAddress: #[192 168 0 1]) getHostAddress
]

{ #category : #testing }
JavaTest >> testJavaInstant [
	| now |
	self deny: (Interop isInstant: 42).

	now := (Java type: 'java.time.Instant') now.
	self assert: (Interop isInstant: now).
	self assert: (now asDateAndTime asSeconds - DateAndTime current asSeconds) abs < 2.
]

{ #category : #testing }
JavaTest >> testJavaIterator [
	| list expectedValues iterator |
	list := (Java type: 'java.util.ArrayList') new.
	self assert: (Interop hasIterator: list).
	
	expectedValues := {true. 2.34. #(9 8 7 6 5). OrderedCollection new. Smalltalk}.
	expectedValues do: [ :ea | list add: ea ].
	
	iterator := Interop getIterator: list.
	self assert: (Interop isIterator: iterator).

	expectedValues do: [ :ea |
		self assert: (Interop hasIteratorNextElement: iterator).
		self assert: ea equals: (Interop getIteratorNextElement: iterator) ].
	self deny: (Interop hasIteratorNextElement: iterator).

]

{ #category : #testing }
JavaTest >> testJavaLocalDate [
	| now |
	self deny: (Interop isDate: 42).

	now := (Java type: 'java.time.LocalDate') now.
	self assert: (Interop isDate: now).
	self assert: now asDate = Date dateAndTimeNow first
]

{ #category : #testing }
JavaTest >> testJavaLocalTime [
	| now |
	self deny: (Interop isTime: 42).

	now := (Java type: 'java.time.LocalTime') now.
	self assert: (Interop isTime: now).
	self assert: ((Date dateAndTimeNow second subtractTime: now asTime)) asDuration abs <  (Duration seconds: 2)
]

{ #category : #testing }
JavaTest >> testJavaString [
	{65. "$A" 16r1f43b "Bear Emoji"} do: [ :ea | 
		self assert: (String with: (Character value: ea))
			equals: ((Java type: 'java.lang.String') new: ((Java type: 'java.lang.Character') toChars: ea)) ]
]

{ #category : #testing }
JavaTest >> testJavaZoneId [
	| default |
	self deny: (Interop isTimeZone: 42).

	default := (Java type: 'java.time.ZoneId') systemDefault.
	self assert: (Interop isTimeZone: default).
	self assert: default asTimeZone abbreviation notEmpty.
]
