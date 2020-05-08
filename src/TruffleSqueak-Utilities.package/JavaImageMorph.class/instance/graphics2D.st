accessing
graphics2D

	^ graphics2D ifNil: [
		graphics2D := self bufferedImage getGraphics.
		graphics2D setBackground: (Java type: 'java.awt.Color') white.
		graphics2D ]