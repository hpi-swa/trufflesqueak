what to show
toggleCallTargetInfo
	| wasShowing |
	self okToChange ifTrue:
		[wasShowing := self showingCallTargetInfo.
		self restoreTextualCodingPane.
		self showCallTargetInfo: wasShowing not.
		self setContentsToForceRefetch.
		self contentsChanged]