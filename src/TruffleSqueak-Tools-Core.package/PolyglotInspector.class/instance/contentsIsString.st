selecting
contentsIsString

	^ (selectionIndex = 0) | (selectionIndex between: 2 and: (1 + self optionalFieldList size))