initialize-release
initialize

	super initialize.
	panes := OrderedCollection new.
	self	exportedVariables: OrderedCollection new.
	
	self rootDirectories: (OrderedCollection newFrom: {PEFileEntryFSDirectory on: FileDirectory default}, self gitDirectories)