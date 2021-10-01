testing
stepIn: aSystemWindow
	self changed: #moduleAndClassList.
	self selectedModuleName ifNotNil: [
		self changed: #methodList.
		self selectedMethodName ifNotNil: [
			self changed: #callTargetInfo ] ]