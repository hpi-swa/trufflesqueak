polyglot
codeForPredeclaringVariables: aCollection

	aCollection ifEmpty: [^ ''].
	^ '| ', (aCollection joinSeparatedBy: ' '), ' |'