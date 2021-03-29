menu commands
mainFieldListMenu: aMenu
	"Arm the supplied menu with items for the field-list of the receiver"

	aMenu addStayUpItemSpecial.

	aMenu addList: #(
		('inspect (i)'						inspectSelection)
		('explore (I)'						exploreSelection)
		('basic inspect'						inspectSelectionBasic)
		('inspect VM object'					inspectVMObject)).

	(#(self ellipsis element nil) includes: self typeOfSelection)
		ifTrue: [self addCollectionItemsTo: aMenu].

	self addFieldItemsTo: aMenu.
	
	aMenu addLine.

	Polyglot availableLanguages do: [ :ea |
		aMenu
			add: 'inspect as ', ea getName
			target: self
			selector: #inspectAs:
			argument: ea ].

	^ aMenu
