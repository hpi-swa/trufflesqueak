editor functions
openFile: aPCFileEntry

	(aPCFileEntry notNil and: [aPCFileEntry isFile]) ifTrue: [
		self panes
			ifEmpty: [
				self addPane.
				self panes first openFile: aPCFileEntry]
			ifNotEmpty: [self activePane openFile: aPCFileEntry]]