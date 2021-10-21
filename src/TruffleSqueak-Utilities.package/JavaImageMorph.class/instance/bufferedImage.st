accessing
bufferedImage

	^ bufferedImage ifNil: [
		bufferedImage := TruffleSqueakUtilities formToBufferedImage: self form ]