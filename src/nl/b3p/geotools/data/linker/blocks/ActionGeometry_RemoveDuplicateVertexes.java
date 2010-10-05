/**
 * $Id$
 */

package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nl.b3p.geotools.data.linker.feature.EasyFeature;

/**
 * Deze Action verwijdert uit lijnen en polygonen dubbele opeenvolgende vertexen
 * die soms eens voorkomen in bronbestanden.
 *
 * Bijvoorbeeld POLYGON(A B C C C D E A) -> POLYGON(A B C D E A)
 *
 * POLYGON(A B C B D A) blijft hetzelfde
 *
 * Hiermee kan worden voorkomen dat ArcGIS hierop crasht.
 * @author matthijsln
 */
public class ActionGeometry_RemoveDuplicateVertexes extends Action {

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();
        constructors.add(Collections.EMPTY_LIST);
        return constructors;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {

        Object geometry = feature.getFeature().getDefaultGeometry();

        if(geometry instanceof Polygon) {
            Polygon p = removeDuplicateVertexes((Polygon)geometry);

            if(p != null) {
                feature.getFeature().setDefaultGeometry(p);
            }
        } else if(geometry instanceof MultiPolygon) {
            MultiPolygon mp = (MultiPolygon)removeDuplicateVertexesMultiGeometry((MultiPolygon)geometry);

            if(mp != null) {
                feature.getFeature().setDefaultGeometry(mp);
            }
        } else if(geometry instanceof LineString) {
            LineString l = removeDuplicateVertexes((LineString)geometry);

            if(l != null) {
                feature.getFeature().setDefaultGeometry(l);
            }
        } else if(geometry instanceof MultiLineString) {
            MultiLineString ml = (MultiLineString)removeDuplicateVertexesMultiGeometry((MultiLineString)geometry);

            if(ml != null) {
                feature.getFeature().setDefaultGeometry(ml);
            }
        }

        return feature;
    }
    
    private static GeometryCollection removeDuplicateVertexesMultiGeometry(GeometryCollection gc) {
        boolean isChanged = false;
        
        Geometry[] geometries = new Geometry[gc.getNumGeometries()];
        for(int i = 0; i < geometries.length; i++) {
            Geometry g = gc.getGeometryN(i);
            geometries[i] = g;

            if(g instanceof Polygon) {
                g = removeDuplicateVertexes((Polygon)g);
            } else {
                g = removeDuplicateVertexes((LineString)g);
            }
            if(g != null) {
                isChanged = true;
                geometries[i] = g;
            }
        }

        if(isChanged) {
            if(gc instanceof MultiPolygon) {
                Polygon[] polys = new Polygon[geometries.length];
                System.arraycopy(geometries, 0, polys, 0, geometries.length);
                return gc.getFactory().createMultiPolygon(polys);
            } else {
                LineString[] lines = new LineString[geometries.length];
                System.arraycopy(geometries, 0, lines, 0, geometries.length);
                return gc.getFactory().createMultiLineString(lines);
            }
        } else {
            return null;
        }
    }

    private static Polygon removeDuplicateVertexes(Polygon p) {
        LinearRing exterior = removeDuplicateVertexes((LinearRing)p.getExteriorRing());

        LinearRing[] interiorRings = new LinearRing[p.getNumInteriorRing()];
        boolean interiorRingChanged = false;
        for(int i = 0; i < interiorRings.length; i++) {
            interiorRings[i] = removeDuplicateVertexes((LinearRing)p.getInteriorRingN(i));
            interiorRingChanged = interiorRingChanged | (interiorRings[i] != null);
        }

        if(interiorRingChanged || (exterior != null)) {
            if(exterior == null) {
                exterior = (LinearRing)p.getExteriorRing();
            }
            for(int i = 0; i < interiorRings.length; i++) {
                if(interiorRings[i] == null) {
                    interiorRings[i] = (LinearRing)p.getInteriorRingN(i);
                }
            }
            return p.getFactory().createPolygon(exterior, interiorRings);
        }
        return null;
    }

    private static LineString removeDuplicateVertexes(LineString line) {
        Coordinate[] coords = removeDuplicateVertexes(line.getCoordinates());
        if(coords == null) {
            return null;
        } else {
            return line.getFactory().createLineString(coords);
        }
    }

    private static LinearRing removeDuplicateVertexes(LinearRing ring) {
        Coordinate[] coords = removeDuplicateVertexes(ring.getCoordinates());
        if(coords == null) {
            return null;
        } else {
            return ring.getFactory().createLinearRing(coords);
        }
    }

    /**
     * Returnt NULL indien geen duplicate vertexen gevonden
     */
    private static Coordinate[] removeDuplicateVertexes(Coordinate[] vertices) {
        if(vertices.length < 3) {
            return null;
        }

        int i = 1;

        while(i < vertices.length) {
            if(vertices[i].equals(vertices[i-1])) {
                break;
            }
            i++;
        }

        if(i == vertices.length) {
            /* Geen duplicaten gevonden */
            return null;
        }

        Coordinate c = vertices[i];

        int j = i;

        i++;
        do {
            while(i < vertices.length && vertices[i].equals(c)) {
                i++;
            }
            if(i == vertices.length) {
                break;
            }
            c = vertices[i++];
            vertices[j++] = c;
        } while(i < vertices.length);

        Coordinate[] fixed = new Coordinate[j];
        System.arraycopy(vertices, 0, fixed, 0, j);

        return fixed;
    }

    /* Evt voor in JUnit test class
     *
     * Test WKTs:
     * 
     *
LINESTRING (0 1, 0 1, 0 2, 0 3)

LINESTRING (1 1, 1 2, 1 2)

LINESTRING (2 1, 2 2, 2 3)

LINESTRING (3 1, 3 2, 3 2, 3 3)

LINESTRING (4 1, 4 2, 4 2, 4 2, 4 3, 4 4)

MULTILINESTRING ((5 1, 5 2, 5 2, 5 3, 5 3, 5 4, 5 4, 5 4),
  (6 1, 6 2, 6 3, 6 3, 6 3, 6 3, 6 3))

POLYGON ((0 1, 0 1, 0 2, 0 3, 0.5 3, 0 1))

POLYGON ((1 1, 1 2, 1 2, 1.5 2, 1 1))

POLYGON ((2 1, 2 2, 2 3, 2.5 3, 2 1))

POLYGON ((3 1, 3 2, 3 2, 3 3, 3.5 3, 3 1))

POLYGON ((4 1, 4 2, 4 2, 4 2, 4 3, 4 4, 4.5 4, 4 1))

POLYGON ((5 1, 8 1, 8 4, 8 4, 5 4, 5 4, 5 1),
  (6 2, 7 2, 7 2, 7 3, 6 3, 6 2))



    static final Coordinate A = new Coordinate(1, 1);
    static final Coordinate B = new Coordinate(2, 2);
    static final Coordinate C = new Coordinate(3, 3);
    static final Coordinate D = new Coordinate(4, 4);

    static final Coordinate[] testPoints = new Coordinate[] { A, B, C, D };

    public static void main(String[] args) {
        Object[] tests = new Object[] {
            new Object[] {
                new Coordinate[] { A, A, B, C }, // input
                new Coordinate[] { A, B, C }  // verwachte output
            },
            new Object[] {
                new Coordinate[] { A, B, B }, // input
                new Coordinate[] { A, B }  // verwachte output
            },
            new Object[] {
                new Coordinate[] { A, B, C }, // input
                null  // verwachte output
            },
            new Object[] {
                new Coordinate[] { A, B, B, C }, // input
                new Coordinate[] { A, B, C }  // verwachte output
            },
            new Object[] {
                new Coordinate[] { A, B, B, B, C, D}, // input
                new Coordinate[] { A, B, C, D }  // verwachte output
            },
            new Object[] {
                new Coordinate[] { A, B, B, C, C, D, D, D}, // input
                new Coordinate[] { A, B, C, D }  // verwachte output
            },
            new Object[] {
                new Coordinate[] { A, B, C, C, C, C, C }, // input
                new Coordinate[] { A, B, C }  // verwachte output
            }
        };

        for(Object t: tests) {
            Object[] test = (Object[])t;
            System.out.println("Test met input " + printTest(test[0]));
            Coordinate[] output = removeDuplicateVertexes((Coordinate[])test[0]);
            if(test[1] == null) {
                if(output == null) {
                    System.out.println("Verwachte output is: ongewijzigd, resultaat: ongewijzigd. TEST OK");
                } else {
                    System.out.println("Verwachte output is: ongewijzigd, resultaat: " + printTest(output) + ". TEST NIET OK");
                }
            } else {
                if(!Arrays.equals(output, (Object[])test[1])) {
                    System.out.println("Verwachte output is: " + printTest(test[1]) + ", resultaat NIET OK: " + printTest(output));
                } else {
                    System.out.println("Verwachte output is: " + printTest(test[1]) + ", resultaat komt overeen, TEST OK");
                }
            }
        }
    }

    private static String printTest(Object c) {
        Coordinate vx[] = (Coordinate[])c;

        String s = "( ";
        for(int i = 0; i < vx.length; i++) {
            if(i > 0) {
                s += ", ";
            }
            Coordinate v = vx[i];
            s += (char)('A' + ArrayUtils.indexOf(testPoints, v));

        }
        s += " )";
        return s;
    }*/

    @Override
    public String toString() {
        return "remove duplicate vertexes";
    }

    @Override
    public String getDescription_NL() {
        return "Verwijder uit lijnen en vlakken dubbele opeenvolgende vertexen";
    }

}
