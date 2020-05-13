as yet unclassified
assertCodeBoxAt: anInteger in: aText

	| attributes codeBox textAnchor |
	attributes := aText attributesAt: anInteger.
	self assert: 1 equals: attributes size.
	textAnchor := attributes first.
	self assert: (textAnchor isKindOf: TextAnchor).
	self assert: (textAnchor anchoredMorph model isKindOf: PECodeBox).
	codeBox := textAnchor anchoredMorph model.
	self assert: codeBox collapsed not.
	^ codeBox