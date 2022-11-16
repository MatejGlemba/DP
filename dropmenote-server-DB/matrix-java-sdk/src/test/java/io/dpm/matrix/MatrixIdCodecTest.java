/*
 * matrix-java-sdk - Matrix Client SDK for Java
 * Copyright (C) 2018 Kamax Sarl
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package io.dpm.matrix;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class MatrixIdCodecTest {

    private List<String[]> pairs = Arrays.asList(new String[] { "á", "=c3=a1" },
            new String[] { ".abá12_", ".ab=c3=a112_" },
            new String[] { "john.doe@example.org", "john.doe=40example.org" },
            new String[] { "john.doe@sub.example.org", "john.doe=40sub.example.org" },
            new String[] { "わたし@example.org", "=e3=82=8f=e3=81=9f=e3=81=97=40example.org" });

    @Test
    public void doEncoding() {
        for (String[] pair : pairs) {
            String e = MatrixIdCodec.encode(pair[0]);
            assertTrue(pair[0] + " -> " + e, pair[1].contentEquals(e));
        }
    }

    @Test
    public void doDecoding() {
        for (String[] pair : pairs) {
            String d = MatrixIdCodec.decode(pair[1]);
            assertTrue(pair[1] + " -> " + d, pair[0].contentEquals(d));
        }
    }

}
