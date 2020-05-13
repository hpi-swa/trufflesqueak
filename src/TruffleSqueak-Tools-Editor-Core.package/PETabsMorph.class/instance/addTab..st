public
addTab: aString

	| tab |
	tab := PETabMorph title: aString.
	tab
		when: #closed send: #updateScrollbars to: self;
		when: #clicked send: #tabClicked: to: self with: tab.
	tabHolder addMorphBack: tab.
	self updateScrollbars.
	^ tab