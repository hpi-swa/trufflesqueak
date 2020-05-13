public
buildWith: aBuilder

	^ aBuilder build: (PEPluggablePanelSpec new
		model: self;
		onClick: [self triggerEvent: #focus];
		children: ((self class includesSelector: #buildChildrenWith:)
			ifTrue: [self buildChildrenWith: aBuilder] 
			ifFalse: [#children]))