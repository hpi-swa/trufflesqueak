serialization
loadFromJson: aJsonReadStream
	self resetCells.
	(Json readFrom: aJsonReadStream) cells do: [:cell | self addCodeCellFor: cell ]