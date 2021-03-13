accessing
selectedMethodName: anObject

	selectedMethodName := anObject.
	self changed: #methodListIndex.
	self changed: #callTargetInfo.