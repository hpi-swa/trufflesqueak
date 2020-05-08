accessing
cellContainer

	^ cellContainer ifNil: [ cellContainer := PNBCodeCellContainer for: self ]