accessing
output

	^ output ifNil: [
		output := PNBCodeCellOutput new.
		self addMorphBack: output.
		self changed.
		output ]