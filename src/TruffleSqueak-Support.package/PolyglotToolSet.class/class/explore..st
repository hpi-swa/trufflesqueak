inspecting
explore: anObject
	^ anObject isForeignObject
		ifTrue: [ PolyglotObjectExplorer openOn: anObject ]
		ifFalse: [ super explore: anObject ]