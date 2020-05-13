public
hasActiveTab

	^ self tabs detect: [:each | each state = #active] 
		ifFound: [:tab | true]
		ifNone: [false]