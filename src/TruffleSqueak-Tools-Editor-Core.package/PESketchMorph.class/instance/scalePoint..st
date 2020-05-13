accessing
scalePoint: aPoint

	scalePoint := self keepAspectRatio
		ifTrue: [aPoint min: aPoint transposed]
		ifFalse: [aPoint].
	self layoutChanged.