accessing
bufferedImage

	^ bufferedImage ifNil: [
		bufferedImage := GraalSqueakUtilities primitiveFormToBufferedImage: self form ]