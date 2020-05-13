menu
menuBar

	| menu |
	menu := MenuMorph new
		layoutFrame: (LayoutFrame fractions: (0@0 extent: 1@1));
		layoutPolicy: TableLayout new;
		listDirection: #leftToRight;
		layoutInset: 3@0;
		removeDropShadow;
		"vResizing: #spaceFill;
		hResizing: #shrinkWrap;
		"stayUp: true.
	
	1 to: 3 do: [:i | menu add: 'Item ', i subMenu: (MenuMorph fromArray: {{'a'. []}. {'b'. []}})].

	^ {menu}