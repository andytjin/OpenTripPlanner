/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.common.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class CompactLineStringTest extends TestCase {

    private List<Coordinate> coordinateList = new ArrayList<Coordinate>();;
    private GeometryFactory geometryFactory = new GeometryFactory();
    private double x0 = 1.111111111;
    private double y0 = 0.123456789;
    private double x1 = 2.0;
    private double y1 = 0.0;

    private LineString lineString = makeGeometryLineString(new Coordinate[0]);
    private int[] coordsArray = CompactLineString.compactLineString(x0, y0, x1, y1, lineString, false);
    private LineString lineString2 = CompactLineString.uncompactLineString(x0, y0, x1, y1, coordsArray, false);
    private byte[] packedCoords = CompactLineString.compackLineString(x0, y0, x1, y1, lineString, false);
    private LineString lsi = (LineString) lineString.reverse(); // The expected output;
    private int[] coordsArrayReverse = CompactLineString.compactLineString(x1, y1, x0, y0, lineString, true);
    private byte[] packedCoordsArrayReverse = CompactLineString.compackLineString(x1, y1, x0, y0, lineString, true);


    @Test
    public final void testcompactStraightLineAndPacked() {
        addCoordinates(new Coordinate(x0, y0),new Coordinate(x1, y1));

        assertTrue(coordsArray == CompactLineString.STRAIGHT_LINE); // ==, not equals

        assertTrue(lineString.equalsExact(lineString2, 0.00000015));

        assertTrue(packedCoords == CompactLineString.STRAIGHT_LINE_PACKED); // ==, not equals

        lineString2 = CompactLineString.uncompackLineString(x0, y0, x1, y1, packedCoords, false);
        assertTrue(lineString.equalsExact(lineString2, 0.00000015));
        coordinateList.clear();
    }

    @Test
    public final void testcompactStraightLineAndPackedWithExtraCoordinates() {
        addCoordinates(new Coordinate(x0, y0),new Coordinate(-179.99, 1.12345),new Coordinate(179.99, 1.12345),new Coordinate(x1, y1));
        lineString = makeGeometryLineString(new Coordinate[0]);

        coordsArray = CompactLineString.compactLineString(x0, y0, x1, y1, lineString, false);
        assertTrue(coordsArray != CompactLineString.STRAIGHT_LINE);

        lineString2 = CompactLineString.uncompactLineString(x0, y0, x1, y1, coordsArray, false);
        assertTrue(lineString.equalsExact(lineString2, 0.00000015));

        packedCoords = CompactLineString.compackLineString(x0, y0, x1, y1, lineString, false);
        assertTrue(packedCoords != CompactLineString.STRAIGHT_LINE_PACKED);

        lineString2 = CompactLineString.uncompackLineString(x0, y0, x1, y1, packedCoords, false);
        assertTrue(lineString.equalsExact(lineString2, 0.00000015));
        coordinateList.clear();
    }

    @Test
    public final void testcompactStraightLineAndPackedReverse() {
        addCoordinates(new Coordinate(x0, y0),new Coordinate(-179.99, 1.12345),new Coordinate(179.99, 1.12345),new Coordinate(x1, y1));


        assertTrue(coordsArrayReverse != CompactLineString.STRAIGHT_LINE);
        assertEquals(coordsArray.length, coordsArrayReverse.length);

        for (int i = 0; i < coordsArray.length; i++)
            assertEquals(coordsArray[i], coordsArrayReverse[i]);

        lineString2 = CompactLineString.uncompactLineString(x1, y1, x0, y0, coordsArrayReverse, true);
        assertTrue(lsi.equalsExact(lineString2, 0.00000015));

        LineString lineString3 = CompactLineString.uncompactLineString(x1, y1, x0, y0, coordsArray, true);
        assertTrue(lsi.equalsExact(lineString3, 0.00000015));


        assertTrue(packedCoordsArrayReverse != CompactLineString.STRAIGHT_LINE_PACKED);
        assertEquals(packedCoords.length, packedCoordsArrayReverse.length);

        for (int i = 0; i < packedCoords.length; i++)
            assertEquals(packedCoords[i], packedCoordsArrayReverse[i]);

        lineString2 = CompactLineString.uncompackLineString(x1, y1, x0, y0, packedCoordsArrayReverse, true);
        assertTrue(lsi.equalsExact(lineString2, 0.00000015));

        lineString3 = CompactLineString.uncompackLineString(x1, y1, x0, y0, packedCoords, true);
        assertTrue(lsi.equalsExact(lineString2, 0.00000015));
        coordinateList.clear();
    }


    @Test
    public final void testDlugoszVarLenIntPacker() {

        packTest(new int[] {}, 0);
        packTest(new int[] { 0 }, 1);
        packTest(new int[] { 63 }, 1);
        packTest(new int[] { -64 }, 1);
        packTest(new int[] { 64 }, 2);
        packTest(new int[] { -65 }, 2);
        packTest(new int[] { -8192 }, 2);
        packTest(new int[] { -8193 }, 3);
        packTest(new int[] { 8191 }, 2);
        packTest(new int[] { 8192 }, 3);
        packTest(new int[] { -1048576 }, 3);
        packTest(new int[] { -1048577 }, 4);
        packTest(new int[] { 1048575 }, 3);
        packTest(new int[] { 1048576 }, 4);
        packTest(new int[] { -67108864 }, 4);
        packTest(new int[] { -67108865 }, 5);
        packTest(new int[] { 67108863 }, 4);
        packTest(new int[] { 67108864 }, 5);
        packTest(new int[] { Integer.MAX_VALUE }, 5);
        packTest(new int[] { Integer.MIN_VALUE }, 5);

        packTest(new int[] { 0, 0 }, 2);
        packTest(new int[] { 0, 0, 0 }, 3);

        packTest(new int[] { 8100, 8200, 8300 }, 8);
    }

    private void packTest(int[] arr, int expectedPackedLen) {
        byte[] packed = DlugoszVarLenIntPacker.pack(arr);
        System.out.println("Unpacked: " + Arrays.toString(arr) + " -> packed: "
                + unsignedCharString(packed));
        assertEquals(expectedPackedLen, packed.length);
        int[] unpacked = DlugoszVarLenIntPacker.unpack(packed);
        assertTrue(Arrays.equals(arr, unpacked));
    }

    private String unsignedCharString(byte[] data) {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < data.length; i++) {
            sb.append(String.format("%02X", data[i] & 0xFF));
            if (i < data.length - 1)
                sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private void addCoordinates(Object... coordinates) {
        for (Object coordinate : coordinates) {
            coordinateList.add((Coordinate)coordinate);
        }
    }

    private LineString makeGeometryLineString(Coordinate[] coordinate){
        return geometryFactory.createLineString(coordinateList.toArray(coordinate));

    }


}
