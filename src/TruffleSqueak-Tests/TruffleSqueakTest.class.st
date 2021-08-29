Class {
	#name : #TruffleSqueakTest,
	#superclass : #TestCase,
	#category : 'TruffleSqueak-Tests'
}

{ #category : #accessing }
TruffleSqueakTest >> assertSorted: aCollection name: aName [
	| previousElement |
	previousElement := aCollection at: 1.
	2 to: aCollection size do: [ :index |
		| element |
		element := aCollection at: index.
		self assert: (previousElement caseInsensitiveLessOrEqual: element)
			description: aName, ' is not sorted at ', index, '(', previousElement, ' > ', element, ')'.
		previousElement := element ]
]

{ #category : #testing }
TruffleSqueakTest >> testAdoptInstanceArrays [
	{ByteArray. DoubleByteArray. WordArray. DoubleWordArray}
		combinations: 2 atATimeDo: [ :each | | instance |
			instance := each second withAll: (1 to: 8).
			each first adoptInstance: instance.
			self deny: instance size = 8.
			each second adoptInstance: instance.
			self assert: ((1 to: 8) hasEqualElements: instance) ]
]

{ #category : #testing }
TruffleSqueakTest >> testArrayStatistics [
	Polyglot isPolyglotEvalAllowed ifFalse: [ ^self ].
	[ TruffleSqueakUtilities arrayStatistics ] on: Error do: [ :e | self fail: e ]
]

{ #category : #testing }
TruffleSqueakTest >> testCallTarget [
	| method string |
	Java primitiveIsHostLookupAllowed ifFalse: [ ^ false ].
	
	self assert: (SmalltalkImage >> #exitToDebugger) callTarget isNil.
	
	method := Integer >> #benchFib.
	1 benchFib. "Ensure method is called"

	self deny: method callTarget isNil.

	string := CallTargetBrowser callTargetInfoStringFor: method callTarget.
	self assert: (string includesSubstring: 'knownCallSiteCount')

]

{ #category : #testing }
TruffleSqueakTest >> testLayoutStatistics [
	Polyglot isPolyglotEvalAllowed ifFalse: [ ^self ].
	[ TruffleSqueakUtilities layoutStatistics ] on: Error do: [ :e | self fail: e ]
]

{ #category : #testing }
TruffleSqueakTest >> testTestMapConsistency [
	| testNames booleanResult |
	Polyglot isPolyglotEvalAllowed ifFalse: [ ^self ].

	testNames := TestCase buildSuite tests collect: [:test |
		test class asString, '>>', test selector].
	booleanResult := (Java type: 'de.hpi.swa.trufflesqueak.test.SqueakTests')
		testTestMapConsistency: testNames.
	self assert: booleanResult description: 'Test failed on Java side'

]

{ #category : #testing }
TruffleSqueakTest >> testVMIntrospection [
	| testObject metaObject markingFlag |
	Polyglot isPolyglotEvalAllowed ifFalse: [ ^self ].

	testObject := SmalltalkImage vmObject.
	"Every Java object understands hashCode"
	self assert: (Interop fitsInLong: testObject hashCode).
	"Ensure that class has a layout"
	self assert: testObject hasLayout.
	"Traverse class and unwrap object"
	self assert: 'Object' equals: testObject getSuperclass toString asString.
	"Allocate Object[]"
	self assert: 10 equals: (testObject getOtherPointers getClass new: 10) size.
	"Test inst vars"
	self assert: (testObject allInstVarNames includesAllOf: #('squeakHash' 'squeakClass' 'superclass' 'methodDict')).
	self deny: (testObject allInstVarNames includesAnyOf: #('clone' 'become' 'finalize' 'object0')).
	"Read and write inst vars"
	markingFlag := testObject markingFlag.
	self assert: (testObject markingFlag: markingFlag not) = markingFlag not.
	self assert: testObject markingFlag = markingFlag not.
	testObject markingFlag: markingFlag.
	
	"Meta Object API"
	self assert: (Interop hasMetaObject: testObject).
	metaObject := Interop getMetaObject: testObject..
	self assert: (Interop isMetaObject: metaObject).
	self assert: 'ClassObject' equals: (Interop getMetaSimpleName: metaObject) asString.	
	self assert: (Interop isMetaInstance: metaObject instance: testObject).	

]
