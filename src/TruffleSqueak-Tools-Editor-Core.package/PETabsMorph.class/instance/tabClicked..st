public
tabClicked: aPCTabMorph

	self tabs do: [:each | each state: #unselected] without: aPCTabMorph