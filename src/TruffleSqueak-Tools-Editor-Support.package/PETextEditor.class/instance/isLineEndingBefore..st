typing/selecting keys
isLineEndingBefore: aNumber

	^ (aNumber > 1 and: [self text at: aNumber - 1]) = self lineEndings last