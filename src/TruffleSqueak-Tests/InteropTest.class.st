Class {
	#name : #InteropTest,
	#superclass : #TestCase,
	#category : 'TruffleSqueak-Tests'
}

{ #category : #testing }
InteropTest >> testArrays [
	self deny: (Interop hasArrayElements: Smalltalk).
	
	{#(1 Smalltalk true nil).
		#[12 24 46 127].
		OrderedCollection with: 42 with: Smalltalk.
		WeakArray with: Smalltalk with: 42 with: Object new} do: [ :ea |
			((ea respondsTo: #isReadOnlyObject) and: [ ea isReadOnlyObject ])
				ifTrue: [ ea beWritableObject ].
			self assert: (Interop hasArrayElements: ea).
			self assert: ea size = (Interop getArraySize: ea).
			ea withIndexDo: [ :val :index |
				self assert: (Interop isArrayElementReadable: ea index: index).
				self assert: (Interop isArrayElementModifiable: ea index: index).
				self assert: val equals: (Interop readArrayElement: ea index: index) ].
			self assert: 7 equals: (Interop writeArrayElement: ea index: 1 value: 7).
			self assert: 7 equals: (Interop readArrayElement: ea index: 1) ].
		
	Java primitiveIsHostLookupAllowed ifFalse: [ ^self ].
	
	self assert: 3 equals: ((Java type: 'java.util.Arrays')
		binarySearch: #[0 8 255 1 192] key: ((Java type: 'java.lang.Byte') valueOf: '1')).
	self assert: 2 equals: ((Java type: 'java.util.Arrays')
		binarySearch: #[0 8 255 1 192] key: ((Java type: 'java.lang.Byte') valueOf: '-1'))

]

{ #category : #testing }
InteropTest >> testBooleans [
	self deny: (Interop isBoolean: Smalltalk).
	
	self assert: (Interop isBoolean: true).
	self assert: true == (Interop asBoolean: true).
	self assert: (Interop isBoolean: false).
	self assert: false == (Interop asBoolean: false).
]

{ #category : #testing }
InteropTest >> testBuffers [
	"Buffers in Smalltalk are not (yet) supported"
	self deny: (Interop hasBufferElements: Smalltalk).
]

{ #category : #testing }
InteropTest >> testDateAndTime [
	| now localTimeZone |
	Java primitiveIsHostLookupAllowed ifFalse: [ ^self ].
	
	self deny: (Interop isDate: Smalltalk).
	self assert: (Interop isDate: Date today).
	self assert: Date today equals: (Interop asDate: Date today) asDate.

	now := Time now.
	self deny: (Interop isTime: Smalltalk).
	self assert: (Interop isTime: now).
	self assert: now equals: (Interop asTime: now) asTime.
	
	self deny: (Interop isTimeZone: Smalltalk).
	self assert: (Interop isTimeZone: TimeZone default).
	self assert: TimeZone default abbreviation
		equals: (Interop asTimeZone: TimeZone default) asTimeZone abbreviation.
	
	now := DateAndTime now.
	self assert: (Interop isTime: now).
	self assert: now asTime equals: (Interop asTime: now) asTime.
	self assert: (Interop isDate: now).
	self assert: now asDate makeUTC equals: (Interop asDate: now) asDate.
	self assert: (Interop isTimeZone:now).
	localTimeZone := DateAndTime localTimeZone.
	[
		DateAndTime localTimeZone: TimeZone timeZones first.
		self assert: now timeZoneAbbreviation
			equals: (Interop asTimeZone: now) asTimeZone abbreviation
	] ensure: [ DateAndTime localTimeZone: localTimeZone ]

]

{ #category : #testing }
InteropTest >> testDurations [
	Java primitiveIsHostLookupAllowed ifFalse: [ ^self ].
	
	self deny: (Interop isDuration: Smalltalk).
	
	self assert: (Interop isDuration: Duration oneDay).
	self assert: Duration oneDay equals: (Interop asDuration: Duration oneDay) asDuration.
]

{ #category : #testing }
InteropTest >> testExceptionHandling [
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	self assert: ((
		Polyglot
			eval: #js
			string: 'try { Polyglot.eval("smalltalk", "42/0") } catch (error) { error }') dividend = 42)
]

{ #category : #testing }
InteropTest >> testExceptions [
	| e |
	
	e := Error new messageText: 'Error message'; yourself.
	
	self assert: (Interop isException: e).
	self assert: (Interop hasExceptionMessage: e).
	self assert: 'Error message' equals: (Interop getExceptionMessage: e).
	self deny: (Interop hasExceptionCause: e).
	self deny: (Interop hasExceptionStackTrace: e). "Not supported yet"

	Java primitiveIsHostLookupAllowed ifFalse: [ ^self ].
	self assert: 'RUNTIME_ERROR' equals: (Interop getExceptionType: e).
	
	"JS tests disabled due to https://github.com/graalvm/graaljs/issues/373"
	true ifTrue: [ ^ self ].
	
	(Polyglot isPermitted: #js) ifFalse: [ ^self ].
	
	e := Polyglot eval: #js string: 'try {foo()} catch (e) {e}'.
	self assert: (Interop isException: e).
	self assert: (Interop hasExceptionMessage: e).
	self assert: 'Error message' equals: (Interop getExceptionMessage: e).
	self assert: 'RUNTIME_ERROR' equals: (Interop getExceptionType: e).
	self assert: (Interop hasExceptionStackTrace: e).
	self deny: (Interop hasExceptionCause: e)

]

{ #category : #testing }
InteropTest >> testExecutables [
	| block |
	self deny: (Interop isExecutable: Smalltalk).
	
	block := [:x | x + 1].
	self assert: (Interop isExecutable: block).
	self assert: 42 equals: (Interop execute: block with: #(41)).
	self assert: (Interop hasExecutableName: block).
	self assert: '[closure] in InteropTest>>testExecutables' equals: (Interop getExecutableName: block)

]

{ #category : #testing }
InteropTest >> testHashes [
	| dict iterator iteratorData |
	self deny: (Interop hasHashEntries: Smalltalk).
	
	dict := Dictionary new
		at: 'foo' put: 'bar';
		at: 13 put: 42;
		yourself.
	self assert: (Interop hasHashEntries: dict).
	self assert: dict size equals: (Interop getHashSize: dict).
	self assert: (Interop isHashEntryExisting: dict key: 'foo').
	self assert: (Interop isHashEntryExisting: dict key: 13).
	self deny: (Interop isHashEntryExisting: dict key: Smalltalk).
	self assert: (Interop isHashEntryInsertable: dict key: Smalltalk).
	self assert: (Interop isHashEntryModifiable: dict key: 'foo').
	self assert: (Interop isHashEntryReadable: dict key: 'foo').
	self assert: (Interop isHashEntryRemovable: dict key: 'foo').
	self assert: (Interop isHashEntryWritable: dict key: 'foo').
	self assert: (dict at: 'foo') equals: (Interop readHashValue: dict key: 'foo').
	self assert: (dict at: 'foo') equals: (Interop readHashValueOrDefault: dict key: 'foo' default: true).
	self assert: (Interop readHashValueOrDefault: dict key: 'bar' default: true).
	Interop removeHashEntry: dict key: 'foo'.
	self deny: (dict includesKey: 'foo').
	Interop writeHashEntry: dict key: Smalltalk value: Array.
	self assert: (dict includesKey: Smalltalk).
	self assert: Array equals: (Interop readHashValue: dict key: Smalltalk).

	"Iterate entries"
	iterator := Interop getHashEntriesIterator: dict.
	self assert: (Interop isIterator: iterator).
	iteratorData := Dictionary new.
	self assert: (Interop hasIteratorNextElement: iterator).
	iteratorData add: (Interop getIteratorNextElement: iterator).
	self assert: (Interop hasIteratorNextElement: iterator).
	iteratorData add: (Interop getIteratorNextElement: iterator).
	self deny: (Interop hasIteratorNextElement: iterator).
	self assert: dict equals: iteratorData.

	"Iterate keys"
	iterator := Interop getHashKeysIterator: dict.
	self assert: (Interop isIterator: iterator).
	iteratorData := Set new.
	self assert: (Interop hasIteratorNextElement: iterator).
	iteratorData add: (Interop getIteratorNextElement: iterator).
	self assert: (Interop hasIteratorNextElement: iterator).
	iteratorData add: (Interop getIteratorNextElement: iterator).
	self deny: (Interop hasIteratorNextElement: iterator).
	self assert: dict keys asSet equals: iteratorData.

	"Iterate values"
	iterator := Interop getHashValuesIterator: dict.
	self assert: (Interop isIterator: iterator).
	iteratorData := Set new.
	self assert: (Interop hasIteratorNextElement: iterator).
	iteratorData add: (Interop getIteratorNextElement: iterator).
	self assert: (Interop hasIteratorNextElement: iterator).
	iteratorData add: (Interop getIteratorNextElement: iterator).
	self deny: (Interop hasIteratorNextElement: iterator).
	self assert: dict values asSet equals: iteratorData.

]

{ #category : #testing }
InteropTest >> testIdentityAPIs [
	"Boxed objects have an identity"
	{nil. 'foo'. #('bar'). Object new. Smalltalk} do: [ :ea |
		self assert: (Interop hasIdentity: ea).
		self assert: (Interop isIdentical: ea to: ea).
		self assert: (Interop identityHashCode: ea) equals: (Interop identityHashCode: ea) ].
	
	Java primitiveIsHostLookupAllowed ifFalse: [ ^self ]. "TODO: remove and make the below work"

	"Unboxed objects do not have an identity"
	#(true false 42 12.34 $x) do: [ :ea |
		self deny: (Interop hasIdentity: ea).
		self deny: (Interop isIdentical: ea to: ea) ]

]

{ #category : #testing }
InteropTest >> testInstantiables [
	self deny: (Interop isInstantiable: Smalltalk).
	
	{Array. OrderedCollection. String} do: [ :ea |
		self assert: (Interop isInstantiable: ea).
		self assert: ea new equals: (Interop instantiate: ea with: #()).
		self assert: (ea new: 5) equals: (Interop instantiate: ea with: #(5)) ]
	
]

{ #category : #testing }
InteropTest >> testIterator [
	self deny: (Interop isIterator: Smalltalk).
	self deny: (Interop hasIterator: Smalltalk).
	self assert: (Interop hasIterator: Dictionary new).
	
	{#(42 Smalltalk true). #[12 15 42 92]. 'foo'.
		OrderedCollection with: 42 with: Smalltalk} do: [ :obj | | iter |
			iter := ReadStream on: obj.
			self assert: (Interop isIterator: iter).
			obj do: [ :ea |
				self assert: (Interop hasIteratorNextElement: iter).
				self assert: ea equals: (Interop getIteratorNextElement: iter) ].
			self deny: (Interop hasIteratorNextElement: iter) ].

]

{ #category : #testing }
InteropTest >> testMembers [
	{nil. #(). 'foo'. Object new} do: [ :ea |
		self assert: (Interop hasMembers: ea).
		(Interop getMembers: ea) do: [ :member |
			self assert: (Interop isMemberInvocable: ea member: member) ] ].
	
	self assert: (Interop invokeMember: #(Smalltalk) member: 'includes' arguments: #(Smalltalk))

]

{ #category : #testing }
InteropTest >> testMetadataAPIs [
	Java primitiveIsHostLookupAllowed ifFalse: [ ^self ].

	"All boxed objects have a language"
	self assert: (Interop hasLanguage: Smalltalk).
	self assert: (Interop getLanguage: Smalltalk) equals: (Interop getLanguage: Smalltalk)
		 description: 'Languages not equal'.
	"Immediate values have no language"
	self deny: (Interop hasLanguage: 42).

	"All boxed objects provide their class as meta object"
	self assert: (Interop hasMetaObject: Smalltalk).
	self assert: Smalltalk class equals: (Interop getMetaObject: Smalltalk).
	self assert: Smalltalk class asString equals: (Interop getMetaQualifiedName: (Interop getMetaObject: Smalltalk)) asString.
	self assert: Smalltalk class asString equals: (Interop getMetaSimpleName: (Interop getMetaObject: Smalltalk)) asString.
	self assert: ((Interop toDisplayString: Smalltalk) asString includesSubstring: Smalltalk asString)
]

{ #category : #testing }
InteropTest >> testNulls [
	self deny: (Interop isNull: Smalltalk).
	self deny: (Interop isNull: UndefinedObject).
	self assert: (Interop isNull: nil)

]

{ #category : #testing }
InteropTest >> testNumbers [
	self deny: (Interop fitsInLong: Smalltalk).
	self deny: (Interop fitsInDouble: Smalltalk).

	self assert: (Interop fitsInLong: 42).
	self assert: 42 == (Interop asLong: 42).
	self assert: (Interop fitsInDouble: 42).
	self assert: 42.0 == (Interop asDouble: 42).
	
	self deny: (Interop fitsInLong: 42.3).
	self assert: (Interop fitsInDouble: 42.3).
	self assert: 42.3 == (Interop asDouble: 42.3)

]

{ #category : #testing }
InteropTest >> testScopeAPIs [
	| s |
	s := Polyglot primitiveGetScope: Polyglot languageIdSmalltalk.
	self assert: SmalltalkInteropScope instance equals: s.
	self assert: (Interop isScope: s).
	self deny: (Interop hasScopeParent: s).
	
	thisContext sender. "Ensure sender is available"
	s := thisContext.
	self assert: (Interop hasScopeParent: s).
	[ Interop hasScopeParent: s ] whileTrue: [
		s := Interop getScopeParent: s.
		self assert: (Interop isScope: s) ]
]

{ #category : #testing }
InteropTest >> testSmalltalkDNU [
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	self assert: 1234 equals: (
		Polyglot
			eval: #js
			string: 'Polyglot.eval("smalltalk", "InteropTestDNUTestSubject new").freedom()').
	self assert: 1 equals: (
		Polyglot
			eval: #js
			string: 'Polyglot.eval("smalltalk", "InteropTestDNUTestSubject new").freedom_("some argument")').
	self assert: 2 equals: (
		Polyglot
			eval: #js
			string: 'Polyglot.eval("smalltalk", "InteropTestDNUTestSubject new").freedom_and_(true, [])').
]

{ #category : #testing }
InteropTest >> testStrings [
	| s |
	self deny: (Interop isString: Smalltalk).

	s := 'foo'.
	self assert: (Interop isString: s).
	self assert: s equals: (Interop asString: s).
	
	s := s asWideString.
	self assert: (s isKindOf: WideString).
	self assert: (Interop isString: s).
	self assert: s equals: (Interop asString: s).

]

{ #category : #testing }
InteropTest >> testToDisplayString [
	{nil. true. false. #(1 3). #[54]. 'foo'. Object new. Smalltalk} do: [ :ea |
		self assert: ea printString equals: (Interop asString: (Interop toDisplayString: ea))]

]
