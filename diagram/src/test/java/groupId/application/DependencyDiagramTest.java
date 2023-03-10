
package groupId.application;

import static java.util.stream.Collectors.toSet;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Copied from
 * https://github.com/Mastercard/flow/blob/main/doc/src/test/java/com/mastercard/test/flow/doc/ModuleDiagramTest.java
 */
@SuppressWarnings( "static-method" )
class DependencyDiagramTest {

	/**
	 * Maps from the groupID to the scope of links that we're interested in within that group.
	 */
	private static final Map<String, Set<String>> SCOPES;
	static {
		Map<String, Set<String>> m = new HashMap<>();
		m.put( "groupId", Stream.of( "compile", "test" ).collect( toSet() ) );
		SCOPES = Collections.unmodifiableMap( m );
	}

	/**
	 * Regenerates the diagram in the main readme
	 *
	 * @throws Exception if insertion fails
	 */
	@Test
	void framework() throws Exception {
		Util.insert( Paths.get( "../README.md" ),
		    "<!-- start_module_diagram:deps -->",
		    s -> diagram( "LR", "groupId" ),
		    "<!-- end_module_diagram -->" );
	}

	private static String diagram( String orientation,
	    String ... groupIDs ) {
		PomData root = new PomData( null, Paths.get( "../pom.xml" ) );

		Set<String> artifacts = new HashSet<>();
		root.visit( pd -> artifacts.add( pd.coords() ) );

		Map<String, List<PomData>> groups = new TreeMap<>();
		root.visit( pd -> groups.computeIfAbsent( pd.groupId(), g -> new ArrayList<>() ).add( pd ) );

		Map<String, List<Link>> links = new HashMap<>();
		root.visit( pd -> pd.dependencies()
		    .filter( dd -> artifacts.contains( dd.coords() ) )
		    .filter( dd -> artifacts.contains( pd.coords() ) )
		    .filter( dd -> SCOPES.get( pd.groupId() ).contains( dd.scope() ) )
		    .forEach( dd -> links
		        .computeIfAbsent( dd.coords(), g -> new ArrayList<>() )
		        .add( new Link(
		            dd.groupId(),
		            dd.artifactId(),
		            "compile".equals( dd.scope() ) ? " -->|compile| " : " -.->|test| ",
		            pd.groupId(),
		            pd.artifactId() ) ) ) );

		StringBuilder mermaid = new StringBuilder( "```mermaid\ngraph " )
		    .append( orientation )
		    .append( "\n" );

		for ( String groupID : groupIDs ) {
			mermaid.append( "  subgraph " ).append( groupID ).append( "\n" );
			groups.get( groupID )
			    .stream()
			    .sorted( Comparator.comparing( PomData::artifactId ) )
			    .forEach( pom -> links.getOrDefault( pom.coords(), Collections.emptyList() ).stream()
			        .filter( Link::intraGroup )
			        .forEach( l -> mermaid.append( "  " ).append( l ) ) );
			mermaid.append( "  end\n" );
		}

		mermaid.append( "```" );
		return mermaid.toString();
	}

	private static class Link {

		private final String fromGroup;
		private final String fromArtifact;
		private final String type;
		private final String toGroup;
		private final String toArtifact;

		protected Link( String fromGroup, String fromArtifact, String type, String toGroup,
		    String toArtifact ) {
			this.fromGroup = fromGroup;
			this.fromArtifact = fromArtifact;
			this.type = type;
			this.toGroup = toGroup;
			this.toArtifact = toArtifact;
		}

		public boolean intraGroup() {
			return fromGroup.equals( toGroup );
		}

		@Override
		public String toString() {
			return "  " + fromArtifact + type + toArtifact + "\n";
		}
	}
}
