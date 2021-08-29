Class {
	#name : #ForeignObjectTest,
	#superclass : #TestCase,
	#category : 'TruffleSqueak-Tests'
}

{ #category : #helpers }
ForeignObjectTest >> jsEval: aString [
	^ Polyglot eval: Polyglot languageIdJS string: aString
]

{ #category : #testing }
ForeignObjectTest >> testJSArray [
	| jsArray stCollection |
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	jsArray := (self jsEval: '[1, 3, 4]').

	self assert: (Interop hasArrayElements: jsArray).
	self assert: 3 equals: (Interop getArraySize: jsArray).
	self assert: (Interop isArrayElementExisting: jsArray index: 2).
	self deny: (Interop isArrayElementExisting: jsArray index: 5).
	self deny: (Interop isArrayElementInsertable: jsArray index: 2).
	self assert: (Interop isArrayElementInsertable: jsArray index: 5).
	self assert: (Interop isArrayElementModifiable: jsArray index: 2).
	self deny: (Interop isArrayElementModifiable: jsArray index: 5).
	self assert: (Interop isArrayElementReadable: jsArray index: 2).
	self deny: (Interop isArrayElementReadable: jsArray index: 5).
	self assert: (Interop isArrayElementRemovable: jsArray index: 2).
	self deny: (Interop isArrayElementRemovable: jsArray index: 5).
	self assert: (Interop isArrayElementWritable: jsArray index: 2).
	self assert: (Interop isArrayElementWritable: jsArray index: 5).

	self assert: 3 equals: (Interop readArrayElement: jsArray index: 2).
	self assert: 5 equals: (Interop writeArrayElement: jsArray index: 2 value: 5).
	self assert: 5 equals: (Interop readArrayElement: jsArray index: 2).
	self assert: jsArray equals: (Interop removeArrayElement: jsArray index: 2).
	self assert: 2 equals: (Interop getArraySize: jsArray).
	
	jsArray := (self jsEval: '[1, 2, 3, 4]').
	self assert: 4 equals: jsArray size.
	1 to: jsArray size do: [ :i |
		self assert: i equals: (jsArray at: i) ].
	
	stCollection := jsArray asCollection.
	self assert: (stCollection includes: 1).
	self assert: 10 equals: stCollection sum.
	self assert: 4 equals: stCollection max.
	self assert: #(1 4 9 16) equals: (stCollection collect: [ :e | e * e ]).
	self assert: jsArray size + 1 equals: (jsArray, #(Smalltalk)) size

]

{ #category : #testing }
ForeignObjectTest >> testJSBasic [
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].
	self assert: (Interop isNull: (self jsEval: 'null')).
	
	self assert: (Interop isInstantiable: (self jsEval: 'Object')).
	self assert: 0 equals: (Interop getMemberSize:
		(Interop instantiate: (self jsEval: 'Object') with: #()))
]

{ #category : #testing }
ForeignObjectTest >> testJSDate [
	| jsDate dateAndTime |
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	jsDate := (self jsEval: 'new Date(949322096000)').
	
	self assert: (Interop isInstant: jsDate).
	dateAndTime := DateAndTime fromUnixTime: 949322096.
	self assert: dateAndTime equals: jsDate asDateAndTime.

	self assert: (Interop isDate: jsDate).
	self assert: dateAndTime asDate equals: jsDate asDate.
	
	self assert: (Interop isTime: jsDate).
	self assert: dateAndTime asTime equals: jsDate asTime.

	self assert: (Interop isTimeZone: jsDate).
	self assert: jsDate asTimeZone abbreviation notEmpty.

	self deny: (Interop isDuration: jsDate)

]

{ #category : #testing }
ForeignObjectTest >> testJSExecutable [
	| jsLambda |
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	jsLambda := (self jsEval: 'x => x').
	self assert: (Interop isExecutable: jsLambda).
	self assert: Smalltalk equals: (Interop execute: jsLambda with: {Smalltalk}).
	self assert: Array equals: (jsLambda value: Array).
	
	self assert: 42 equals: (self jsEval: '() => 42') value.
	self assert: (2 + 3) equals: ((self jsEval: '(a, b) => a + b') value: 2 value: 3).
	self assert: (2 + 3 + 4) equals: (
		(self jsEval: '(a, b, c) => a + b + c') value: 2 value: 3 value: 4).
	self assert: (2 + 3 + 4 + 5) equals: (
		(self jsEval: '(a, b, c, d) => a + b + c + d') value: 2 value: 3 value: 4 value: 5).
	self assert: (2 + 3 + 4 + 5 + 6 + 7 + 8) equals: (
		(self jsEval: '(a, b, c, d, e, f, g) => a + b + c + d + e + f + g')
			valueWithArguments: #(2 3 4 5 6 7 8)).

]

{ #category : #testing }
ForeignObjectTest >> testJSMetadataAPIs [
	| jsCode jsObject |
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	jsCode := '{a: "Foo"}'.
	jsObject := (self jsEval: 'new Object(', jsCode, ')').

	self assert: (Interop hasLanguage: jsObject).
	self assert: (Interop getLanguage: jsObject) equals: (Interop getLanguage: jsObject)
		description: 'Languages not equal'.

	"All boxed objects provide their class as meta object"
	self assert: (Interop hasMetaObject: jsObject).
	self assert: (Interop getMetaObject: jsObject) equals: (self jsEval: 'Object')
		description: 'Meta objects not equal'.
	self assert: 'Object' equals: (Interop getMetaQualifiedName: (Interop getMetaObject: jsObject)) asString.
	self assert: 'Object' equals: (Interop getMetaSimpleName: (Interop getMetaObject: jsObject)) asString.
	self assert: jsCode equals: (Interop toDisplayString: jsObject) asString.
	
	self assert: (jsObject isKindOf: (Interop getMetaObject: jsObject)).
	self assert: (jsObject respondsTo: #a).
	self deny: (jsObject respondsTo: #foo).
	
]

{ #category : #testing }
ForeignObjectTest >> testJSObject [
	| jsObject |
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	jsObject := (self jsEval: 'new Object({a: "Foo"})').
	
	self deny: (Interop isBoolean: jsObject).
	self should: [ Interop asBoolean: jsObject ] raise: Error.
	self deny: (Interop isNull: jsObject).
	self deny: (Interop isString: jsObject).
	self should: [ Interop asString: jsObject ] raise: Error.
	self deny: (Interop isPointer: jsObject).
	self should: [ Interop asPointer: jsObject ] raise: Error.
	self deny: (Interop fitsInLong: jsObject).
	self should: [ Interop asLong: jsObject ] raise: Error.
	self deny: (Interop fitsInDouble: jsObject).
	self should: [ Interop asDouble: jsObject ] raise: Error.
	self deny: (Interop hasArrayElements: jsObject).
	
	self assert: 1 equals: (Interop getMemberSize: jsObject).
	self assert: (Interop getMemberSize: jsObject)
		equals: (Interop getMembers: jsObject) size.
	self assert: ((Interop getMembers: jsObject) includes: 'a').

	self assert: (Interop hasIdentity: jsObject).
	self assert: (Interop hasMembers: jsObject).
	self assert: (Interop isMemberExisting: jsObject member: 'a').
	self deny: (Interop isMemberInsertable: jsObject member: 'a').
	self deny: (Interop isMemberInternal: jsObject member: 'a').
	self deny: (Interop isMemberInvocable: jsObject member: 'a').
	self assert: (Interop isMemberModifiable: jsObject member: 'a').
	self assert: (Interop isMemberReadable: jsObject member: 'a').
	self assert: (Interop isMemberRemovable: jsObject member: 'a').
	self assert: (Interop isMemberWritable: jsObject member: 'a').
	self assert: 'Foo' equals: (Interop readMember: jsObject member: 'a').
	
	self deny: (Interop isMemberExisting: jsObject member: 'b').
	self assert: (Interop isMemberInsertable: jsObject member: 'b').
	self assert: Smalltalk equals: (Interop writeMember: jsObject member: 'b' value: Smalltalk).
	self assert: ((Interop getMembers: jsObject) includes: 'b').
	self assert: Smalltalk equals: (Interop readMember: jsObject member: 'b').
	self assert: jsObject equals: (Interop removeMember: jsObject member: 'b').
	self deny: (Interop isMemberExisting: jsObject member: 'b')

]

{ #category : #testing }
ForeignObjectTest >> testSmalltalkFromJS [
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	self assert: Morph equals: (self jsEval: 'Polyglot.eval("smalltalk", "Morph")').
	self assert: 2 equals:
		(self jsEval: 'Polyglot.eval(''smalltalk'', ''Array'').with_with(42, true).length').

	"Ensure objects instantiated through interop are correctly initialized (see trufflesqueak#112)"
	self assert: Morph new color equals:
		(self jsEval: 'new (Polyglot.eval("smalltalk", "Morph"))().color()')
	
]
