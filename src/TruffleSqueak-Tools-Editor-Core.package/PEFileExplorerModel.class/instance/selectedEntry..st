accessing
selectedEntry: anObject
	selectedEntry := anObject.
	"Trigger an event for the code editor to notify it about a newly selected file."
	self triggerEvent: #selectionChanged with: selectedEntry.
	"Trigger an event for ToolBuilder so that the selection is highlighted in blue."
	self changed: #selectedEntry