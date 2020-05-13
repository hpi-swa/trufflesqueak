event handling
mouseLeave: event

	| result |
	result := super mouseLeave: event.
	self notifySelectionChanged.
	^ result