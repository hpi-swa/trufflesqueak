public
convertLineEndings: newLineEndings

	| selection lineEndingsBeforeSelection lineEndingsInSelection sizeDifference |
	lineEndingsBeforeSelection := 0.
	lineEndingsInSelection := 0.
	self unsavedCode asString lineIndicesDo: [:start :end :endWithLineEnding |
		(endWithLineEnding < self selection start and: endWithLineEnding < self selection stop)
			ifTrue: [lineEndingsBeforeSelection := lineEndingsBeforeSelection + 1].
		(self selection includes: end + 1)
			ifTrue: [lineEndingsInSelection := lineEndingsInSelection + 1]].
	
	sizeDifference := newLineEndings size - self lineEndings size.

	selection := self selection.
	selection := (selection start + (sizeDifference * lineEndingsBeforeSelection)) 
		to: selection stop + (sizeDifference * (lineEndingsBeforeSelection + lineEndingsInSelection)).
	self unsavedCode: (PELineEndingManager with: newLineEndings string: self unsavedCode).
	self changed: #unsavedCode.
	self selection: selection.