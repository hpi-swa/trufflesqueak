"
TruffleSqueak exposes all non-Smalltalk objects as instances of ForeignObject.
The class provides mappings between Truffle's interop API and the Smalltalk metaobject protocol.
"
Class {
	#name : #ForeignObject,
	#superclass : #Object,
	#category : 'TruffleSqueak-Core'
}

{ #category : #'instance creation' }
ForeignObject class >> new [
	^ self error: 'Instances can only be created using the Polyglot API.'
]

{ #category : #'instance creation' }
ForeignObject class >> new: sizeRequested [
	^ self error: 'Instances can only be created using the Polyglot API.'
]

{ #category : #setup }
ForeignObject class >> primitiveRegisterForeignObjectClass [
	<primitive: 'primitiveRegisterForeignObjectClass' module: 'PolyglotPlugin'>
	^ false
]

{ #category : #setup }
ForeignObject class >> register [
	(self primitiveRegisterForeignObjectClass)
		ifFalse: [ self error: 'Failed to register ', self name ,' class' ]

]

{ #category : #arithmetic }
ForeignObject >> * aNumber [
	^ self asNumber * aNumber
]

{ #category : #arithmetic }
ForeignObject >> + aNumber [
	^ self asNumber + aNumber
]

{ #category : #arithmetic }
ForeignObject >> , other [
	self isString ifTrue: [ ^ self asString, other ].
	(Interop hasArrayElements: self) ifTrue: [ ^ self asArray, other ].
	^ super, other
]

{ #category : #arithmetic }
ForeignObject >> - aNumber [
	^ self asNumber - aNumber
]

{ #category : #arithmetic }
ForeignObject >> / aNumber [
	^ self asNumber / aNumber
]

{ #category : #comparing }
ForeignObject >> < anObject [
	^ self asNumber < anObject
]

{ #category : #comparing }
ForeignObject >> <= anObject [
	^ self asNumber <= anObject
]

{ #category : #comparing }
ForeignObject >> = anObject [
	self == anObject ifTrue: [ ^ true ].
	(Interop isBoolean: self) ifTrue: [ ^ anObject = (Interop asBoolean: self) ].
	(Interop fitsInLong: self) ifTrue: [ ^ anObject = (Interop asLong: self) ].
	(Interop fitsInDouble: self) ifTrue: [ ^ anObject = (Interop asDouble: self) ].
	(Interop isString: self) ifTrue: [ ^ anObject = (Interop asString: self) ].
	((Interop isHostObject: self) and: [ Interop isHostObject: anObject ])
		ifTrue: [ ^ self equals: anObject ].
	^ false
]

{ #category : #comparing }
ForeignObject >> == anObject [
	^ super == anObject or: [ Interop isIdentical: self to: anObject ]
]

{ #category : #comparing }
ForeignObject >> > anObject [
	^ self asNumber > anObject
]

{ #category : #comparing }
ForeignObject >> >= anObject [
	^ self asNumber >= anObject
]

{ #category : #dispatching }
ForeignObject >> _dispatch: member arguments: arguments ifFail: failBlock [
	(member = 'new' and: [ Interop isInstantiable: self ])
		ifTrue: [ ^ Interop instantiate: self with: arguments ].
	(Interop isMemberInvocable: self member: member)
		ifTrue: [ ^ Interop invokeMember: self member: member arguments: arguments ].
	(arguments size = 1 and: [ Interop isMemberWritable: self member: member])
		ifTrue: [ ^ Interop writeMember: self member: member value: arguments first ].
	^ (Interop isMemberReadable: self member: member)
		ifTrue: [ | result |
			result := Interop readMember: self member: member.
			(Interop isExecutable: result)
				ifTrue: [ Interop execute: result with: arguments ]
				ifFalse: [ result ]]
		ifFalse: failBlock
]

{ #category : #dispatching }
ForeignObject >> _dispatch: identifier ifFail: failBlock [
	^ self _dispatch: identifier arguments: #() ifFail: failBlock
]

{ #category : #redirects }
ForeignObject >> _redirect: identifier [
	^ self _dispatch: identifier
		ifFail: [ self error: 'Foreign object does not understand `', identifier, '`' ]
]

{ #category : #converting }
ForeignObject >> adaptToNumber: rcvr andSend: selector [
	"If I am involved in arithmetic with a number, convert me to a number."
	(Interop fitsInLong: self) ifTrue: [ ^ rcvr perform: selector with: (Interop asLong: self) ].
	(Interop fitsInDouble: self) ifTrue: [ ^ rcvr perform: selector with: (Interop asDouble: self) ].
	self error: 'Cannot adapt foreign object to number'
]

{ #category : #'class behavior' }
ForeignObject >> allInstVarNames [
	^ (Interop hasMembers: self)
		ifTrue: [
			((Interop getMembers: self)
				select: [ :ea | (Interop isMemberReadable: self member: ea) and: (
								Interop isMemberInvocable: self member: ea) not ])
					sorted ]
		ifFalse: [ #() ]
]

{ #category : #controlling }
ForeignObject >> and: alternativeBlock [
	^ self asBoolean and: alternativeBlock
]

{ #category : #converting }
ForeignObject >> asArray [
	^ self asCollection asArray
]

{ #category : #converting }
ForeignObject >> asBoolean [
	^ Interop asBoolean: self
]

{ #category : #converting }
ForeignObject >> asCollection [
	^ ForeignArray on: self
]

{ #category : #converting }
ForeignObject >> asDate [
	^ (Interop isDate: self)
		ifTrue: [ Date starting: (DateAndTime fromUnixTime:
				(Interop asDate: self) toEpochDay * 24 * 60 * 60) ]
		ifFalse: [ self error: 'Foreign object cannot be represented as date' ]
]

{ #category : #converting }
ForeignObject >> asDateAndTime [
	(Interop isInstant: self)
		ifTrue: [ ^ (DateAndTime fromUnixTime: (Interop asInstant: self) getEpochSecond) asLocal ]
		ifFalse: [ self error: 'Foreign object cannot be represented as date and time' ]
]

{ #category : #converting }
ForeignObject >> asDuration [
	(Interop isDuration: self)
		ifTrue: [ | javaDuration seconds nanos |
			javaDuration := Interop asDuration: self.
			seconds := javaDuration getSeconds.
			nanos := javaDuration getNano.
			^ Duration seconds: seconds nanoSeconds: nanos ]
		ifFalse: [ self error: 'Foreign object cannot be represented as duration' ]
]

{ #category : #converting }
ForeignObject >> asNumber [
	(Interop fitsInLong: self) ifTrue: [ ^ Interop asLong: self ].
	(Interop fitsInDouble: self) ifTrue: [ ^ Interop asDouble: self ].
	self error: 'Unable to interpret foreign value as number'
]

{ #category : #converting }
ForeignObject >> asSmalltalk [
	(Interop isBoolean: self) ifTrue: [ ^ Interop asBoolean: self ].
	(Interop isString: self) ifTrue: [ ^ Interop asString: self ].
	(Interop fitsInLong: self) ifTrue: [ ^ Interop asLong: self ].
	(Interop fitsInDouble: self) ifTrue: [ ^ Interop asDouble: self ].
	self error: 'Unable to convert to Smalltalk object'
]

{ #category : #converting }
ForeignObject >> asString [
	"Always return a ByteString, because asString is also part of Squeak's API."
	^ Interop asString: (
		self isString
			ifTrue: [ self ]
			ifFalse: [ Interop toDisplayString: self ])
]

{ #category : #converting }
ForeignObject >> asSymbol [
	^ self asString asSymbol
]

{ #category : #converting }
ForeignObject >> asTime [
	(Interop isTime: self)
		ifTrue: [ | javaLocalTime seconds nanos |
			javaLocalTime := Interop asTime: self.
			seconds := javaLocalTime toSecondOfDay.
			nanos := javaLocalTime getNano.
			(Interop isTimeZone: self)
				ifTrue: [ seconds := seconds - self asTimeZone offset asSeconds ].
			^ Time seconds: seconds nanoSeconds: nanos ]
		ifFalse: [ self error: 'Foreign object cannot be represented as time' ]
]

{ #category : #converting }
ForeignObject >> asTimeZone [
	(Interop isTimeZone: self)
		ifTrue: [ | javaZoneId fullStyle shortStyle locale now offset name abbreviation |
			javaZoneId := Interop asTimeZone: self.
			fullStyle := (Java type: 'java.time.format.TextStyle') FULL.
			shortStyle := (Java type: 'java.time.format.TextStyle') SHORT.
			locale := (Java type: 'java.util.Locale') ENGLISH.
			now := (Java type: 'java.time.Instant') now.
			offset := Duration seconds: (javaZoneId getRules getOffset: now) getTotalSeconds.
			name := javaZoneId getDisplayName: fullStyle _: locale.
			abbreviation := javaZoneId getDisplayName: shortStyle _: locale.
			^ TimeZone offset: offset name: name abbreviation: abbreviation.]
		ifFalse: [ self error: 'Foreign object cannot be represented as time zone' ]
]

{ #category : #accessing }
ForeignObject >> at: identifier [
	^ identifier isInteger
		ifTrue: [ Interop readArrayElement: self index: identifier ]
		ifFalse: [ Interop readMember: self member: identifier ]
]

{ #category : #accessing }
ForeignObject >> at: identifier put: value [
	^ identifier isInteger
		ifTrue: [ Interop writeArrayElement: self index: identifier value: value ]
		ifFalse: [ Interop writeMember: self member: identifier value: value ]
]

{ #category : #accessing }
ForeignObject >> basicAt: index [
	(Interop isString: self) ifTrue: [ ^ (Interop asString: self) at: index ].
	^ Interop readArrayElement: self index: index
]

{ #category : #accessing }
ForeignObject >> basicAt: index put: value [
	(Interop isString: self) ifTrue: [ self error: 'Not supported' ].
	^ Interop writeArrayElement: self index: index value: value
]

{ #category : #accessing }
ForeignObject >> basicSize [
	(Interop isString: self) ifTrue: [ ^ (Interop asString: self) basicSize ].
	(Interop hasArrayElements: self) ifTrue: [ ^ Interop getArraySize: self ].
	"The number of indexable fields of fixed-length objects is 0 (see Object>>basicSize)."
	^ 0
]

{ #category : #redirects }
ForeignObject >> clone [
	^ self _redirect: 'clone'
]

{ #category : #redirects }
ForeignObject >> copy [
	^ self _redirect: 'copy'
]

{ #category : #redirects }
ForeignObject >> deepCopy [
	^ self _redirect: 'deepCopy'
]

{ #category : #'user interface' }
ForeignObject >> defaultLabelForInspector [
	^ self defaultLabelForPolyglotTools
]

{ #category : #'error handling' }
ForeignObject >> doesNotUnderstand: aMessage [
	| identifier arguments |
	identifier := aMessage selector asString copyUpTo: $:.
	arguments := aMessage arguments.
	^ self _dispatch: identifier arguments: arguments
		ifFail: [ super doesNotUnderstand: aMessage ].
]

{ #category : #explorer }
ForeignObject >> explorableMembers [
	^ self allInstVarNames
]

{ #category : #explorer }
ForeignObject >> explorerContents [
	^ (self explorableMembers asOrderedCollection collect: [:member |
		PolyglotObjectExplorerWrapper
			with: self
			member: member ]),
		((1 to: self basicSize) collect: [:index |
			PolyglotObjectExplorerWrapper
				with: (self basicAt: index)
				name: index printString
				model: self ])
]

{ #category : #'source code management' }
ForeignObject >> getSource [
	^ (Interop hasSourceLocation: self)
		ifFalse: [ nil ]
		ifTrue: [ (Interop getSourceLocation: self) getCharacters asString ]
]

{ #category : #explorer }
ForeignObject >> hasContentsInExplorer [

	^self basicSize > 0 or: [self explorableMembers isEmpty not]
]

{ #category : #overrides }
ForeignObject >> identityHash [
	^ (Interop hasIdentity: self)
		ifTrue: [ Interop identityHashCode: self ]
		ifFalse: [Interop hostIdentityHashCode: self ]
]

{ #category : #overrides }
ForeignObject >> inspectorClass [
	^ PolyglotInspector
]

{ #category : #'class behavior' }
ForeignObject >> instSize [
	^ Interop getMemberSize: self
]

{ #category : #'system primitives' }
ForeignObject >> instVarAt: index [
	^ self instVarNamed: (self allInstVarNames at: index)
]

{ #category : #'system primitives' }
ForeignObject >> instVarAt: index put: anObject [
	^ self instVarNamed: (self allInstVarNames at: index) put: anObject
]

{ #category : #'system primitives' }
ForeignObject >> instVarNamed: aName [
	^ Interop readMember: self member: aName
]

{ #category : #'system primitives' }
ForeignObject >> instVarNamed: aName put: aValue [
	^ Interop writeMember: self member: aName value: aValue
]

{ #category : #overrides }
ForeignObject >> isByteString [
	^ false
]

{ #category : #testing }
ForeignObject >> isEmpty [
	^ self basicSize = 0
]

{ #category : #testing }
ForeignObject >> isForeignObject [
	^ true
]

{ #category : #'class membership' }
ForeignObject >> isKindOf: aClass [
	^ (super isKindOf: aClass) or: [
		aClass isForeignObject and: [
			(Interop isMetaObject: aClass) and: [ 
				Interop isMetaInstance: aClass instance: self ] ] ]
]

{ #category : #testing }
ForeignObject >> isNil [
	^ Interop isNull: self
]

{ #category : #overrides }
ForeignObject >> isString [
	^ Interop isString: self
]

{ #category : #testing }
ForeignObject >> isZero [
	(Interop fitsInLong: self) ifTrue: [ ^ (Interop asLong: self) isZero ].
	(Interop fitsInDouble: self) ifTrue: [ ^ (Interop asDouble: self) isZero ].
	self error: 'Unable to interpret foreign value as number'
]

{ #category : #printing }
ForeignObject >> longPrintOn: aStream [

	self allInstVarNames withIndexDo:
		[:title :index |
		aStream nextPutAll: title;
		 nextPut: $:;
		 space;
		 tab;
		 print: (self instVarAt: index);
		 cr]
]

{ #category : #testing }
ForeignObject >> notNil [
	^ self isNil not
]

{ #category : #controlling }
ForeignObject >> or: alternativeBlock [
	^ self asBoolean or: alternativeBlock
]

{ #category : #redirects }
ForeignObject >> postCopy [
	^ self _redirect: 'postCopy'
]

{ #category : #printing }
ForeignObject >> printInteropOn: aStream [
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

]

{ #category : #printing }
ForeignObject >> printOn: aStream [
	Polyglot emphasizeInterop
		ifTrue: [ self printInteropOn: aStream ]
		ifFalse: [ aStream nextPutAll: self asString ]
]

{ #category : #'class membership' }
ForeignObject >> respondsTo: aSymbol [ 
	^ (super respondsTo: aSymbol) or: [
		(Interop isMemberReadable: self member: aSymbol) or: [
			Interop isMemberInvocable: self member: aSymbol ] ]
]

{ #category : #redirects }
ForeignObject >> shallowCopy [
	^ self _redirect: 'shallowCopy'
]

{ #category : #accessing }
ForeignObject >> size [
	(Interop hasArrayElements: self) ifTrue: [ ^ Interop getArraySize: self ].
	(Interop isString: self) ifTrue: [ ^ (Interop asString: self) basicSize ].
	self errorNotIndexable
]

{ #category : #evaluating }
ForeignObject >> value [
	^ (Interop isExecutable: self)
		ifTrue: [ Interop execute: self with: #() ]
		ifFalse: [ self ]
]

{ #category : #evaluating }
ForeignObject >> value: firstArg [
	^ Interop execute: self with: {firstArg}
]

{ #category : #evaluating }
ForeignObject >> value: firstArg value: secondArg [
	^ Interop execute: self with: {firstArg. secondArg}
]

{ #category : #evaluating }
ForeignObject >> value: firstArg value: secondArg value: thirdArg [
	^ Interop execute: self with: {firstArg. secondArg. thirdArg}
]

{ #category : #evaluating }
ForeignObject >> value: firstArg value: secondArg value: thirdArg value: fourthArg [
	^ Interop execute: self with: {firstArg. secondArg. thirdArg. fourthArg}
]

{ #category : #evaluating }
ForeignObject >> valueWithArguments: anArray [
	^ Interop execute: self with: anArray
]

{ #category : #redirects }
ForeignObject >> veryDeepCopy [
	^ self _redirect: 'veryDeepCopy'
]
