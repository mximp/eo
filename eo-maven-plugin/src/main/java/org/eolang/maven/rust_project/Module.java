/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2023 Objectionary.com
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
package org.eolang.maven.rust_project;

import java.io.IOException;
import java.nio.file.Paths;
import org.eolang.maven.footprint.Footprint;

/**
 * Special class for converting a rust inserts
 * into a separate module of Cargo Project.
 * @since 0.1
 */
public class Module {
    /**
     * Source code of rust insert.
     */
    private final String raw;

    /**
     * Name of file.
     */
    private final String name;

    /**
     * Ctor.
     * @param raw Source code.
     * @param name Name of file.
     */
    public Module(final String raw, final String name) {
        this.raw = raw;
        this.name = name;
    }

    /**
     * Save it by footprint.
     * @param footprint Footprint.
     * @throws IOException If any issues with I/O.
     */
    public void save(final Footprint footprint) throws IOException {
        footprint.save(
            Paths.get("src").resolve(this.name).toString(),
            "rs",
            this::transform
        );
    }

    /**
     * Transform raw to compilable file.
     * @return Content for file.
     */
    private String transform() {
        final String signature = String.format(
            "#[no_mangle]%spub extern \"system\" fn Java_EOorg_EOeolang_EOrust_%s(_env: JNIEnv, _class: JClass,) -> jint {",
            System.lineSeparator(),
            this.name
        );
        return String.join(
            System.lineSeparator(),
            "use jni::objects::{JClass};",
            "use jni::sys::{jint};",
            "use jni::JNIEnv;",
            this.raw.replaceFirst(
                "[ ]*pub[ ]+fn[ ]+foo\\(\\)[ ]+->[ ]*i32[ ]*\\{",
                signature
            )
        );
    }
}
