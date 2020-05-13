panes
editAreaUpdated: aPCEditArea

	aPCEditArea = self activeEditArea ifTrue: [
		self changed: #bottomBarLanguage.
		self changed: #bottomBarLastModified.
		self changed: #bottomBarLineEndings.
		self updateWindowColor]