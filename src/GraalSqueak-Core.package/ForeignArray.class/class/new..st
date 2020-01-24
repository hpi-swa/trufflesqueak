instance creation
new: sizeRequested
	sizeRequested = 0
		ifTrue: [ ^ self on: #() ]
		ifFalse: [ ^ self error: 'Instances can only be created from ForeignObjects.' ]