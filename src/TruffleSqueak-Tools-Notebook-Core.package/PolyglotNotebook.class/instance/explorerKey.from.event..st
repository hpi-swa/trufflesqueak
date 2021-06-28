actions
explorerKey: aChar from: view event: event

	event anyModifierKeyPressed ifFalse: [^ false].

	self currentSelection ifNotNil: [ :obj |
		aChar == $i ifTrue: [obj value inspect. ^ true].
		aChar == $I ifTrue: [obj value explore. ^ true] ].

	^ false