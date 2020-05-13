layout
minExtent

	autoHeight ifFalse: [^ super minExtent].
	self flag: #todo. "Adding +2 removes the scroll bar but causes weird problems."
	^ super minExtent x@(textMorph extent y" + 2")