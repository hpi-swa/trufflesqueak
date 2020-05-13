public
setAllTabsToNonActive

	self tabs do: [:each | each state = #active ifTrue: [each state: #selected]]