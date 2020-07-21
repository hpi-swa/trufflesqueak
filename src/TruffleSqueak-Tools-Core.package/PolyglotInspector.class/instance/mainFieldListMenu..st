menu commands
mainFieldListMenu: aMenu
	"Arm the supplied menu with items for the field-list of the receiver"

	aMenu addStayUpItemSpecial.

	aMenu addList: #(
		('inspect (i)'						inspectSelection)
		('explore (I)'						exploreSelection)).

	self addCollectionItemsTo: aMenu.

	aMenu addList: #(
		-
		('copy name (c)'					copyName)		
		('basic inspect'						inspectBasic)).

	^ aMenu
