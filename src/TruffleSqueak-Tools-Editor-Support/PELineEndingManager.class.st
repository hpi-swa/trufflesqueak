Class {
	#name : #PELineEndingManager,
	#superclass : #Object,
	#category : 'TruffleSqueak-Tools-Editor-Support'
}

{ #category : #detecting }
PELineEndingManager class >> detectLineEndings: aStringOrText [

	| counts zeros |
	counts := Dictionary newFrom: {
		String lf -> 0.
		String cr -> 0.
		String crlf -> 0
	}.
	aStringOrText asString lineIndicesDo: [:start :end :endWithLineEnding | | ending |
		end = endWithLineEnding ifFalse: [
			ending := aStringOrText copyFrom: end + 1 to: endWithLineEnding.
			counts at: ending put: (counts at: ending) + 1]].
	zeros := counts values occurrencesOf: 0.
	zeros < 2 ifTrue: [^ #multiple].
	zeros = 3 ifTrue: [^ #unknown].
	^ self stringToId: (counts associations detect: [:each | each value > 0]) key
]

{ #category : #'id <-> string' }
PELineEndingManager class >> idToString: aSymbol [

	^ (Dictionary newFrom: {
		#lf -> String lf.
		#cr -> String cr.
		#crlf -> String crlf}) at: aSymbol
]

{ #category : #'id <-> string' }
PELineEndingManager class >> stringToId: aString [

	^ self stringToIdDictionary at: aString
]

{ #category : #'id <-> string' }
PELineEndingManager class >> stringToIdDictionary [

	^ Dictionary newFrom: {
		String lf -> #lf.
		String cr -> #cr.
		String crlf -> #crlf}
]

{ #category : #converting }
PELineEndingManager class >> with: aLineEnding string: aStringOrText [

	^ self perform: ((Dictionary newFrom: {
		String cr -> #withCr:.
		String lf -> #withLf:.
		String crlf -> #withCrlf:}) at: aLineEnding) with: aStringOrText
]

{ #category : #converting }
PELineEndingManager class >> withCr: aStringOrText [

	^ aStringOrText withSqueakLineEndings
]

{ #category : #converting }
PELineEndingManager class >> withCrlf: aStringOrText [

	^ aStringOrText withInternetLineEndings
]

{ #category : #converting }
PELineEndingManager class >> withLf: aStringOrText [

	^ aStringOrText withUnixLineEndings
]
