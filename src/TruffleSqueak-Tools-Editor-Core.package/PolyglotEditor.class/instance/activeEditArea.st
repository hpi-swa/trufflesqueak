panes
activeEditArea

	^ self panes 
		ifEmpty: [PEEditAreaNullObject new]
		ifNotEmpty: [self activePane visibleEditArea model]