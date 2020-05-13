public
openFile

	| text lines bytesPerRow |
	self fileEntry size > 100000 ifTrue: [^ self].
	
	bytesPerRow := 16.

	"Build the text as a string first (for speed), then apply text attributes"
	text := WriteStream on: (String new: self fileEntry size * 4 + (self fileEntry size // bytesPerRow * 3)).
	data := self fileEntry readBinary.

	1 to: data size by: bytesPerRow do: [:index | | chunk |
		chunk := (data copyFrom: index to: ((index + bytesPerRow - 1) min: data size)) asArray.
		text
			nextPutAll: ((chunk collect: [:byte | self byteTo2DigitHex: byte]) joinSeparatedBy: String tab);
			tab;
			nextPut: $";
			nextPutAll: (self bytesToAscii: chunk);
			nextPut: $";
			lf].
	lines := (data size / bytesPerRow) ceiling.
	self data: (Text string: text contents runs: (RunArray 
		runs: ((1 to: lines * 2) collect: [:i | | bytesInRow | 
			bytesInRow := bytesPerRow.
			i >= (lines * 2 - 1) ifTrue: ["the last row might have fewer bytes"
				bytesInRow := data size \\ bytesPerRow].
			i odd
				ifTrue: [bytesInRow * 3 "2 for hex, one for tab"] 
				ifFalse: [bytesInRow + 2 "quotation marks" + 1 "lf"]])
		values: ((1 to: lines * 2) collect: [:i | i odd 
			ifTrue: [#()]
			ifFalse: [{TextColor color: Color cantaloupe}]])))