private
generateOneLineCommentsFor: aString startingWith: anotherString

	| result |
	result := ''.
	aString lineIndicesDo: [:start :end :endWithDelimiter | result := result, anotherString, (aString copyFrom: start to: endWithDelimiter)].
	^ result