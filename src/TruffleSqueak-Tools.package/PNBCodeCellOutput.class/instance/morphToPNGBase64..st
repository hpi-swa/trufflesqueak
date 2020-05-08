actions
morphToPNGBase64: aMorph
	| stream |
	(aMorph isKindOf: PNBImageMorph)
		ifTrue: [ stream := (FileStream readOnlyFileNamed: aMorph fileName) binary ];
		ifFalse: [
			stream := ReadWriteStream with: #().	
			PNGReadWriter putForm: aMorph imageForm onStream: stream].
	^ (Base64MimeConverter mimeEncode: stream multiLine: false) contents