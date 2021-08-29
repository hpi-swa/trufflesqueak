"
This class provides access to GraalVM's interoperability API.
"
Class {
	#name : #Interop,
	#superclass : #Object,
	#category : 'TruffleSqueak-Core'
}

{ #category : #'primitives converting' }
Interop class >> asBoolean: anObject [
	<primitive: 'primitiveAsBoolean' module: 'PolyglotPlugin'>
	^ (self isBoolean: anObject)
		ifTrue: [ anObject interopAsBoolean ]
		ifFalse: [ self error: 'Object cannot be converted to boolean' ]
]

{ #category : #'primitives converting' }
Interop class >> asDate: anObject [
	<primitive: 'primitiveAsDate' module: 'PolyglotPlugin'>
	(self isDate: anObject)
		ifFalse: [ self error: 'Object cannot be converted to date' ]
		ifTrue: [ self error: 'Unexpected error' ]
]

{ #category : #'primitives numbers' }
Interop class >> asDouble: anObject [
	<primitive: 'primitiveAsDouble' module: 'PolyglotPlugin'>
	^ (self fitsInDouble: anObject)
		ifTrue: [ anObject interopAsDouble ]
		ifFalse: [ self error: 'Object does not fit in double' ]
]

{ #category : #'primitives converting' }
Interop class >> asDuration: anObject [
	<primitive: 'primitiveAsDuration' module: 'PolyglotPlugin'>
	(self isDuration: anObject)
		ifFalse: [ self error: 'Object cannot be converted to duration' ]
		ifTrue: [ self error: 'Unexpected error' ]
]

{ #category : #'primitives converting' }
Interop class >> asInstant: anObject [
	<primitive: 'primitiveAsInstant' module: 'PolyglotPlugin'>
	(self isInstant: anObject)
		ifFalse: [ self error: 'Object cannot be converted to instant' ]
		ifTrue: [ self error: 'Unexpected error' ]
]

{ #category : #'primitives numbers' }
Interop class >> asLong: anObject [
	<primitive: 'primitiveAsLong' module: 'PolyglotPlugin'>
	^ (self fitsInLong: anObject)
		ifTrue: [ anObject interopAsLong ]
		ifFalse: [ self error: 'Object does not fit in long' ]
]

{ #category : #'primitives converting' }
Interop class >> asPointer: anObject [
	<primitive: 'primitiveAsPointer' module: 'PolyglotPlugin'>
	(self isPointer: anObject)
		ifFalse: [ self error: 'Object cannot be converted to a pointer' ]
		ifTrue: [ self error: 'Unexpected error' ]
]

{ #category : #'primitives converting' }
Interop class >> asString: anObject [
	<primitive: 'primitiveAsString' module: 'PolyglotPlugin'>
	^ (self isString: anObject)
		ifTrue: [ anObject interopAsString ]
		ifFalse: [ self error: 'Object cannot be converted to string' ]
]

{ #category : #'primitives converting' }
Interop class >> asTime: anObject [
	<primitive: 'primitiveAsTime' module: 'PolyglotPlugin'>
	(self isTime: anObject)
		ifFalse: [ self error: 'Object cannot be converted to time' ]
		ifTrue: [ self error: 'Unexpected error' ]
]

{ #category : #'primitives converting' }
Interop class >> asTimeZone: anObject [
	<primitive: 'primitiveAsTimeZone' module: 'PolyglotPlugin'>
	(self isTimeZone: anObject)
		ifFalse: [ self error: 'Object cannot be converted to time zone' ]
		ifTrue: [ self error: 'Unexpected error' ]
]

{ #category : #'primitives executables' }
Interop class >> execute: anObject with: arguments [
	<primitive: 'primitiveExecute' module: 'PolyglotPlugin'>
	^ (self isExecutable: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopExecute: arguments ] ]
		ifFalse: [ self error: 'Object is not executable' ]
]

{ #category : #'primitives numbers' }
Interop class >> fitsInDouble: anObject [
	<primitive: 'primitiveFitsInDouble' module: 'PolyglotPlugin'>
	^ anObject interopFitsInDouble
]

{ #category : #'primitives numbers' }
Interop class >> fitsInLong: anObject [
	<primitive: 'primitiveFitsInLong' module: 'PolyglotPlugin'>
	^ anObject interopFitsInLong
]

{ #category : #'primitives arrays' }
Interop class >> getArraySize: anObject [
	<primitive: 'primitiveGetArraySize' module: 'PolyglotPlugin'>
	^ (self hasArrayElements: anObject)
		ifTrue: [ anObject interopGetArraySize ]
		ifFalse: [ self error: 'Object does not have array elements' ]
]

{ #category : #'primitives buffers' }
Interop class >> getBufferSize: anObject [
	<primitive: 'primitiveGetBufferSize' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject interopGetBufferSize ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]
]

{ #category : #'primitives meta-data' }
Interop class >> getDeclaringMetaObject: anObject [
	<primitive: 'primitiveGetDeclaringMetaObject' module: 'PolyglotPlugin'>
	^ (self hasDeclaringMetaObject: anObject)
		ifTrue: [ anObject interopGetDeclaringMetaObject ]
		ifFalse: [ self error: 'Object does not have a declaring meta object' ]
]

{ #category : #'primitives exceptions' }
Interop class >> getExceptionCause: anObject [
	<primitive: 'primitiveGetExceptionCause' module: 'PolyglotPlugin'>
	(self hasExceptionCause: anObject)
		ifFalse: [ self error: 'Exception has no cause' ]
		ifTrue: [ self error: 'Unexpected error' ]
]

{ #category : #'primitives exceptions' }
Interop class >> getExceptionExitStatus: anObject [
	<primitive: 'primitiveGetExceptionExitStatus' module: 'PolyglotPlugin'>
	((self isException: anObject) and: [ (self getExceptionType: anObject) = 'EXIT' ])
		ifFalse: [ self error: 'Object is not an exit exception' ]
		ifTrue: [ self error: 'Unexpected error' ]
]

{ #category : #'primitives exceptions' }
Interop class >> getExceptionMessage: anObject [
	<primitive: 'primitiveGetExceptionMessage' module: 'PolyglotPlugin'>
	^ (self hasExceptionMessage: anObject)
		ifTrue: [ anObject interopGetExceptionMessage ]
		ifFalse: [ self error: 'Exception has no message' ]
]

{ #category : #'primitives exceptions' }
Interop class >> getExceptionStackTrace: anObject [
	<primitive: 'primitiveGetExceptionStackTrace' module: 'PolyglotPlugin'>
	(self hasExceptionStackTrace: anObject)
		ifFalse: [ self error: 'Exception has no stack trace' ]
		ifTrue: [ self error: 'Unexpected error' ]
]

{ #category : #'primitives exceptions' }
Interop class >> getExceptionType: anObject [
	<primitive: 'primitiveGetExceptionType' module: 'PolyglotPlugin'>
	(self isException: anObject)
		ifFalse: [ self error: 'Object is not an exception' ]
		ifTrue: [ self error: 'Unexpected error' ]
]

{ #category : #'primitives executables' }
Interop class >> getExecutableName: anObject [
	<primitive: 'primitiveGetExecutableName' module: 'PolyglotPlugin'>
	^ anObject interopGetExecutableName
]

{ #category : #'primitives hashes' }
Interop class >> getHashEntriesIterator: anObject [
	<primitive: 'primitiveGetHashEntriesIterator' module: 'PolyglotPlugin'>
	^ (self hasHashEntries: anObject)
		ifTrue: [ anObject interopGetHashEntriesIterator ]
		ifFalse: [ self error: 'Object does not have hash entries' ]
]

{ #category : #'primitives hashes' }
Interop class >> getHashKeysIterator: anObject [
	<primitive: 'primitiveGetHashKeysIterator' module: 'PolyglotPlugin'>
	^ (self hasHashEntries: anObject)
		ifTrue: [ anObject interopGetHashKeysIterator ]
		ifFalse: [ self error: 'Object does not have hash entries' ]
]

{ #category : #'primitives hashes' }
Interop class >> getHashSize: anObject [
	<primitive: 'primitiveGetHashSize' module: 'PolyglotPlugin'>
	^ (self hasHashEntries: anObject)
		ifTrue: [ anObject interopGetHashSize ]
		ifFalse: [ self error: 'Object does not have hash entries' ]
]

{ #category : #'primitives hashes' }
Interop class >> getHashValuesIterator: anObject [
	<primitive: 'primitiveGetHashValuesIterator' module: 'PolyglotPlugin'>
	^ (self hasHashEntries: anObject)
		ifTrue: [ anObject interopGetHashValuesIterator ]
		ifFalse: [ self error: 'Object does not have hash entries' ]
]

{ #category : #'primitives iterator' }
Interop class >> getIterator: anObject [
	<primitive: 'primitiveGetIterator' module: 'PolyglotPlugin'>
	^ (self hasIterator: anObject)
		ifTrue: [ anObject interopGetIterator ]
		ifFalse: [ self error: 'Object does not have an iterator' ]
]

{ #category : #'primitives iterator' }
Interop class >> getIteratorNextElement: anObject [
	<primitive: 'primitiveGetIteratorNextElement' module: 'PolyglotPlugin'>
	^ (self isIterator: anObject)
		ifTrue: [ anObject interopGetIteratorNextElement ]
		ifFalse: [ self error: 'Object is not an iterator' ]
]

{ #category : #'primitives meta-data' }
Interop class >> getLanguage: anObject [
	<primitive: 'primitiveGetLanguage' module: 'PolyglotPlugin'>
	self primitiveFailed
]

{ #category : #'primitives meta-data' }
Interop class >> getLanguageInfo: anObject [
	<primitive: 'primitiveGetLanguageInfo' module: 'PolyglotPlugin'>
	self primitiveFailed
]

{ #category : #convenience }
Interop class >> getMemberSize: anObject [
	^ self getMemberSize: anObject includeInternal: true
]

{ #category : #'primitives members' }
Interop class >> getMemberSize: anObject includeInternal: aBoolean [
	<primitive: 'primitiveGetMemberSize' module: 'PolyglotPlugin'>
	^ (self hasMembers: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ (self getMembers: anObject includeInternal: aBoolean) size ] ]
		ifFalse: [ self error: 'Object does not have members' ]
]

{ #category : #convenience }
Interop class >> getMembers: anObject [
	^ self getMembers: anObject includeInternal: true
]

{ #category : #'primitives members' }
Interop class >> getMembers: anObject includeInternal: aBoolean [
	<primitive: 'primitiveGetMembers' module: 'PolyglotPlugin'>
	^ (self hasMembers: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopGetMembers: aBoolean ] ]
		ifFalse: [ self error: 'Object does not have members' ]
]

{ #category : #'primitives meta-data' }
Interop class >> getMetaObject: anObject [
	<primitive: 'primitiveGetMetaObject' module: 'PolyglotPlugin'>
	^ (self hasMetaObject: anObject)
		ifTrue: [ anObject interopGetMetaObject ]
		ifFalse: [ self error: 'Object does not have a meta object' ]
]

{ #category : #'primitives meta-data' }
Interop class >> getMetaQualifiedName: anObject [
	<primitive: 'primitiveGetMetaQualifiedName' module: 'PolyglotPlugin'>
	^ (self isMetaObject: anObject)
		ifTrue: [ anObject interopGetMetaQualifiedName ]
		ifFalse: [ self error: 'Object is not a meta object' ]
]

{ #category : #'primitives meta-data' }
Interop class >> getMetaSimpleName: anObject [
	<primitive: 'primitiveGetMetaSimpleName' module: 'PolyglotPlugin'>
	^ (self isMetaObject: anObject)
		ifTrue: [ anObject interopGetMetaSimpleName ]
		ifFalse: [ self error: 'Object is not a meta object' ]
]

{ #category : #'primitives scope' }
Interop class >> getScopeParent: anObject [
	<primitive: 'primitiveGetScopeParent' module: 'PolyglotPlugin'>
	^ (self hasScopeParent: anObject)
		ifTrue: [ anObject interopGetScopeParent ]
		ifFalse: [ self error: 'Object does not have a scope parent' ]
]

{ #category : #'primitives scope' }
Interop class >> getSmalltalkScope [
	<primitive: 'primitiveGetSmalltalkScope' module: 'PolyglotPlugin'>
	self primitiveFailed
]

{ #category : #'primitives meta-data' }
Interop class >> getSourceLocation: anObject [
	<primitive: 'primitiveGetSourceLocation' module: 'PolyglotPlugin'>
	(self hasSourceLocation: anObject)
		ifFalse: [ self error: 'Object has no source location' ]
		ifTrue: [ self primitiveFailed ]
]

{ #category : #'primitives arrays' }
Interop class >> hasArrayElements: anObject [
	<primitive: 'primitiveHasArrayElements' module: 'PolyglotPlugin'>
	^ anObject interopHasArrayElements
]

{ #category : #'primitives buffers' }
Interop class >> hasBufferElements: anObject [
	<primitive: 'primitiveHasBufferElements' module: 'PolyglotPlugin'>
	^ anObject interopHasBufferElements
]

{ #category : #'primitives meta-data' }
Interop class >> hasDeclaringMetaObject: anObject [
	<primitive: 'primitiveHasDeclaringMetaObject' module: 'PolyglotPlugin'>
	^ anObject interopHasDeclaringMetaObject
]

{ #category : #'primitives exceptions' }
Interop class >> hasExceptionCause: anObject [
	<primitive: 'primitiveHasExceptionCause' module: 'PolyglotPlugin'>
	^ false
]

{ #category : #'primitives exceptions' }
Interop class >> hasExceptionMessage: anObject [
	<primitive: 'primitiveHasExceptionMessage' module: 'PolyglotPlugin'>
	^ anObject interopHasExceptionMessage
]

{ #category : #'primitives exceptions' }
Interop class >> hasExceptionStackTrace: anObject [
	<primitive: 'primitiveHasExceptionStackTrace' module: 'PolyglotPlugin'>
	^ false
]

{ #category : #'primitives executables' }
Interop class >> hasExecutableName: anObject [
	<primitive: 'primitiveHasExecutableName' module: 'PolyglotPlugin'>
	^ anObject interopHasExecutableName
]

{ #category : #'primitives hashes' }
Interop class >> hasHashEntries: anObject [
	<primitive: 'primitiveHasHashEntries' module: 'PolyglotPlugin'>
	^ anObject interopHasHashEntries
]

{ #category : #'primitives identity' }
Interop class >> hasIdentity: anObject [
	<primitive: 'primitiveHasIdentity' module: 'PolyglotPlugin'>
	^ self isIdentical: anObject to: anObject
]

{ #category : #'primitives iterator' }
Interop class >> hasIterator: anObject [
	<primitive: 'primitiveHasIterator' module: 'PolyglotPlugin'>
	^ anObject interopHasIterator
]

{ #category : #'primitives iterator' }
Interop class >> hasIteratorNextElement: anObject [
	<primitive: 'primitiveHasIteratorNextElement' module: 'PolyglotPlugin'>
	^ (self isIterator: anObject)
		ifTrue: [ anObject interopHasIteratorNextElement ]
		ifFalse: [ self error: 'Object is not an iterator' ]
]

{ #category : #'primitives meta-data' }
Interop class >> hasLanguage: anObject [
	<primitive: 'primitiveHasLanguage' module: 'PolyglotPlugin'>
	^ false
]

{ #category : #'primitives members' }
Interop class >> hasMemberReadSideEffects: andObject member: aMember [
	<primitive: 'primitiveHasMemberReadSideEffects' module: 'PolyglotPlugin'>
	^ true
]

{ #category : #'primitives members' }
Interop class >> hasMemberWriteSideEffects: anObject member: aMember [
	<primitive: 'primitiveHasMemberWriteSideEffects' module: 'PolyglotPlugin'>
	^ true
]

{ #category : #'primitives members' }
Interop class >> hasMembers: anObject [
	<primitive: 'primitiveHasMembers' module: 'PolyglotPlugin'>
	^ anObject interopHasMembers
]

{ #category : #'primitives meta-data' }
Interop class >> hasMetaObject: anObject [
	<primitive: 'primitiveHasMetaObject' module: 'PolyglotPlugin'>
	^ anObject interopHasMetaObject
]

{ #category : #'primitives scope' }
Interop class >> hasScopeParent: anObject [
	<primitive: 'primitiveHasScopeParent' module: 'PolyglotPlugin'>
	^ anObject interopHasScopeParent
]

{ #category : #'primitives meta-data' }
Interop class >> hasSourceLocation: anObject [
	<primitive: 'primitiveHasSourceLocation' module: 'PolyglotPlugin'>
	^ anObject interopHasSourceLocation
]

{ #category : #'primitives host' }
Interop class >> hostIdentityHashCode: anObject [
	<primitive: 'primitiveHostIdentityHashCode' module: 'PolyglotPlugin'>
	self error: 'Unexpected error'
]

{ #category : #'primitives identity' }
Interop class >> identityHashCode: anObject [
	<primitive: 'primitiveIdentityHashCode' module: 'PolyglotPlugin'>
	^ (self hasIdentity: anObject)
		ifTrue: [ self identityHash ]
		ifFalse: [ self error: 'Object does not have an identity' ]
]

{ #category : #'primitives instantiables' }
Interop class >> instantiate: anObject with: arguments [
	<primitive: 'primitiveInstantiate' module: 'PolyglotPlugin'>
	^ (self isInstantiable: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopInstantiate: arguments ] ]
		ifFalse: [ self error: 'Object is not instantiable' ]
]

{ #category : #'primitives members' }
Interop class >> invokeMember: anObject member: aMember arguments: arguments [
	<primitive: 'primitiveInvokeMember' module: 'PolyglotPlugin'>
	^ (self isMemberInvocable: anObject member: aMember)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopInvokeMember: aMember and: arguments ] ]
		ifFalse: [ self error: 'Member ', aMember, ' is not invocable' ]
]

{ #category : #'primitives arrays' }
Interop class >> isArrayElementExisting: anObject index: anIndex [
	<primitive: 'primitiveIsArrayElementExisting' module: 'PolyglotPlugin'>
	^ (self isArrayElementModifiable: anObject index: anIndex) or: [
		(self isArrayElementReadable: anObject index: anIndex) or: [
			(self isArrayElementRemovable: anObject index: anIndex) ] ]
]

{ #category : #'primitives arrays' }
Interop class >> isArrayElementInsertable: anObject index: anIndex [
	<primitive: 'primitiveIsArrayElementInsertable' module: 'PolyglotPlugin'>
	^ anObject interopIsArrayElementInsertable: anIndex - 1
]

{ #category : #'primitives arrays' }
Interop class >> isArrayElementModifiable: anObject index: anIndex [
	<primitive: 'primitiveIsArrayElementModifiable' module: 'PolyglotPlugin'>
	^ anObject interopIsArrayElementModifiable: anIndex - 1
]

{ #category : #'primitives arrays' }
Interop class >> isArrayElementReadable: anObject index: anIndex [
	<primitive: 'primitiveIsArrayElementReadable' module: 'PolyglotPlugin'>
	^ anObject interopIsArrayElementReadable: anIndex - 1
]

{ #category : #'primitives arrays' }
Interop class >> isArrayElementRemovable: anObject index: anIndex [
	<primitive: 'primitiveIsArrayElementRemovable' module: 'PolyglotPlugin'>
	^ anObject interopIsArrayElementRemovable: anIndex - 1
]

{ #category : #'primitives arrays' }
Interop class >> isArrayElementWritable: anObject index: anIndex [
	<primitive: 'primitiveIsArrayElementWritable' module: 'PolyglotPlugin'>
	^ (self isArrayElementModifiable: anObject index: anIndex) or: [
		(self isArrayElementInsertable: anObject index: anIndex) ]
]

{ #category : #'primitives converting' }
Interop class >> isBoolean: anObject [
	<primitive: 'primitiveIsBoolean' module: 'PolyglotPlugin'>
	^ anObject interopIsBoolean
]

{ #category : #'primitives buffers' }
Interop class >> isBufferWritable: anObject [
	<primitive: 'primitiveIsBufferWritable' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopIsBufferWritable ] ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]
]

{ #category : #'primitives converting' }
Interop class >> isDate: anObject [
	<primitive: 'primitiveIsDate' module: 'PolyglotPlugin'>
	^ false
]

{ #category : #'primitives converting' }
Interop class >> isDuration: anObject [
	<primitive: 'primitiveIsDuration' module: 'PolyglotPlugin'>
	^ false
]

{ #category : #'primitives exceptions' }
Interop class >> isException: anObject [
	<primitive: 'primitiveIsException' module: 'PolyglotPlugin'>
	^ anObject interopIsException
]

{ #category : #'primitives exceptions' }
Interop class >> isExceptionIncompleteSource: anObject [
	<primitive: 'primitiveIsExceptionIncompleteSource' module: 'PolyglotPlugin'>
	((self isException: anObject) and: [ (self getExceptionType: anObject) = 'PARSE_ERROR' ])
		ifFalse: [ self error: 'Object is not a parse error' ]
		ifTrue: [ self error: 'Unexpected error' ]
]

{ #category : #'primitives executables' }
Interop class >> isExecutable: anObject [
	<primitive: 'primitiveIsExecutable' module: 'PolyglotPlugin'>
	^ anObject interopIsExecutable
]

{ #category : #'primitives hashes' }
Interop class >> isHashEntryExisting: anObject key: key [
	<primitive: 'primitiveIsHashEntryExisting' module: 'PolyglotPlugin'>
	^ anObject interopIsHashEntryExisting: key
]

{ #category : #'primitives hashes' }
Interop class >> isHashEntryInsertable: anObject key: key [
	<primitive: 'primitiveIsHashEntryInsertable' module: 'PolyglotPlugin'>
	^ anObject interopIsHashEntryInsertable: key
]

{ #category : #'primitives hashes' }
Interop class >> isHashEntryModifiable: anObject key: key [
	<primitive: 'primitiveIsHashEntryModifiable' module: 'PolyglotPlugin'>
	^ anObject interopIsHashEntryModifiable: key
]

{ #category : #'primitives hashes' }
Interop class >> isHashEntryReadable: anObject key: key [
	<primitive: 'primitiveIsHashEntryReadable' module: 'PolyglotPlugin'>
	^ anObject interopIsHashEntryReadable: key
]

{ #category : #'primitives hashes' }
Interop class >> isHashEntryRemovable: anObject key: key [
	<primitive: 'primitiveIsHashEntryRemovable' module: 'PolyglotPlugin'>
	^ anObject interopIsHashEntryRemovable: key
]

{ #category : #'primitives hashes' }
Interop class >> isHashEntryWritable: anObject key: key [
	<primitive: 'primitiveIsHashEntryWritable' module: 'PolyglotPlugin'>
	^ anObject interopIsHashEntryWritable: key
]

{ #category : #'primitives host' }
Interop class >> isHostFunction: anObject [
	<primitive: 'primitiveIsHostFunction' module: 'PolyglotPlugin'>
	^ false
]

{ #category : #'primitives host' }
Interop class >> isHostObject: anObject [
	<primitive: 'primitiveIsHostObject' module: 'PolyglotPlugin'>
	^ false
]

{ #category : #'primitives host' }
Interop class >> isHostSymbol: anObject [
	<primitive: 'primitiveIsHostSymbol' module: 'PolyglotPlugin'>
	^ false
]

{ #category : #'primitives identity' }
Interop class >> isIdentical: anObject to: anotherObject [
	<primitive: 'primitiveIsIdentical' module: 'PolyglotPlugin'>
	^ anObject interopIsIdentical: anotherObject and: nil
]

{ #category : #'primitives converting' }
Interop class >> isInstant: anObject [
	<primitive: 'primitiveIsInstant' module: 'PolyglotPlugin'>
	^ false
]

{ #category : #'primitives instantiables' }
Interop class >> isInstantiable: anObject [
	<primitive: 'primitiveIsInstantiable' module: 'PolyglotPlugin'>
	^ anObject interopIsInstantiable
]

{ #category : #'primitives iterator' }
Interop class >> isIterator: anObject [
	<primitive: 'primitiveIsIterator' module: 'PolyglotPlugin'>
	^ anObject interopIsIterator
]

{ #category : #'primitives members' }
Interop class >> isMemberExisting: anObject member: aMember [
	<primitive: 'primitiveIsMemberExisting' module: 'PolyglotPlugin'>
	^ (anObject interopIsMemberReadable: aMember) or: [
		(anObject interopIsMemberModifiable: aMember) or: [
			(anObject interopIsMemberRemovable: aMember) or: [
				(anObject interopIsMemberInvocable: aMember) ] ] ]
]

{ #category : #'primitives members' }
Interop class >> isMemberInsertable: anObject member: aMember [
	<primitive: 'primitiveIsMemberInsertable' module: 'PolyglotPlugin'>
	^ anObject interopIsMemberInsertable: aMember
]

{ #category : #'primitives members' }
Interop class >> isMemberInternal: anObject member: aMember [
	<primitive: 'primitiveIsMemberInternal' module: 'PolyglotPlugin'>
	^ anObject interopIsMemberInternal: aMember
]

{ #category : #'primitives members' }
Interop class >> isMemberInvocable: anObject member: aMember [
	<primitive: 'primitiveIsMemberInvocable' module: 'PolyglotPlugin'>
	^ anObject interopIsMemberInvocable: aMember
]

{ #category : #'primitives members' }
Interop class >> isMemberModifiable: anObject member: aMember [
	<primitive: 'primitiveIsMemberModifiable' module: 'PolyglotPlugin'>
	^ anObject interopIsMemberModifiable: aMember
]

{ #category : #'primitives members' }
Interop class >> isMemberReadable: anObject member: aMember [
	<primitive: 'primitiveIsMemberReadable' module: 'PolyglotPlugin'>
	^ anObject interopIsMemberReadable: aMember
]

{ #category : #'primitives members' }
Interop class >> isMemberRemovable: anObject member: aMember [
	<primitive: 'primitiveIsMemberRemovable' module: 'PolyglotPlugin'>
	^ anObject interopIsMemberRemovable: aMember
]

{ #category : #'primitives members' }
Interop class >> isMemberWritable: anObject member: aMember [
	<primitive: 'primitiveIsMemberWritable' module: 'PolyglotPlugin'>
	^ (anObject interopIsMemberModifiable: aMember) or: [ anObject interopIsMemberInsertable: aMember ]
]

{ #category : #'primitives meta-data' }
Interop class >> isMetaInstance: aMetaObject instance: instance [
	<primitive: 'primitiveIsMetaInstance' module: 'PolyglotPlugin'>
	^ aMetaObject interopIsMetaInstance: instance
]

{ #category : #'primitives meta-data' }
Interop class >> isMetaObject: anObject [
	<primitive: 'primitiveIsMetaObject' module: 'PolyglotPlugin'>
	^ anObject interopIsMetaObject
]

{ #category : #'primitives converting' }
Interop class >> isNull: anObject [
	<primitive: 'primitiveIsNull' module: 'PolyglotPlugin'>
	^ anObject interopIsNull
]

{ #category : #'primitives numbers' }
Interop class >> isNumber: anObject [
	<primitive: 'primitiveIsNumber' module: 'PolyglotPlugin'>
	^ false
]

{ #category : #'primitives converting' }
Interop class >> isPointer: anObject [
	<primitive: 'primitiveIsPointer' module: 'PolyglotPlugin'>
	^ false
]

{ #category : #'primitives scope' }
Interop class >> isScope: anObject [
	<primitive: 'primitiveIsScope' module: 'PolyglotPlugin'>
	^ anObject interopIsScope
]

{ #category : #'primitives converting' }
Interop class >> isString: anObject [
	<primitive: 'primitiveIsString' module: 'PolyglotPlugin'>
	^ anObject interopIsString
]

{ #category : #'primitives converting' }
Interop class >> isTime: anObject [
	<primitive: 'primitiveIsTime' module: 'PolyglotPlugin'>
	^ false
]

{ #category : #'primitives converting' }
Interop class >> isTimeZone: anObject [
	<primitive: 'primitiveIsTimeZone' module: 'PolyglotPlugin'>
	^ false
]

{ #category : #dispatch }
Interop class >> memberToSelector: aMember for: anObject [
	| selectorName selectorNameVariant currentClass |
	selectorName := aMember asString copyReplaceAll: '_' with: ':'.
	selectorNameVariant := selectorName, ':'.
	currentClass := anObject class.
	[ currentClass isNil ] whileFalse: [
		currentClass selectors
			detect: [ :ea | ea = selectorName or: [ ea = selectorNameVariant ] ]
			ifFound: [ :s | ^ s ].
		currentClass := currentClass superclass ].
	^ nil

]

{ #category : #dispatch }
Interop class >> memberToSelector: aMember for: anObject arguments: arguments [
	| selector |
	selector := ((aMember asString copyReplaceAll: '_' with: ':'), (
		arguments ifEmpty: [ '' ] ifNotEmpty: [ ':' ])) asSymbol.
	^ (anObject class canUnderstand: selector)
		ifTrue: [ selector ] ifFalse: [ nil ]
]

{ #category : #'primitives arrays' }
Interop class >> readArrayElement: anObject index: anIndex [
	<primitive: 'primitiveReadArrayElement' module: 'PolyglotPlugin'>
	^ (self isArrayElementReadable: anObject index: anIndex)
		ifTrue: [ anObject interopReadArrayElement: anIndex - 1 ]
		ifFalse: [ self error: 'Array element is not readable at index #', anIndex ]
]

{ #category : #'primitives buffers' }
Interop class >> readBufferByte: anObject index: anIndex [
	<primitive: 'primitiveReadBufferByte' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopReadBufferByte: anIndex - 1 ] ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]
]

{ #category : #'primitives buffers' }
Interop class >> readBufferDouble: anObject index: anIndex [
	<primitive: 'primitiveReadBufferDouble' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopReadBufferDouble: anIndex - 1 ] ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]
]

{ #category : #'primitives buffers' }
Interop class >> readBufferFloat: anObject index: anIndex [
	<primitive: 'primitiveReadBufferFloat' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopReadBufferFloat: anIndex - 1 ] ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]
]

{ #category : #'primitives buffers' }
Interop class >> readBufferInt: anObject index: anIndex [
	<primitive: 'primitiveReadBufferInt' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopReadBufferInt: anIndex - 1 ] ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]
]

{ #category : #'primitives buffers' }
Interop class >> readBufferLong: anObject index: anIndex [
	<primitive: 'primitiveReadBufferLong' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopReadBufferLong: anIndex - 1 ] ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]
]

{ #category : #'primitives buffers' }
Interop class >> readBufferShort: anObject index: anIndex [
	<primitive: 'primitiveReadBufferShort' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopReadBufferShort: anIndex - 1 ] ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]
]

{ #category : #'primitives hashes' }
Interop class >> readHashValue: anObject key: key [
	<primitive: 'primitiveReadHashValue' module: 'PolyglotPlugin'>
	^ (self isHashEntryReadable: anObject key: key)
		ifTrue: [ anObject interopReadHashValue: key ]
		ifFalse: [ self error: 'Key ', key, ' is not readable' ]
]

{ #category : #'primitives hashes' }
Interop class >> readHashValueOrDefault: anObject key: key default: defaultValue [
	<primitive: 'primitiveReadHashValueOrDefault' module: 'PolyglotPlugin'>
	^ (self hasHashEntries: anObject)
		ifTrue: [ anObject interopReadHashValueOrDefault: key and: defaultValue ]
		ifFalse: [ self error: 'Object does not have hash entries' ]
]

{ #category : #'primitives members' }
Interop class >> readMember: anObject member: aMember [
	<primitive: 'primitiveReadMember' module: 'PolyglotPlugin'>
	^ (self isMemberReadable: anObject member: aMember)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopReadMember: aMember ] ]
		ifFalse: [ self error: 'Member ', aMember, ' is not readable' ]
]

{ #category : #'primitives arrays' }
Interop class >> removeArrayElement: anObject index: anIndex [
	<primitive: 'primitiveRemoveArrayElement' module: 'PolyglotPlugin'>
	(self isArrayElementRemovable: anObject index: anIndex)
		ifFalse: [ self error: 'Array element is not removable at index #', anIndex ]
		ifTrue: [ self primitiveFailed ]
]

{ #category : #'primitives hashes' }
Interop class >> removeHashEntry: anObject key: key [
	<primitive: 'primitiveRemoveHashEntry' module: 'PolyglotPlugin'>
	^ (self isHashEntryRemovable: anObject key: key)
		ifTrue: [ anObject interopRemoveHashEntry: key ]
		ifFalse: [ self error: 'Key ', key, ' is not removable' ]
]

{ #category : #'primitives members' }
Interop class >> removeMember: anObject member: aMember [
	<primitive: 'primitiveRemoveMember' module: 'PolyglotPlugin'>
	^ (self isMemberRemovable: anObject member: aMember)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopRemoveMember: aMember ] ]
		ifFalse: [ self error: 'Member ', aMember, ' is not removable' ]
]

{ #category : #dispatch }
Interop class >> selectorToMember: aSelector [
	| member |
	member := aSelector asString.
	member last = $: ifTrue: [ member := member allButLast copyReplaceAll: ':' with: '_' ].
	^ member
]

{ #category : #'primitives exceptions' }
Interop class >> throwException: anObject [
	<primitive: 'primitiveThrowException' module: 'PolyglotPlugin'>
	(self isException: anObject)
		ifFalse: [ self error: 'Object is not an exception and cannot be thrown' ]
		ifTrue: [ self error: 'Unexpected error' ]
]

{ #category : #'primitives meta-data' }
Interop class >> toDisplayString: anObject [
	^ self toDisplayString: anObject allowSideEffects: true
]

{ #category : #'primitives meta-data' }
Interop class >> toDisplayString: anObject allowSideEffects: aBoolean [
	<primitive: 'primitiveToDisplayString' module: 'PolyglotPlugin'>
	^ anObject interopToDisplayString: aBoolean
]

{ #category : #'primitives arrays' }
Interop class >> writeArrayElement: anObject index: anIndex value: aValue [
	<primitive: 'primitiveWriteArrayElement' module: 'PolyglotPlugin'>
	^ (self isArrayElementWritable: anObject index: anIndex)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopWriteArrayElement: anIndex - 1 and: aValue ] ]
		ifFalse: [ self error: 'Array element is not writable at index #', anIndex ]
]

{ #category : #'primitives buffers' }
Interop class >> writeBufferByte: anObject index: anIndex value: value [
	<primitive: 'primitiveWriteBufferByte' module: 'PolyglotPlugin'>
	^ (self isBufferWritable: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopWriteBufferByte: anIndex - 1 and: value ] ]
		ifFalse: [ self error: 'Buffer is not writable or does not have buffer elements' ]
]

{ #category : #'primitives buffers' }
Interop class >> writeBufferDouble: anObject index: anIndex value: value [
	<primitive: 'primitiveWriteBufferDouble' module: 'PolyglotPlugin'>
	^ (self isBufferWritable: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopWriteBufferDouble: anIndex - 1 and: value ] ]
		ifFalse: [ self error: 'Buffer is not writable or does not have buffer elements' ]
]

{ #category : #'primitives buffers' }
Interop class >> writeBufferFloat: anObject index: anIndex value: value [
	<primitive: 'primitiveWriteBufferFloat' module: 'PolyglotPlugin'>
	^ (self isBufferWritable: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopWriteBufferFloat: anIndex - 1 and: value ] ]
		ifFalse: [ self error: 'Buffer is not writable or does not have buffer elements' ]
]

{ #category : #'primitives buffers' }
Interop class >> writeBufferInt: anObject index: anIndex value: value [
	<primitive: 'primitiveWriteBufferInt' module: 'PolyglotPlugin'>
	^ (self isBufferWritable: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopWriteBufferInt: anIndex - 1 and: value ] ]
		ifFalse: [ self error: 'Buffer is not writable or does not have buffer elements' ]
]

{ #category : #'primitives buffers' }
Interop class >> writeBufferLong: anObject index: anIndex value: value [
	<primitive: 'primitiveWriteBufferLong' module: 'PolyglotPlugin'>
	^ (self isBufferWritable: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopWriteBufferLong: anIndex - 1 and: value ] ]
		ifFalse: [ self error: 'Buffer is not writable or does not have buffer elements' ]
]

{ #category : #'primitives buffers' }
Interop class >> writeBufferShort: anObject index: anIndex value: value [
	<primitive: 'primitiveWriteBufferShort' module: 'PolyglotPlugin'>
	^ (self isBufferWritable: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopWriteBufferShort: anIndex - 1 and: value ] ]
		ifFalse: [ self error: 'Buffer is not writable or does not have buffer elements' ]
]

{ #category : #'primitives hashes' }
Interop class >> writeHashEntry: anObject key: key value: value [
	<primitive: 'primitiveWriteHashEntry' module: 'PolyglotPlugin'>
	^ (self isHashEntryWritable: anObject key: key)
		ifTrue: [ anObject interopWriteHashEntry: key and: value ]
		ifFalse: [ self error: 'Key ', key, ' is not writable' ]
]

{ #category : #'primitives members' }
Interop class >> writeMember: anObject member: aMember value: aValue [
	<primitive: 'primitiveWriteMember' module: 'PolyglotPlugin'>
	^ (self isMemberWritable: anObject member: aMember)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopWriteMember: aMember and: aValue ] ]
		ifFalse: [ self error: 'Member ', aMember, ' is not writable' ]
]
