panes
panesChanged

	self panes withIndexDo: [:each :i | each 
		frameFractions: ((i - 1 * (1 / panes size))@0 extent: (1 / panes size)@1)].
	self changed: #panes