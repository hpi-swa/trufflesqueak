testing
stepIn: aSystemWindow
	self selectedModuleName ifNotNil: [
		self changed: #methodList.
		self selectedMethodName ifNotNil: [
			self changed: #callTargetInfo ] ]