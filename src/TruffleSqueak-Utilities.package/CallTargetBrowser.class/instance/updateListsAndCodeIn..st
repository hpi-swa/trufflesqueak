self-updating
updateListsAndCodeIn: aWindow
	super updateListsAndCodeIn: aWindow.
	self showingCallTargetInfo ifTrue: [
		self setContentsToForceRefetch.
		self contentsChanged ]