Extension { #name : #Object }

{ #category : #'*TruffleSqueak-Core' }
Object >> defaultLabelForPolyglotTools [
	^ String streamContents: [ :aStream |
		aStream nextPutAll: (
			(Interop hasMetaObject: self)
				ifTrue: [ (Interop getMetaQualifiedName: (Interop getMetaObject: self)) asString ]
				ifFalse: [ self class name ]).
		(Interop hasLanguage: self)
			ifTrue: [ aStream
				nextPutAll: ' (';
				nextPutAll: (Interop getLanguageInfo: self) getName asString;
				nextPutAll: ')' ] ]

]

{ #category : #'*TruffleSqueak-Core-boolean' }
Object >> interopAsBoolean [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-number' }
Object >> interopAsByte [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-date and time' }
Object >> interopAsDate [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-number' }
Object >> interopAsDouble [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-duration' }
Object >> interopAsDuration [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-number' }
Object >> interopAsFloat [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-date and time' }
Object >> interopAsInstant [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-number' }
Object >> interopAsInt [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-number' }
Object >> interopAsLong [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-pointer' }
Object >> interopAsPointer [
	^ Java throwUnsupportedMessageException
]

{ #category : #'*TruffleSqueak-Core-string' }
Object >> interopAsString [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-date and time' }
Object >> interopAsTime [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-date and time' }
Object >> interopAsTimeZone [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-executable' }
Object >> interopExecute: arguments [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-number' }
Object >> interopFitsInByte [
	^ false
]

{ #category : #'*TruffleSqueak-Core-number' }
Object >> interopFitsInDouble [
	^ false
]

{ #category : #'*TruffleSqueak-Core-number' }
Object >> interopFitsInFloat [
	^ false
]

{ #category : #'*TruffleSqueak-Core-number' }
Object >> interopFitsInInt [
	^ false
]

{ #category : #'*TruffleSqueak-Core-number' }
Object >> interopFitsInLong [
	^ false
]

{ #category : #'*TruffleSqueak-Core-number' }
Object >> interopFitsInShort [
	^ false
]

{ #category : #'*TruffleSqueak-Core-arrays' }
Object >> interopGetArraySize [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-buffers' }
Object >> interopGetBufferSize [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-meta-object' }
Object >> interopGetDeclaringMetaObject [
	^ Java throwUnsupportedMessageException
]

{ #category : #'*TruffleSqueak-Core-exception' }
Object >> interopGetExceptionCause [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-exception' }
Object >> interopGetExceptionExitStatus [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-exception' }
Object >> interopGetExceptionMessage [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-exception' }
Object >> interopGetExceptionStackTrace [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-exception' }
Object >> interopGetExceptionType [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-executable' }
Object >> interopGetExecutableName [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Object >> interopGetHashEntriesIterator [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Object >> interopGetHashKeysIterator [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Object >> interopGetHashSize [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Object >> interopGetHashValuesIterator [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-iterator' }
Object >> interopGetIterator [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-iterator' }
Object >> interopGetIteratorNextElement [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-language' }
Object >> interopGetLanguage [
	<primitive: 'primitiveSqueakLanguage' module: 'PolyglotPlugin'>
]

{ #category : #'*TruffleSqueak-Core-members' }
Object >> interopGetMembers: includeInternal [
	| selectors currentClass |
	selectors := includeInternal
		ifTrue: [ Set withAll: self class allInstVarNames ]
		ifFalse: [ Set new ].
	currentClass := self class.
	[ currentClass isNil ] whileFalse: [
		selectors addAll: (currentClass selectors collect: [ :ea | Interop selectorToMember: ea ]).
		currentClass := currentClass superclass ].
	^ selectors asArray
]

{ #category : #'*TruffleSqueak-Core-meta-object' }
Object >> interopGetMetaObject [
	^ self class
]

{ #category : #'*TruffleSqueak-Core-meta-object' }
Object >> interopGetMetaQualifiedName [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-meta-object' }
Object >> interopGetMetaSimpleName [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-scope' }
Object >> interopGetScopeParent [
	^ Java throwUnsupportedMessageException
]

{ #category : #'*TruffleSqueak-Core-source location' }
Object >> interopGetSourceLocation [
	^ Java throwUnsupportedMessageException
]

{ #category : #'*TruffleSqueak-Core-arrays' }
Object >> interopHasArrayElements [
	^ false
]

{ #category : #'*TruffleSqueak-Core-buffers' }
Object >> interopHasBufferElements [
	^ false
]

{ #category : #'*TruffleSqueak-Core-meta-object' }
Object >> interopHasDeclaringMetaObject [
	^ false
]

{ #category : #'*TruffleSqueak-Core-exception' }
Object >> interopHasExceptionCause [
	^ false
]

{ #category : #'*TruffleSqueak-Core-exception' }
Object >> interopHasExceptionMessage [
	^ false
]

{ #category : #'*TruffleSqueak-Core-exception' }
Object >> interopHasExceptionStackTrace [
	^ false
]

{ #category : #'*TruffleSqueak-Core-executable' }
Object >> interopHasExecutableName [
	^ false
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Object >> interopHasHashEntries [
	^ false
]

{ #category : #'*TruffleSqueak-Core-iterator' }
Object >> interopHasIterator [
	^ false
]

{ #category : #'*TruffleSqueak-Core-iterator' }
Object >> interopHasIteratorNextElement [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-language' }
Object >> interopHasLanguage [
	^ true
]

{ #category : #'*TruffleSqueak-Core-members' }
Object >> interopHasMemberReadSideEffects: member [
	^ false
]

{ #category : #'*TruffleSqueak-Core-members' }
Object >> interopHasMemberWriteSideEffects: member [
	^ false
]

{ #category : #'*TruffleSqueak-Core-members' }
Object >> interopHasMembers [
	^ true
]

{ #category : #'*TruffleSqueak-Core-meta-object' }
Object >> interopHasMetaObject [
	^ true
]

{ #category : #'*TruffleSqueak-Core-scope' }
Object >> interopHasScopeParent [
	^ false
]

{ #category : #'*TruffleSqueak-Core-source location' }
Object >> interopHasSourceLocation [
	^ false "TODO"
]

{ #category : #'*TruffleSqueak-Core-identity' }
Object >> interopIdentityHashCode [
	^ Java asInteger: self identityHash
]

{ #category : #'*TruffleSqueak-Core-instantiable' }
Object >> interopInstantiate: arguments [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-members' }
Object >> interopInvokeMember: member and: arguments [
	^ (Interop memberToSelector: member for: self arguments: arguments)
		ifNotNil: [ :sel | self perform: sel withArguments: arguments]
		ifNil: [ self doesNotUnderstand: (
			Message selector: member asSymbol arguments: arguments) ]
]

{ #category : #'*TruffleSqueak-Core-arrays' }
Object >> interopIsArrayElementInsertable: index [
	^ false
]

{ #category : #'*TruffleSqueak-Core-arrays' }
Object >> interopIsArrayElementModifiable: index [
	^ false
]

{ #category : #'*TruffleSqueak-Core-arrays' }
Object >> interopIsArrayElementReadable: index [
	^ false
]

{ #category : #'*TruffleSqueak-Core-arrays' }
Object >> interopIsArrayElementRemovable: index [
	^ false
]

{ #category : #'*TruffleSqueak-Core-boolean' }
Object >> interopIsBoolean [
	^ false
]

{ #category : #'*TruffleSqueak-Core-buffers' }
Object >> interopIsBufferWritable [
	^ false
]

{ #category : #'*TruffleSqueak-Core-date and time' }
Object >> interopIsDate [
	^ false
]

{ #category : #'*TruffleSqueak-Core-duration' }
Object >> interopIsDuration [
	^ false
]

{ #category : #'*TruffleSqueak-Core-exception' }
Object >> interopIsException [
	^ false
]

{ #category : #'*TruffleSqueak-Core-exception' }
Object >> interopIsExceptionIncompleteSource [
	^ false
]

{ #category : #'*TruffleSqueak-Core-executable' }
Object >> interopIsExecutable [
	^ false
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Object >> interopIsHashEntryExisting: key [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Object >> interopIsHashEntryInsertable: key [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Object >> interopIsHashEntryModifiable: key [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Object >> interopIsHashEntryReadable: key [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Object >> interopIsHashEntryRemovable: key [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Object >> interopIsHashEntryWritable: key [
	^ (self interopIsHashEntryModifiable: key) or: [ self interopIsHashEntryInsertable: key ]
]

{ #category : #'*TruffleSqueak-Core-identity' }
Object >> interopIsIdentical: other and: node [
	^ self == other
]

{ #category : #'*TruffleSqueak-Core-identity' }
Object >> interopIsIdenticalOrUndefined: other [
	^ Java asTriState: self == other
]

{ #category : #'*TruffleSqueak-Core-instantiable' }
Object >> interopIsInstantiable [
	^ false
]

{ #category : #'*TruffleSqueak-Core-iterator' }
Object >> interopIsIterator [
	^ false
]

{ #category : #'*TruffleSqueak-Core-members' }
Object >> interopIsMemberInsertable: member [
	^ false
]

{ #category : #'*TruffleSqueak-Core-members' }
Object >> interopIsMemberInternal: member [
	^ false
]

{ #category : #'*TruffleSqueak-Core-members' }
Object >> interopIsMemberInvocable: member [
	^  member isString and: [
			(Interop memberToSelector: member for: self) notNil or: [
				(self class lookupSelector: #doesNotUnderstand:) ~~ (Object >> #doesNotUnderstand:) ] ]
]

{ #category : #'*TruffleSqueak-Core-members' }
Object >> interopIsMemberModifiable: member [
	^ false
]

{ #category : #'*TruffleSqueak-Core-members' }
Object >> interopIsMemberReadable: member [
	^ member isString and: [ (self class instVarIndexFor: member ifAbsent: 0) > 0 ]
]

{ #category : #'*TruffleSqueak-Core-members' }
Object >> interopIsMemberRemovable: member [
	^ false
]

{ #category : #'*TruffleSqueak-Core-meta-object' }
Object >> interopIsMetaInstance: arguments [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-meta-object' }
Object >> interopIsMetaObject [
	^ false
]

{ #category : #'*TruffleSqueak-Core-null' }
Object >> interopIsNull [
	^ false
]

{ #category : #'*TruffleSqueak-Core-number' }
Object >> interopIsNumber [
	^ false
]

{ #category : #'*TruffleSqueak-Core-pointer' }
Object >> interopIsPointer [
	^ false
]

{ #category : #'*TruffleSqueak-Core-scope' }
Object >> interopIsScope [
	^ false
]

{ #category : #'*TruffleSqueak-Core-string' }
Object >> interopIsString [
	^ self isString
]

{ #category : #'*TruffleSqueak-Core-date and time' }
Object >> interopIsTime [
	^ false
]

{ #category : #'*TruffleSqueak-Core-date and time' }
Object >> interopIsTimeZone [
	^ false
]

{ #category : #'*TruffleSqueak-Core-arrays' }
Object >> interopReadArrayElement: index [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-buffers' }
Object >> interopReadBufferByte: index [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-buffers' }
Object >> interopReadBufferDouble: index [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-buffers' }
Object >> interopReadBufferFloat: index [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-buffers' }
Object >> interopReadBufferInt: index [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-buffers' }
Object >> interopReadBufferLong: index [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-buffers' }
Object >> interopReadBufferShort: index [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Object >> interopReadHashValue: key [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Object >> interopReadHashValueOrDefault: key and: default [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-members' }
Object >> interopReadMember: member [
	^ self instVarAt: (self class
			instVarIndexFor: member asString
			ifAbsent: [
				(Interop memberToSelector: member for: self)
					ifNotNil: [ :sel |
						(self class lookupSelector: sel)
							ifNotNil: [ :cm | [ :args | self withArgs: args executeMethod: cm ] ]
							ifNil: [ nil "TODO: should never happen?" ] ]
					ifNil: [ nil "TODO" ] ] )
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Object >> interopRemoveHashEntry: key [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-members' }
Object >> interopRemoveMember: member [
	^ Java throwUnsupportedMessageException
]

{ #category : #'*TruffleSqueak-Core-exception' }
Object >> interopThrowException [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-string' }
Object >> interopToDisplayString: allowSideEffects [
	^ self printString
]

{ #category : #'*TruffleSqueak-Core-arrays' }
Object >> interopWriteArrayElement: index and: value [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-buffers' }
Object >> interopWriteBufferByte: index and: value [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-buffers' }
Object >> interopWriteBufferDouble: index and: value [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-buffers' }
Object >> interopWriteBufferFloat: index and: value [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-buffers' }
Object >> interopWriteBufferInt: index and: value [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-buffers' }
Object >> interopWriteBufferLong: index and: value [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-buffers' }
Object >> interopWriteBufferShort: index and: value [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Object >> interopWriteHashEntry: key and: value [
	self subclassResponsibility
]

{ #category : #'*TruffleSqueak-Core-members' }
Object >> interopWriteMember: member and: value [
	^ Java throwUnsupportedMessageException
]

{ #category : #'*TruffleSqueak-Core' }
Object >> isForeignObject [
	^ false
]
