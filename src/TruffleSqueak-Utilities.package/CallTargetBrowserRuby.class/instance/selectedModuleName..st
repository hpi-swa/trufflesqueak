accessing
selectedModuleName: anObject

	selectedModuleName := anObject.
	selectedMethodName := nil.
	self changed: #moduleListIndex.
	self changed: #methodList.
	self changed: #relabel.