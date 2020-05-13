private
removePreviousAnnotationMorphs: aText

	| offset cleanedText |
	cleanedText := aText.
	offset := 0.
	aText runs withStartStopAndValueDo: [:start :stop :attributes | attributes
		select: [:each | each isKindOf: TextAnchor]
		thenDo: [:each | (each anchoredMorph model isKindOf: PECodeBox) ifFalse: [
				cleanedText := cleanedText replaceFrom: start + offset to: stop + offset with: ''.
				offset := offset - 1.]]]