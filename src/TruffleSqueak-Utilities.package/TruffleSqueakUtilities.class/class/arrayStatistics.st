statistics
arrayStatistics
	| arrayTypes total |
	arrayTypes := #(0 0 0 0 0 0).
	(Array allInstances collect: [ :ea | (self primitiveGetVMObject: ea) ]) do: [ :ea |
		ea isEmptyType ifTrue: [ arrayTypes at: 1 put: (arrayTypes at: 1) + 1 ].
		ea isBooleanType ifTrue: [ arrayTypes at: 2 put: (arrayTypes at: 2) + 1 ].
		ea isCharType ifTrue: [ arrayTypes at: 3 put: (arrayTypes at: 3) + 1 ].
		ea isLongType ifTrue: [ arrayTypes at: 4 put: (arrayTypes at: 4) + 1 ].
		ea isDoubleType ifTrue: [ arrayTypes at: 5 put: (arrayTypes at: 5) + 1 ].
		ea isObjectType ifTrue: [ arrayTypes at: 6 put: (arrayTypes at: 6) + 1 ] ].
	total := arrayTypes sum.

	^ '# Array Statistics
	empty: {1} ({8}%)
	bool: {2} ({9}%)
	char: {3} ({10}%)
	long: {4} ({11}%)
	double: {5} ({12}%)
	Object: {6} ({13}%)
	Total: {7}
	' format: (arrayTypes, {total},
		(arrayTypes collect: [ :ea | (ea / total asFloat) * 100 printShowingMaxDecimalPlaces: 4 ]))