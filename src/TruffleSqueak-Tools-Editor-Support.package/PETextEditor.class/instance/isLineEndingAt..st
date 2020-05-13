typing/selecting keys
isLineEndingAt: aNumber

	^ (aNumber <= self text size and: [self text at: aNumber]) = self lineEndings first