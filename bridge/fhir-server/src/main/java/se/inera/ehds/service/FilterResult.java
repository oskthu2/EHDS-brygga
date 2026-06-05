package se.inera.ehds.service;

import java.util.List;

/** Resultat från SparrFilterService: filtrerade poster + huruvida fail-closed tillämpades. */
public record FilterResult<T>(List<T> entries, boolean failClosed) {}
