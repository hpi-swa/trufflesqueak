self-updating
updateListsAndCodeIn: aWindow
	super updateListsAndCodeIn: aWindow.
	(self showingCallTargetInfo or: [ self showCoverage ]) ifTrue: [
		self setContentsToForceRefetch.
		self contentsChanged ]