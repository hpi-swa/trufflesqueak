actions
up
	| index otherCell |
	index := self cellContainer submorphs indexOf: self.
	index <= 1 ifTrue: [ ^ self ].
	self remove.
	otherCell := self cellContainer submorphs at: index - 1.
	self cellContainer addMorph: self inFrontOf: otherCell.
	self cellContainer changed: #submorphs