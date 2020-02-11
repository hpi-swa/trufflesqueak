converting
asString
	| label separator |
	label := self itemName ifNil: [''].
	(label includes: Character cr)
		ifTrue: [label := label withSeparatorsCompacted].
	 
	separator := self class showContentsInColumns
		ifTrue: [String tab]
		ifFalse: [label ifEmpty: [''] ifNotEmpty: [': ']].

	^ '{1}{2}{3} members in total' format: {label.separator.Interop getMemberSize: self object}