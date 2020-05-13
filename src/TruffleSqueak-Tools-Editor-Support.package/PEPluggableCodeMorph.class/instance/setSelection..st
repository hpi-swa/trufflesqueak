model access
setSelection: sel

	| result |
	result := super setSelection: sel.
	self notifySelectionChanged.
	^ result