/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2021 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package EOorg.EOeolang;

import org.eolang.Data;
import org.eolang.Dataized;
import org.eolang.PhMethod;
import org.eolang.PhWith;
import org.eolang.Phi;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link EOidentical}.
 *
 * @since 0.18
 */
public final class EOidenticalTest {

    @Test
    public void twoDataObjects() {
        MatcherAssert.assertThat(
            new Dataized(
                new PhWith(
                    new PhWith(
                        new EOidentical(Phi.Φ),
                        0, new Data.ToPhi(1L)
                    ),
                    1, new Data.ToPhi(1L)
                )
            ).take(Boolean.class),
            Matchers.equalTo(true)
        );
    }

    @Test
    public void twoDifferentDataObjects() {
        MatcherAssert.assertThat(
            new Dataized(
                new PhWith(
                    new PhWith(
                        new EOidentical(Phi.Φ),
                        0, new Data.ToPhi(7L)
                    ),
                    1, new Data.ToPhi(42L)
                )
            ).take(Boolean.class),
            Matchers.equalTo(false)
        );
    }

    @Test
    public void twoCompositeDataObjects() {
        final Phi left = new PhWith(
            new PhMethod(new Data.ToPhi(1L), "add"),
            0, new Data.ToPhi(2L)
        );
        MatcherAssert.assertThat(
            new Dataized(
                new PhWith(
                    new PhWith(
                        new EOidentical(Phi.Φ),
                        0, left
                    ),
                    1, new Data.ToPhi(3L)
                )
            ).take(Boolean.class),
            Matchers.equalTo(false)
        );
    }
}