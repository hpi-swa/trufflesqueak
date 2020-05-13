querying
forId: id

	^ self all detect: [:each | each id = id]