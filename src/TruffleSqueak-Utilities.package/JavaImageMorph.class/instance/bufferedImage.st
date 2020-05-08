accessing
bufferedImage

	^ bufferedImage ifNil: [
		bufferedImage := TruffleSqueakUtilities primitiveFormToBufferedImage: self form ]