edit areas
removeEditArea: aMorph

	| nextEditArea |
	nextEditArea := editAreas after: aMorph ifAbsent: [
		editAreas before: aMorph ifAbsent: [nil]].
	editAreas remove: aMorph.
	editArea = aMorph ifTrue: [
		editArea delete.
		editArea := nil.
		nextEditArea ifNotNil: [self makeEditAreaActive: nextEditArea]].
	editAreas ifEmpty: [self triggerEvent: #closed with: self]