statistics
layoutStatistics
	| allLocations allLocationsSizes allLocationsTypes totalLocations |
	allLocations := (((Class allSubInstances select: [ :ea | ea instSpec = 1]) "All PointersObjects"
		collect: [ :ea | ea vmObject ]) select: [ :ea | ea hasLayout ] )
		collect: [ :ea | ea getLayout getLocations ].
	allLocationsSizes := allLocations collect: [ :ea | ea size ].
	allLocationsTypes := #(0 0 0 0 0 0 0 0 0).
	allLocations do: [ :ea |
		ea asCollection do: [ :slot |
			slot isUninitialized ifTrue: [ allLocationsTypes at: 1 put: (allLocationsTypes at: 1) + 1 ].
			slot isPrimitive ifTrue: [ allLocationsTypes at: 2 put: (allLocationsTypes at: 2) + 1 ].
			slot isBool ifTrue: [ allLocationsTypes at: 3 put: (allLocationsTypes at: 3) + 1 ].
			slot isChar ifTrue: [ allLocationsTypes at: 4 put: (allLocationsTypes at: 4) + 1 ].
			slot isLong ifTrue: [ allLocationsTypes at: 5 put: (allLocationsTypes at: 5) + 1 ].
			slot isDouble ifTrue: [ allLocationsTypes at: 6 put: (allLocationsTypes at: 6) + 1 ].
			slot isGeneric ifTrue: [ allLocationsTypes at: 7 put: (allLocationsTypes at: 7) + 1 ].
			slot isExtension ifTrue: [
				slot isPrimitive
					ifTrue: [ allLocationsTypes at: 8 put: (allLocationsTypes at: 8) + 1 ]
					ifFalse: [ allLocationsTypes at: 9 put: (allLocationsTypes at: 9) + 1 ] ] ] ].
	totalLocations := (allLocationsTypes at: 1) + (allLocationsTypes at: 2) + (allLocationsTypes at: 7).

	^ '# Object Layout Statistics
	Uninitialized: {1} ({11}%)
	Primitive: {2} ({12}%) [extensions: {8}]
	Primitive Types: {3} bool, {4} char, {5} long, {6} double
	Generic: {7} ({13}%) [extensions: {9}]
	Total: {10}
	
	Average number of slots per class: {14}
	Max number of slots per class: {15}
	' format: (allLocationsTypes, {
		totalLocations.
		((allLocationsTypes at: 1) / totalLocations asFloat) * 100 printShowingMaxDecimalPlaces: 4.
		((allLocationsTypes at: 2) / totalLocations asFloat) * 100 printShowingMaxDecimalPlaces: 4.
		((allLocationsTypes at: 7) / totalLocations asFloat) * 100 printShowingMaxDecimalPlaces: 4.
		allLocationsSizes average asFloat printShowingMaxDecimalPlaces: 4.
		allLocationsSizes max})