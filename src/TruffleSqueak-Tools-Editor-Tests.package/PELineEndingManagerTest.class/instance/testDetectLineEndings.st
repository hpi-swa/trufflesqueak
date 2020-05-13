as yet unclassified
testDetectLineEndings

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