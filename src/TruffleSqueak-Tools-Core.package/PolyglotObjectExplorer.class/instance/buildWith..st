toolbuilder
buildWith: builder

	| windowSpec treeSpec textSpec buttonSpec buttonOffset tool |
	windowSpec := builder pluggableWindowSpec new.
	windowSpec
		model: self;
		children: OrderedCollection new;
		label: #label;
		extent: self initialExtent.
	
	treeSpec := builder pluggableTreeSpec new.
	treeSpec
		model: self;
		nodeClass: self class nodeClass;
		roots: #getList;
		keyPress: #explorerKey:from:event:;
		getSelected: #currentSelection;
		setSelected: #currentSelection:;
		setSelectedParent: #currentParent:;
		menu: #genericMenu:;
		dragItem: #getDragItem:;
		autoDeselect: false;
		columns: (PolyglotObjectExplorerWrapper showContentsInColumns
			ifTrue: [{
				[:listMorph | (listMorph filteredItems collect: [:item |
					item preferredWidthOfColumn: 1]) max].
				nil "take all the space"}]);
		frame: (0@0 corner: 1@0.71).
	windowSpec children add: treeSpec.

	buttonOffset := (Preferences standardButtonFont widthOfString: 'inspect') * 3/2.

	textSpec := builder pluggableCodePaneSpec new.
	textSpec
		model: self;
		getText: #expression;
		editText: #expression:;
		menu: #codePaneMenu:shifted:;
		help: #helpText;
		frame: (LayoutFrame fractions: (0@0.71 corner: 1@1) offsets: (0@0 corner: buttonOffset negated@0));
		stylerClass: PolyglotTextStyler.
	windowSpec children add: textSpec.

	buttonSpec := builder pluggableButtonSpec new
		model: self;
		label: 'inspect';
		action: #inspectObject;
		help: 'Switch to an inspector tool';
		frame: (LayoutFrame fractions: (1@0.71 corner: 1@1) offsets: (buttonOffset negated@0 corner: 0 @ 0)).
	windowSpec children add: buttonSpec.
	
	tool := builder build: windowSpec.
	self changed: #expandRootsRequested.
	^ tool