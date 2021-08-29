Class {
	#name : #PELineEndingManagerTest,
	#superclass : #TestCase,
	#category : 'TruffleSqueak-Tools-Editor-Tests'
}

{ #category : #'as yet unclassified' }
PELineEndingManagerTest >> testDetectLineEndings [

	| lm |
	lm := PELineEndingManager.
	{[:x | x]. [:x | x asText]} do: [:transform |
		{String cr. #cr. String lf. #lf. String crlf. #crlf} pairsDo: [:lineEnding :name | self 
			assert: name
			equals: (lm detectLineEndings: (transform value: 'abc', lineEnding, 'def'))].
		self 
			assert: #unknown
			equals: (lm detectLineEndings: (transform value: 'abc')).
		self 
			assert: #multiple
			equals: (lm detectLineEndings: (transform value: 'abc', String cr, 'def', String lf)).
		self 
			assert: #multiple
			equals: (lm detectLineEndings:
				(transform value: 'abc', String cr, 'def', String lf, 'ghi', String cr, 'jkl', String lf))]
]

{ #category : #'as yet unclassified' }
PELineEndingManagerTest >> testWithCr [

	self assert: (PELineEndingManager withCr: String lf) equals: String cr
]

{ #category : #'as yet unclassified' }
PELineEndingManagerTest >> testWithCrlf [

	self assert: (PELineEndingManager withCrlf: String cr) equals: String crlf
]

{ #category : #'as yet unclassified' }
PELineEndingManagerTest >> testWithLf [

	self assert: (PELineEndingManager withLf: String cr) equals: String lf
]
