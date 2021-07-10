modules-classes
moduleAndClassListIndex: anInteger
	self selectedModuleName: (self moduleAndClassList at: anInteger ifAbsent: [ nil ])