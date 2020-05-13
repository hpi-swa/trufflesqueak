code actions
addCodeBoxes: aString

	| result |
	result := PECodeBoxReadWriter addCodeBoxesTo: aString file: self fileEntry.
	result second do: [:each | self setupCodeBoxEventsFor: each].
	^ result first