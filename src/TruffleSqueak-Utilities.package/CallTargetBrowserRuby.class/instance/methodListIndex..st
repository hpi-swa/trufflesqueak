methods
methodListIndex: anInteger
	self selectedMethodName: (self methodList at: anInteger ifPresent: [ :t | t asString ] ifAbsent: [ nil ])