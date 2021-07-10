accessing
selectedModuleName: anObject

	selectedModuleName := anObject.
	selectedMethodName := nil.
	self changed: #moduleAndClassListIndex.
	self changed: #methodList.
	self changed: #relabel.