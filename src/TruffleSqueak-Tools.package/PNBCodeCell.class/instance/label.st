accessing
label
	^ label ifNil: [ label := PNBCodeCellLabel for: self ]