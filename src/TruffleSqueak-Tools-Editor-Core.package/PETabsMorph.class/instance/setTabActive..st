public
setTabActive: aNumber

	self tabs withIndexDo: [:each :index |
		index = aNumber
			ifTrue: [
				each state: #active.
				scrollPane scrollToShow: (each position x@0 extent: each width@0)] 
			ifFalse: [each state: #unselected]]