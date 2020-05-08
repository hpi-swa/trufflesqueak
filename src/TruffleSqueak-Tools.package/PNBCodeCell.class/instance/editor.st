accessing
editor
	^ editor ifNil: [ editor := PNBCodeCellEditor for: self ]