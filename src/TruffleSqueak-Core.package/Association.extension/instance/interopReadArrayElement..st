*TruffleSqueak-Core-arrays
interopReadArrayElement: index
	^ index caseOf: { [ 0 ] -> [ key ]. [ 1 ] -> [ value ] }
		otherwise: [ self error: 'Unexpected index:', index asString ]