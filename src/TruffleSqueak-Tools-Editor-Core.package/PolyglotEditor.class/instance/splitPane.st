panes
splitPane

	| fileEntry |
	self hasActiveEditArea ifFalse: [^ self].
	fileEntry := self activeEditArea fileEntry.
	self panes do: [:each | each setAllTabsToNonActive].
	
	self addPane.
	panes last openFile: fileEntry