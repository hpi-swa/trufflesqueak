statistics
openLocationsHistogram: aType
	| values |
	#primitive = aType
		ifTrue: [ values := self getAllLayouts collect: [ :ea |
					ea getLocations asCollection count: [ :slot | slot isPrimitive wrappedJavaObject ] ] ]
		ifFalse: [ values := self getAllLayouts collect: [ :ea |
				ea getLocations asCollection count: [ :slot | slot isGeneric wrappedJavaObject ] ]].
	HistogramMorph openOn: values