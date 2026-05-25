package se.inera.ehds.mapping.tk;

import java.util.List;

/** Maps a RIVTA response type R to a list of FHIR resources of type F. */
public interface TkMapper<R, F> {
    List<F> map(R response, MapperContext context);
}
