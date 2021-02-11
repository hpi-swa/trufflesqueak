converting
asString
	| label separator |
	label := self itemName ifNil: [''].
	(label includes: Character cr)
		ifTrue: [label := label withSeparatorsCompacted].
	 
	separator := self class showContentsInColumns
		ifTrue: [String tab]
		ifFalse: [label ifEmpty: [''] ifNotEmpty: [': ']].

	^ '{1}{2}{3} in total' format: {label. separator. self members size}