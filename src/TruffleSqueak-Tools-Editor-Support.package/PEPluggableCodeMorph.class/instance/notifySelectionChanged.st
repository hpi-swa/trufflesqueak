model access
notifySelectionChanged

	self setSelectionSelector ifNotNil: [:selector | 
		self model perform: selector with: selectionInterval]