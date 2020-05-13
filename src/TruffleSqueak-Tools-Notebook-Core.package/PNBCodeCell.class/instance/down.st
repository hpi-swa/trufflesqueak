actions
down
	| index otherCell |
	index := self cellContainer submorphs indexOf: self.
	index >= self cellContainer submorphs size ifTrue: [ ^ self ].
	self remove.
	otherCell := self cellContainer submorphs at: index.
	self cellContainer addMorph: self behind: otherCell.
	self cellContainer changed: #submorphs