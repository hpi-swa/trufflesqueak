toolbuilder
buildSidebar: builder
	| panelSpec treeSpec |

	panelSpec := builder pluggablePanelSpec new
		"layout: #vertical;"
		children: OrderedCollection new;
		yourself.
	
	treeSpec := builder pluggableTreeSpec new.
	treeSpec
		model: self;
		nodeClass: ObjectExplorerWrapper;
		roots: #getList;
		keyPress: #explorerKey:from:event:;
		getSelected: #currentSelection;
		"setSelected: #currentSelection:;"
		"setSelectedParent: #currentParent:;"
		menu: #genericMenu:;
		dragItem: #getDragItem:;
		columns: (ObjectExplorerWrapper showContentsInColumns
			ifTrue: [{
				[:listMorph | (listMorph filteredItems collect: [:item |
					item preferredWidthOfColumn: 1]) max].
				nil "take all the space"}]);
		autoDeselect: false;
		frame: (LayoutFrame
			fractions: (0@0 corner: 1@1)
			offsets: (0@ (self buttonHeight + 2) corner: 0@0)).
	panelSpec children addAll: {self buildSwitchesWith: builder. treeSpec}.	
	^ panelSpec