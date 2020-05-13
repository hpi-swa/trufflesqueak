editor access
scrollSelectionIntoView: event

	| result |
	result := super scrollSelectionIntoView: event.
	self notifySelectionChanged.
	^ result