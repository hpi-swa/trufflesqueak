geometry
extent: aPoint

	autoHeight
		ifTrue: [	super extent: aPoint x@self minExtent y.
			self layoutChanged. owner layoutChanged] 
		ifFalse: [super extent: aPoint]