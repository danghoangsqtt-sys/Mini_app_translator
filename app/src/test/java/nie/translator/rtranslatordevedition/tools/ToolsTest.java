/*
 * Copyright 2016 Luca Martino.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nie.translator.rtranslatordevedition.tools;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotSame;

public class ToolsTest {
    @Test
    public void merge_withoutInputs_returnsEmptyArray() {
        assertArrayEquals(new byte[0], Tools.merge());
    }

    @Test
    public void merge_singleArray_returnsIndependentCopy() {
        byte[] source = new byte[]{4, 8, 15, 16, 23, 42};

        byte[] merged = Tools.merge(source);

        assertArrayEquals(source, merged);
        assertNotSame(source, merged);
    }

    @Test
    public void merge_multipleArrays_preservesEveryByteInOrder() {
        byte[] first = new byte[]{1, 2};
        byte[] second = new byte[0];
        byte[] third = new byte[]{3, 4, 5};

        byte[] merged = Tools.merge(first, second, third);

        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, merged);
        assertArrayEquals(new byte[]{1, 2}, first);
        assertArrayEquals(new byte[]{3, 4, 5}, third);
    }
}
