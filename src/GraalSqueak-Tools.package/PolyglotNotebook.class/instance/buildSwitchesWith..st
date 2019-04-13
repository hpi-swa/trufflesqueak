toolbuilder
buildSwitchesWith: builder
	"Build the instance/comment/class switch"

	| panelSpec r a l s |
	panelSpec := builder pluggablePanelSpec new
		layout: #horizontal;
		spacing: -1;
		children: OrderedCollection new;
		yourself.

	r := builder pluggableButtonSpec new.
	r 
			model: self;
			"horizontalResizing: #shrinkWrap;"
			label: 'Run all';
			help: 'Run all code cells'; 
			action: #actionRunAll.

	a := builder pluggableButtonSpec new.
	a 
			model: self;
			label: 'Add cell';
			help: 'Add a new code cell';
			action: #actionAddCell.
			
	l := builder pluggableButtonSpec new.
	l 
			model: self;
			label: 'Load';
			help: 'Load notebook from .ipynb file';
			action: #actionLoad.
	s := builder pluggableButtonSpec new.
	s 
			model: self;
			label: 'Save';
			help: 'Save notebook in .ipynb file';
			action: #actionSave.
			
	panelSpec children addAll: {
		r.
		"builder pluggableSpacerSpec new."
		a.
		l.
		s.
	}.
	
	panelSpec frame: (LayoutFrame new
		leftFraction: 0 offset: 0;
		topFraction: 0 offset: 0;
		rightFraction: 1 offset: 0;
		bottomFraction: 0 offset: self buttonHeight).

	^panelSpec