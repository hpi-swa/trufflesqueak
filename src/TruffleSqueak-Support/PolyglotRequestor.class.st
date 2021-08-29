Class {
	#name : #PolyglotRequestor,
	#superclass : #Object,
	#instVars : [
		'bindings',
		'currentSource'
	],
	#classVars : [
		'Default'
	],
	#category : 'TruffleSqueak-Support'
}

{ #category : #'instance creation' }
PolyglotRequestor class >> default [
	^ Default ifNil: [ Default := self new ]
]

{ #category : #binding }
PolyglotRequestor >> bindingOf: aString [
	(bindings includesKey: aString) ifFalse: [
		aString first isUppercase
			ifTrue: [^nil]
			ifFalse: [bindings at: aString put: nil]
	].
	^ bindings associationAt: aString
]

{ #category : #accessing }
PolyglotRequestor >> bindings [

	^ bindings
]

{ #category : #requests }
PolyglotRequestor >> correctFrom: start to: stop with: aString [
	
	| loc |
	aString = '#insert period' ifTrue:
		[loc := start.
		[(loc := loc-1)>0 and: [(self sourceCode at: loc) isSeparator]]
			whileTrue: [loc := loc-1].
		^ self correctFrom: loc+1 to: loc with: '.'].
	
	self currentSource: (self sourceCode copyReplaceFrom: start to: stop with: aString)
]

{ #category : #accessing }
PolyglotRequestor >> currentSource [

	^ currentSource
]

{ #category : #accessing }
PolyglotRequestor >> currentSource: anObject [

	currentSource := anObject.
]

{ #category : #requests }
PolyglotRequestor >> deselect [
]

{ #category : #'initialize-release' }
PolyglotRequestor >> initialize [
	bindings := Dictionary new
]

{ #category : #requests }
PolyglotRequestor >> nextTokenFrom: start direction: dir [
	"simple token-finder for compiler automated corrections"
	| loc |
	loc := start + dir.
	[(loc between: 1 and: currentSource size) and: [(currentSource at: loc) isSeparator]]
		whileTrue: [loc := loc + dir].
	^ loc
]

{ #category : #requests }
PolyglotRequestor >> notify: aString at: anInteger in: aStream [
	self primitiveThrowSyntaxError: aString at: anInteger in: aStream contents
]

{ #category : #primitives }
PolyglotRequestor >> primitiveThrowSyntaxError: aMessage at: aPosition in: aSource [
	<primitive: 'primitiveThrowSyntaxError' module: 'PolyglotPlugin'>
	self primitiveFailed
]

{ #category : #requests }
PolyglotRequestor >> select [
]

{ #category : #requests }
PolyglotRequestor >> selectFrom: start to: stop [
]

{ #category : #requests }
PolyglotRequestor >> selectInvisiblyFrom: start to: stop [
]

{ #category : #requests }
PolyglotRequestor >> selectionInterval [
	^ 1 to: currentSource size
]

{ #category : #requests }
PolyglotRequestor >> text [
	^ currentSource
]

{ #category : #requests }
PolyglotRequestor >> wantsInteractiveErrorCorrection [
	^ false
]
