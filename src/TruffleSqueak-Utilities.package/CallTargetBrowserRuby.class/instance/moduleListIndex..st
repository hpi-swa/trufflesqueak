modules
moduleListIndex: anInteger
	self selectedModuleName: (self moduleList at: anInteger ifAbsent: [ nil ])