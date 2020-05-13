embedded morphs
resizeEmbeddedMorphs

	text embeddedMorphs do: [:each |
		each width: self width - (self margins left + self margins right)]