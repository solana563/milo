package com.ubayanda.milonative.crossmix

/** how songs hand over to each other (from Mochy Music's constants, GPL-3.0) */
enum class CrossmixMode {
    AUTO_CROSSFADE,
    OFF,
    MANUAL_CROSSFADE,
    CROSSMIX,
}

enum class CrossmixLength(val scale: Float) {
    SHORT(0.6f),
    MEDIUM(1f),
    LONG(1.5f),
}

/** how far the slower song may be sped up to meet the other; off still locks two songs already at one tempo */
enum class CrossmixTempoMatch(val maxSpeedUp: Float) {
    OFF(0.006f),
    GENTLE(0.04f),
    FULL(0.10f),
}

/** the crossmix effect layered over a pause, play or skip fade */
enum class TransportEffect(val label: String) {
    OFF("None"),
    ECHO("Echo"),
    REWIND("Rewind"),
    TAPE_STOP("Tape stop"),
    FAR_AWAY("Fade to far"),
    REVERB("Reverb wash"),
    FILTER("Filter sweep"),
}
